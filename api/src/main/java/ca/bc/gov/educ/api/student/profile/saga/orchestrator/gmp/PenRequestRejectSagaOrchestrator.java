package ca.bc.gov.educ.api.student.profile.saga.orchestrator.gmp;

import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenReqEmailSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestRejectSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestSagaData;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.PEN_REQUEST_REJECT_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.PEN_REQUEST_REJECT_SAGA_TOPIC;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.PROFILE_REQUEST_EMAIL_API_TOPIC;


@Component
@Slf4j
public class PenRequestRejectSagaOrchestrator extends BasePenReqSagaOrchestrator<PenRequestRejectSagaData> {

  @Autowired
  public PenRequestRejectSagaOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher) {
    super(sagaService, messagePublisher, PenRequestRejectSagaData.class, PEN_REQUEST_REJECT_SAGA.toString(), PEN_REQUEST_REJECT_SAGA_TOPIC.toString());
  }

  /**
   * this is the source of truth for this particular saga flow.
   */
  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
      .step(INITIATED, INITIATE_SUCCESS, GET_PEN_REQUEST, this::executeGetPenRequest)
      .step(GET_PEN_REQUEST, PEN_REQUEST_FOUND, UPDATE_PEN_REQUEST, this::executeUpdatePenRequest)
      .step(UPDATE_PEN_REQUEST, PEN_REQUEST_UPDATED, NOTIFY_STUDENT_PEN_REQUEST_REJECT, this::executeNotifyStudentPenRequestReject)
      .step(NOTIFY_STUDENT_PEN_REQUEST_REJECT, STUDENT_NOTIFIED, MARK_SAGA_COMPLETE, this::markSagaComplete);
  }

  @Override
  protected void updatePenRequestPayload(final PenRequestSagaData penRequestSagaData, final PenRequestRejectSagaData penRequestRejectSagaData) {
    penRequestSagaData.setPenRequestStatusCode(penRequestRejectSagaData.getPenRequestStatusCode());
    penRequestSagaData.setFailureReason(penRequestRejectSagaData.getRejectionReason());
    penRequestSagaData.setStatusUpdateDate(LocalDateTime.now().toString());
    penRequestSagaData.setReviewer(penRequestRejectSagaData.getReviewer());
    penRequestSagaData.setUpdateUser(penRequestRejectSagaData.getUpdateUser());
  }

  @Override
  protected String updateGetPenRequestPayload(final PenRequestRejectSagaData penRequestRejectSagaData) {
    return penRequestRejectSagaData.getPenRetrievalRequestID();
  }

  /**
   * this method will send message to pen request email api to send email to the student that the pen request is now returned.
   *
   * @param event                    current event
   * @param saga                     the model object.
   * @param penRequestRejectSagaData the payload as object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void executeNotifyStudentPenRequestReject(final Event event, final Saga saga, final PenRequestRejectSagaData penRequestRejectSagaData) throws IOException, InterruptedException, TimeoutException {
    val eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(NOTIFY_STUDENT_PEN_REQUEST_REJECT.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(NOTIFY_STUDENT_PEN_REQUEST_REJECT)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(this.buildPenReqEmailSagaData(penRequestRejectSagaData))
      .build();
    this.postMessageToTopic(PROFILE_REQUEST_EMAIL_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PROFILE_REQUEST_EMAIL_API_TOPIC for NOTIFY_STUDENT_PEN_REQUEST_REJECT Event.");

  }

  /**
   * this method builds the email payload as json string.
   *
   * @param penRequestRejectSagaData the payload as object.
   * @return the string representation of the json object.
   * @throws JsonProcessingException if it is unable to convert the object to json string.
   */
  private String buildPenReqEmailSagaData(final PenRequestRejectSagaData penRequestRejectSagaData) throws JsonProcessingException {
    val penReqEmailSagaData = PenReqEmailSagaData.builder()
      .emailAddress(penRequestRejectSagaData.getEmail())
      .identityType(penRequestRejectSagaData.getIdentityType())
      .rejectionReason(penRequestRejectSagaData.getRejectionReason())
      .build();
    return JsonUtil.getJsonStringFromObject(penReqEmailSagaData);
  }
}
