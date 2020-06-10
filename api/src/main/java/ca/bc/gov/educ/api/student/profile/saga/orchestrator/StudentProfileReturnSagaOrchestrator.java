package ca.bc.gov.educ.api.student.profile.saga.orchestrator;

import ca.bc.gov.educ.api.student.profile.saga.mappers.StudentProfileCommentsMapper;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessageSubscriber;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.schedulers.EventTaskScheduler;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.*;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.ProfileRequestStatusCode.RETURNED;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.STUDENT_PROFILE_RETURN_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.*;

@Component
@Slf4j
public class StudentProfileReturnSagaOrchestrator extends BaseProfileReqSagaOrchestrator<StudentProfileReturnActionSagaData> {

  public StudentProfileReturnSagaOrchestrator(SagaService sagaService, MessagePublisher messagePublisher, MessageSubscriber messageSubscriber, EventTaskScheduler taskScheduler) {
    super(sagaService, messagePublisher, messageSubscriber, taskScheduler, StudentProfileReturnActionSagaData.class, STUDENT_PROFILE_RETURN_SAGA.toString(), STUDENT_PROFILE_REQUEST_RETURN_SAGA_TOPIC.toString());
  }

  @Override
  protected void populateNextStepsMap() {
    stepBuilder()
        .step(INITIATED, INITIATE_SUCCESS, ADD_STUDENT_PROFILE_COMMENT, this::executeAddProfileRequestComments)
        .step(ADD_STUDENT_PROFILE_COMMENT, STUDENT_PROFILE_COMMENT_ADDED, GET_STUDENT_PROFILE, this::executeGetProfileRequest)
        .step(ADD_STUDENT_PROFILE_COMMENT, STUDENT_PROFILE_COMMENT_ALREADY_EXIST, GET_STUDENT_PROFILE, this::executeGetProfileRequest)
        .step(GET_STUDENT_PROFILE, STUDENT_PROFILE_FOUND, UPDATE_STUDENT_PROFILE, this::executeUpdateProfileRequest)
        .step(UPDATE_STUDENT_PROFILE, STUDENT_PROFILE_UPDATED, NOTIFY_STUDENT_PROFILE_REQUEST_RETURN, this::executeNotifyStudentProfileRequestReturned)
        .step(NOTIFY_STUDENT_PROFILE_REQUEST_RETURN, STUDENT_NOTIFIED, MARK_SAGA_COMPLETE, this::markSagaComplete);
  }

  private void executeAddProfileRequestComments(Event event, Saga saga, StudentProfileReturnActionSagaData studentProfileReturnActionSagaData) throws InterruptedException, TimeoutException, IOException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(ADD_STUDENT_PROFILE_COMMENT.toString());
    saga.setStatus(IN_PROGRESS.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    StudentProfileComments studentProfileComments = StudentProfileCommentsMapper.mapper.toComments(studentProfileReturnActionSagaData);
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
        .eventType(ADD_STUDENT_PROFILE_COMMENT)
        .replyTo(getTopicToSubscribe())
        .eventPayload(JsonUtil.getJsonStringFromObject(studentProfileComments))
        .build();
    postMessageToTopic(STUDENT_PROFILE_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_PROFILE_API_TOPIC for ADD_PROFILE_REQUEST_COMMENT Event.");
  }

  @Override
  protected void updateProfileRequestPayload(StudentProfileSagaData sagaData, StudentProfileReturnActionSagaData studentProfileReturnActionSagaData) {
    sagaData.setStudentRequestStatusCode(RETURNED.toString());
    sagaData.setStatusUpdateDate(LocalDateTime.now().toString());
    sagaData.setUpdateUser(studentProfileReturnActionSagaData.getUpdateUser());
  }

  @Override
  protected String updateGetProfileRequestPayload(StudentProfileReturnActionSagaData studentProfileReturnActionSagaData) {
    return studentProfileReturnActionSagaData.getStudentProfileRequestID();
  }

  /**
   * it will send a message to pen request api topic to add a comment.
   *
   * @param event                                     current event.
   * @param saga                                      the model object.
   * @param studentProfileReturnActionSagaData the payload as the object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  private void executeNotifyStudentProfileRequestReturned(Event event, Saga saga, StudentProfileReturnActionSagaData studentProfileReturnActionSagaData) throws IOException, InterruptedException, TimeoutException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(NOTIFY_STUDENT_PROFILE_REQUEST_RETURN.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
        .eventType(NOTIFY_STUDENT_PROFILE_REQUEST_RETURN)
        .replyTo(getTopicToSubscribe())
        .eventPayload(buildEmailSagaData(studentProfileReturnActionSagaData))
        .build();
    postMessageToTopic(PROFILE_REQUEST_EMAIL_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_PROFILE_EMAIL_API_TOPIC for NOTIFY_STUDENT_PROFILE_REQUEST_RETURN Event.");
  }

  private String buildEmailSagaData(StudentProfileReturnActionSagaData studentProfileRequestRejectActionSagaData) throws JsonProcessingException {
    StudentProfileEmailSagaData penReqEmailSagaData = StudentProfileEmailSagaData.builder()
        .emailAddress(studentProfileRequestRejectActionSagaData.getEmail())
        .identityType(studentProfileRequestRejectActionSagaData.getIdentityType())
        .build();
    return JsonUtil.getJsonStringFromObject(penReqEmailSagaData);
  }
}
