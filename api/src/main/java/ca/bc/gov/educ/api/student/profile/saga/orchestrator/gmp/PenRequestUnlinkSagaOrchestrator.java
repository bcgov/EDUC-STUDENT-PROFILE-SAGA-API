package ca.bc.gov.educ.api.student.profile.saga.orchestrator.gmp;

import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.DigitalIdSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestUnlinkSagaData;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.PEN_REQUEST_UNLINK_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.DIGITAL_ID_API_TOPIC;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.PEN_REQUEST_UNLINK_SAGA_TOPIC;

@Component
@Slf4j
public class PenRequestUnlinkSagaOrchestrator extends BasePenReqSagaOrchestrator<PenRequestUnlinkSagaData> {

  public PenRequestUnlinkSagaOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher) {
    super(sagaService, messagePublisher, PenRequestUnlinkSagaData.class, PEN_REQUEST_UNLINK_SAGA.toString(), PEN_REQUEST_UNLINK_SAGA_TOPIC.toString());
  }

  /**
   * this is the source of truth for this particular saga flow.
   */
  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
      .step(INITIATED, INITIATE_SUCCESS, GET_DIGITAL_ID, this::executeGetDigitalId)
      .step(GET_DIGITAL_ID, DIGITAL_ID_FOUND, UPDATE_DIGITAL_ID, this::executeUpdateDigitalId)
      .step(UPDATE_DIGITAL_ID, DIGITAL_ID_UPDATED, GET_PEN_REQUEST, this::executeGetPenRequest)
      .step(GET_PEN_REQUEST, PEN_REQUEST_FOUND, UPDATE_PEN_REQUEST, this::executeUpdatePenRequest)
      .step(UPDATE_PEN_REQUEST, PEN_REQUEST_UPDATED, MARK_SAGA_COMPLETE, this::markSagaComplete);
  }

  @Override
  protected void updatePenRequestPayload(final PenRequestSagaData penRequestSagaData, final PenRequestUnlinkSagaData penRequestUnlinkSagaData) {
    penRequestSagaData.setReviewer(penRequestUnlinkSagaData.getReviewer());
    penRequestSagaData.setPenRequestStatusCode(penRequestUnlinkSagaData.getPenRequestStatusCode());
    penRequestSagaData.setUpdateUser(penRequestUnlinkSagaData.getUpdateUser());
    penRequestSagaData.setStatusUpdateDate(LocalDateTime.now().toString());
    penRequestSagaData.setPen(null);
  }

  @Override
  protected String updateGetPenRequestPayload(final PenRequestUnlinkSagaData penRequestUnlinkSagaData) {
    return penRequestUnlinkSagaData.getPenRetrievalRequestID();
  }

  /**
   * @param event                    current event
   * @param saga                     the model object.
   * @param penRequestUnlinkSagaData the payload as object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void executeGetDigitalId(final Event event, final Saga saga, final PenRequestUnlinkSagaData penRequestUnlinkSagaData) throws InterruptedException, TimeoutException, IOException {

    final var eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(GET_DIGITAL_ID.toString()); // set current event as saga state.
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    final var nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(GET_DIGITAL_ID)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(penRequestUnlinkSagaData.getDigitalID())
      .build();
    this.postMessageToTopic(DIGITAL_ID_API_TOPIC.toString(), nextEvent);
    log.info("message sent to DIGITAL_ID_API_TOPIC for GET_DIGITAL_ID Event.");
  }

  /**
   * this is executed after get digital id, so the event response would contain the entire digital id payload, this method will only update the student Id.
   *
   * @param event                    current event
   * @param saga                     the model object.
   * @param penRequestUnlinkSagaData the payload as object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void executeUpdateDigitalId(final Event event, final Saga saga, final PenRequestUnlinkSagaData penRequestUnlinkSagaData) throws IOException, InterruptedException, TimeoutException {
    final var eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(UPDATE_DIGITAL_ID.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    final var digitalIdSagaData = JsonUtil.getJsonObjectFromString(DigitalIdSagaData.class, event.getEventPayload());
    digitalIdSagaData.setStudentID(null);
    digitalIdSagaData.setUpdateUser(penRequestUnlinkSagaData.getUpdateUser());
    final var nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(UPDATE_DIGITAL_ID)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(JsonUtil.getJsonStringFromObject(digitalIdSagaData))
      .build();
    this.postMessageToTopic(DIGITAL_ID_API_TOPIC.toString(), nextEvent);
    log.info("message sent to DIGITAL_ID_API_TOPIC for UPDATE_DIGITAL_ID Event.");

  }


}
