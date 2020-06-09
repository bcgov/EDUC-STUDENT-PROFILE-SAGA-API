package ca.bc.gov.educ.api.student.profile.saga.orchestrator;

import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessageSubscriber;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.schedulers.EventTaskScheduler;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.StudentProfileEmailSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.StudentProfileRequestRejectActionSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.StudentProfileSagaData;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.ProfileRequestStatusCode.REJECTED;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.STUDENT_PROFILE_REJECT_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.PROFILE_REQUEST_EMAIL_API_TOPIC;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.STUDENT_PROFILE_REQUEST_REJECT_SAGA_TOPIC;

@Component
@Slf4j
public class StudentProfileRejectSagaOrchestrator extends BaseProfileReqSagaOrchestrator<StudentProfileRequestRejectActionSagaData> {

  @Autowired
  public StudentProfileRejectSagaOrchestrator(SagaService sagaService, MessagePublisher messagePublisher, MessageSubscriber messageSubscriber, EventTaskScheduler taskScheduler) {
    super(sagaService, messagePublisher, messageSubscriber, taskScheduler, StudentProfileRequestRejectActionSagaData.class, STUDENT_PROFILE_REJECT_SAGA.toString(), STUDENT_PROFILE_REQUEST_REJECT_SAGA_TOPIC.toString());
  }


  @Override
  protected void populateNextStepsMap() {
    stepBuilder()
        .step(INITIATED, INITIATE_SUCCESS, GET_STUDENT_PROFILE, this::executeGetProfileRequest)
        .step(GET_STUDENT_PROFILE, STUDENT_PROFILE_FOUND, UPDATE_STUDENT_PROFILE, this::executeUpdateProfileRequest)
        .step(UPDATE_STUDENT_PROFILE, STUDENT_PROFILE_UPDATED, NOTIFY_STUDENT_PROFILE_REQUEST_REJECT, this::executeNotifyStudentProfileRequestRejected)
        .step(NOTIFY_STUDENT_PROFILE_REQUEST_REJECT, STUDENT_NOTIFIED, MARK_SAGA_COMPLETE, this::markSagaComplete);
  }

  @Override
  protected void updateProfileRequestPayload(StudentProfileSagaData sagaData, StudentProfileRequestRejectActionSagaData studentProfileRequestRejectActionSagaData) {
    sagaData.setStudentRequestStatusCode(REJECTED.toString());
    sagaData.setFailureReason(studentProfileRequestRejectActionSagaData.getRejectionReason());
    sagaData.setStatusUpdateDate(LocalDateTime.now().toString());
    sagaData.setUpdateUser(studentProfileRequestRejectActionSagaData.getUpdateUser());
  }

  @Override
  protected String updateGetProfileRequestPayload(StudentProfileRequestRejectActionSagaData studentProfileRequestRejectActionSagaData) {
    return studentProfileRequestRejectActionSagaData.getStudentProfileRequestID();
  }

  /**
   * it will send a message to pen request api topic to add a comment.
   *
   * @param event                                     current event.
   * @param saga                                      the model object.
   * @param studentProfileRequestRejectActionSagaData the payload as the object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  private void executeNotifyStudentProfileRequestRejected(Event event, Saga saga, StudentProfileRequestRejectActionSagaData studentProfileRequestRejectActionSagaData) throws IOException, InterruptedException, TimeoutException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(NOTIFY_STUDENT_PROFILE_REQUEST_REJECT.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
        .eventType(NOTIFY_STUDENT_PROFILE_REQUEST_REJECT)
        .replyTo(getTopicToSubscribe())
        .eventPayload(buildEmailSagaData(studentProfileRequestRejectActionSagaData))
        .build();
    postMessageToTopic(PROFILE_REQUEST_EMAIL_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_PROFILE_EMAIL_API_TOPIC for NOTIFY_STUDENT_PROFILE_REQUEST_REJECT Event.");
  }

  private String buildEmailSagaData(StudentProfileRequestRejectActionSagaData studentProfileRequestRejectActionSagaData) throws JsonProcessingException {
    StudentProfileEmailSagaData penReqEmailSagaData = StudentProfileEmailSagaData.builder()
        .emailAddress(studentProfileRequestRejectActionSagaData.getEmail())
        .rejectionReason(studentProfileRequestRejectActionSagaData.getRejectionReason())
        .identityType(studentProfileRequestRejectActionSagaData.getIdentityType())
        .build();
    return JsonUtil.getJsonStringFromObject(penReqEmailSagaData);
  }
}
