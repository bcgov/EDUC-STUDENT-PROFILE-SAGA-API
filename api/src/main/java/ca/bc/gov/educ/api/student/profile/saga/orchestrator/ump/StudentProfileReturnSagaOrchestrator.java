package ca.bc.gov.educ.api.student.profile.saga.orchestrator.ump;

import ca.bc.gov.educ.api.student.profile.saga.mappers.StudentProfileCommentsMapper;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileEmailSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileReturnActionSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileSagaData;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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

  public StudentProfileReturnSagaOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher) {
    super(sagaService, messagePublisher, StudentProfileReturnActionSagaData.class, STUDENT_PROFILE_RETURN_SAGA.toString(), STUDENT_PROFILE_REQUEST_RETURN_SAGA_TOPIC.toString());
  }

  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
      .step(INITIATED, INITIATE_SUCCESS, ADD_STUDENT_PROFILE_COMMENT, this::executeAddProfileRequestComments)
      .step(ADD_STUDENT_PROFILE_COMMENT, STUDENT_PROFILE_COMMENT_ADDED, GET_STUDENT_PROFILE, this::executeGetProfileRequest)
      .step(ADD_STUDENT_PROFILE_COMMENT, STUDENT_PROFILE_COMMENT_ALREADY_EXIST, GET_STUDENT_PROFILE, this::executeGetProfileRequest)
      .step(GET_STUDENT_PROFILE, STUDENT_PROFILE_FOUND, UPDATE_STUDENT_PROFILE, this::executeUpdateProfileRequest)
      .step(UPDATE_STUDENT_PROFILE, STUDENT_PROFILE_UPDATED, NOTIFY_STUDENT_PROFILE_REQUEST_RETURN, this::executeNotifyStudentProfileRequestReturned)
      .step(NOTIFY_STUDENT_PROFILE_REQUEST_RETURN, STUDENT_NOTIFIED, MARK_SAGA_COMPLETE, this::markSagaComplete);
  }

  protected void executeAddProfileRequestComments(final Event event, final Saga saga, final StudentProfileReturnActionSagaData studentProfileReturnActionSagaData) throws InterruptedException, TimeoutException, IOException {
    val eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(ADD_STUDENT_PROFILE_COMMENT.toString());
    saga.setStatus(IN_PROGRESS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val studentProfileComments = StudentProfileCommentsMapper.mapper.toComments(studentProfileReturnActionSagaData);
    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(ADD_STUDENT_PROFILE_COMMENT)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(JsonUtil.getJsonStringFromObject(studentProfileComments))
      .build();
    this.postMessageToTopic(STUDENT_PROFILE_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_PROFILE_API_TOPIC for ADD_PROFILE_REQUEST_COMMENT Event.");
  }

  @Override
  protected void updateProfileRequestPayload(final StudentProfileSagaData sagaData, final StudentProfileReturnActionSagaData studentProfileReturnActionSagaData) {
    sagaData.setStudentRequestStatusCode(RETURNED.toString());
    sagaData.setReviewer(studentProfileReturnActionSagaData.getReviewer());
    sagaData.setStatusUpdateDate(LocalDateTime.now().toString());
    sagaData.setUpdateUser(studentProfileReturnActionSagaData.getUpdateUser());
  }

  @Override
  protected String updateGetProfileRequestPayload(final StudentProfileReturnActionSagaData studentProfileReturnActionSagaData) {
    return studentProfileReturnActionSagaData.getStudentProfileRequestID();
  }

  /**
   * it will send a message to pen request api topic to add a comment.
   *
   * @param event                              current event.
   * @param saga                               the model object.
   * @param studentProfileReturnActionSagaData the payload as the object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void executeNotifyStudentProfileRequestReturned(final Event event, final Saga saga, final StudentProfileReturnActionSagaData studentProfileReturnActionSagaData) throws IOException, InterruptedException, TimeoutException {
    val eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(NOTIFY_STUDENT_PROFILE_REQUEST_RETURN.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(NOTIFY_STUDENT_PROFILE_REQUEST_RETURN)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(this.buildEmailSagaData(studentProfileReturnActionSagaData))
      .build();
    this.postMessageToTopic(PROFILE_REQUEST_EMAIL_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_PROFILE_EMAIL_API_TOPIC for NOTIFY_STUDENT_PROFILE_REQUEST_RETURN Event.");
  }

  private String buildEmailSagaData(final StudentProfileReturnActionSagaData studentProfileRequestRejectActionSagaData) throws JsonProcessingException {
    val penReqEmailSagaData = StudentProfileEmailSagaData.builder()
      .emailAddress(studentProfileRequestRejectActionSagaData.getEmail())
      .identityType(studentProfileRequestRejectActionSagaData.getIdentityType())
      .build();
    return JsonUtil.getJsonStringFromObject(penReqEmailSagaData);
  }
}
