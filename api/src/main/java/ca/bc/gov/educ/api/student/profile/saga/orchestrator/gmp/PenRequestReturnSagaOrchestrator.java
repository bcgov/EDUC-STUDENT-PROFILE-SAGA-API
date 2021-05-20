package ca.bc.gov.educ.api.student.profile.saga.orchestrator.gmp;


import ca.bc.gov.educ.api.student.profile.saga.mappers.PenRequestCommentsMapper;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenReqEmailSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestReturnSagaData;
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
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.PEN_REQUEST_RETURN_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.*;

@Component
@Slf4j
public class PenRequestReturnSagaOrchestrator extends BasePenReqSagaOrchestrator<PenRequestReturnSagaData> {

  @Autowired
  public PenRequestReturnSagaOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher) {
    super(sagaService, messagePublisher, PenRequestReturnSagaData.class, PEN_REQUEST_RETURN_SAGA.toString(), PEN_REQUEST_RETURN_SAGA_TOPIC.toString());
  }

  /**
   * this is the source of truth for this particular saga flow.
   */
  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
      .step(INITIATED, INITIATE_SUCCESS, ADD_PEN_REQUEST_COMMENT, this::executeAddPenRequestComments)
      .step(ADD_PEN_REQUEST_COMMENT, PEN_REQUEST_COMMENT_ADDED, GET_PEN_REQUEST, this::executeGetPenRequest)
      .step(ADD_PEN_REQUEST_COMMENT, PEN_REQUEST_COMMENT_ALREADY_EXIST, GET_PEN_REQUEST, this::executeGetPenRequest)
      .step(GET_PEN_REQUEST, PEN_REQUEST_FOUND, UPDATE_PEN_REQUEST, this::executeUpdatePenRequest)
      .step(UPDATE_PEN_REQUEST, PEN_REQUEST_UPDATED, NOTIFY_STUDENT_PEN_REQUEST_RETURN, this::executeNotifyStudentPenRequestReturn)
      .step(NOTIFY_STUDENT_PEN_REQUEST_RETURN, STUDENT_NOTIFIED, MARK_SAGA_COMPLETE, this::markSagaComplete);
  }

  /**
   * it will send a message to pen request api topic to add a comment.
   *
   * @param event                    current event.
   * @param saga                     the model object.
   * @param penRequestReturnSagaData the payload as the object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void executeAddPenRequestComments(final Event event, final Saga saga, final PenRequestReturnSagaData penRequestReturnSagaData) throws IOException, InterruptedException, TimeoutException {
    val eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(ADD_PEN_REQUEST_COMMENT.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val penRequestComments = PenRequestCommentsMapper.mapper.toPenReqComments(penRequestReturnSagaData);
    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(ADD_PEN_REQUEST_COMMENT)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(JsonUtil.getJsonStringFromObject(penRequestComments))
      .build();
    this.postMessageToTopic(PEN_REQUEST_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PEN_REQUEST_API_TOPIC for ADD_PEN_REQUEST_COMMENT Event.");
  }

  @Override
  protected void updatePenRequestPayload(final PenRequestSagaData penRequestSagaData, final PenRequestReturnSagaData penRequestReturnSagaData) {
    penRequestSagaData.setPenRequestStatusCode(penRequestReturnSagaData.getPenRequestStatusCode());
    penRequestSagaData.setStatusUpdateDate(LocalDateTime.now().toString());
    penRequestSagaData.setReviewer(penRequestReturnSagaData.getReviewer());
    penRequestSagaData.setUpdateUser(penRequestReturnSagaData.getUpdateUser());
  }

  @Override
  protected String updateGetPenRequestPayload(final PenRequestReturnSagaData penRequestReturnSagaData) {
    return penRequestReturnSagaData.getPenRetrievalRequestID();
  }

  /**
   * this method will send message to pen request email api to send email to the student that the pen request is now returned.
   *
   * @param event                    current event
   * @param saga                     the model object.
   * @param penRequestReturnSagaData the payload as object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void executeNotifyStudentPenRequestReturn(final Event event, final Saga saga, final PenRequestReturnSagaData penRequestReturnSagaData) throws IOException, InterruptedException, TimeoutException {
    val eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(NOTIFY_STUDENT_PEN_REQUEST_RETURN.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(NOTIFY_STUDENT_PEN_REQUEST_RETURN)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(this.buildPenReqEmailSagaData(penRequestReturnSagaData))
      .build();
    this.postMessageToTopic(PROFILE_REQUEST_EMAIL_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PROFILE_REQUEST_EMAIL_API_TOPIC for NOTIFY_STUDENT_PEN_REQUEST_RETURN Event.");

  }

  /**
   * this method builds the email payload as json string.
   *
   * @param penRequestReturnSagaData the payload as object.
   * @return the string representation of the json object.
   * @throws JsonProcessingException if it is unable to convert the object to json string.
   */
  private String buildPenReqEmailSagaData(final PenRequestReturnSagaData penRequestReturnSagaData) throws JsonProcessingException {
    val penReqEmailSagaData = PenReqEmailSagaData.builder()
      .emailAddress(penRequestReturnSagaData.getEmail())
      .identityType(penRequestReturnSagaData.getIdentityType())
      .build();
    return JsonUtil.getJsonStringFromObject(penReqEmailSagaData);
  }
}
