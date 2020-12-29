package ca.bc.gov.educ.api.student.profile.saga.orchestrator.gmp;

import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessageSubscriber;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.api.student.profile.saga.schedulers.EventTaskScheduler;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestSagaData;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.GET_PEN_REQUEST;
import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.UPDATE_PEN_REQUEST;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.PEN_REQUEST_API_TOPIC;

/**
 * This class is super class specific to Pen Request Saga API
 *
 * @param <T>
 * @author om
 */
@Slf4j
public abstract class BasePenReqSagaOrchestrator<T> extends BaseOrchestrator<T> {

  protected BasePenReqSagaOrchestrator(SagaService sagaService, MessagePublisher messagePublisher, Class<T> clazz, String sagaName, String topicToSubscribe) {
    super(sagaService, messagePublisher, clazz, sagaName, topicToSubscribe);
  }


  protected void executeUpdatePenRequest(Event event, Saga saga, T t) throws IOException, InterruptedException, TimeoutException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(UPDATE_PEN_REQUEST.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    PenRequestSagaData penRequestSagaData = JsonUtil.getJsonObjectFromString(PenRequestSagaData.class, event.getEventPayload());
    updatePenRequestPayload(penRequestSagaData, t);
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(UPDATE_PEN_REQUEST)
      .replyTo(getTopicToSubscribe())
      .eventPayload(JsonUtil.getJsonStringFromObject(penRequestSagaData))
      .build();
    postMessageToTopic(PEN_REQUEST_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PEN_REQUEST_API_TOPIC for UPDATE_PEN_REQUEST Event.");
  }

  protected void executeGetPenRequest(Event event, Saga saga, T t) throws InterruptedException, TimeoutException, IOException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(GET_PEN_REQUEST.toString()); // set current event as saga state.
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(GET_PEN_REQUEST)
      .replyTo(getTopicToSubscribe())
      .eventPayload(updateGetPenRequestPayload(t))
      .build();
    postMessageToTopic(PEN_REQUEST_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PEN_REQUEST_API_TOPIC for GET_PEN_REQUEST Event.");
  }

  protected abstract void updatePenRequestPayload(PenRequestSagaData penRequestSagaData, T t);

  protected abstract String updateGetPenRequestPayload(T t);
}
