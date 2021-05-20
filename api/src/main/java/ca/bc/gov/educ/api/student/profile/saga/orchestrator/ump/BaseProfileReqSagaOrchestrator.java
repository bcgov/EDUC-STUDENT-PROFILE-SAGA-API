package ca.bc.gov.educ.api.student.profile.saga.orchestrator.ump;

import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileSagaData;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.GET_STUDENT_PROFILE;
import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.UPDATE_STUDENT_PROFILE;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.STUDENT_PROFILE_API_TOPIC;

/**
 * This class is super class specific to Student Profile Request Saga API
 *
 * @param <T>
 * @author om
 */
@Slf4j
public abstract class BaseProfileReqSagaOrchestrator<T> extends BaseOrchestrator<T> {


  protected BaseProfileReqSagaOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, final Class<T> clazz, final String sagaName, final String topicToSubscribe) {
    super(sagaService, messagePublisher, clazz, sagaName, topicToSubscribe);
  }


  protected void executeUpdateProfileRequest(final Event event, final Saga saga, final T t) throws IOException, InterruptedException, TimeoutException {
    val eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(UPDATE_STUDENT_PROFILE.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val sagaData = JsonUtil.getJsonObjectFromString(StudentProfileSagaData.class, event.getEventPayload());
    this.updateProfileRequestPayload(sagaData, t);
    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(UPDATE_STUDENT_PROFILE)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(JsonUtil.getJsonStringFromObject(sagaData))
      .build();
    this.postMessageToTopic(STUDENT_PROFILE_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_PROFILE_API_TOPIC for UPDATE_PROFILE_REQUEST Event.");
  }

  protected void executeGetProfileRequest(final Event event, final Saga saga, final T t) throws InterruptedException, TimeoutException, IOException {
    val eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(GET_STUDENT_PROFILE.toString()); // set current event as saga state.
    saga.setStatus(IN_PROGRESS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(GET_STUDENT_PROFILE)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(this.updateGetProfileRequestPayload(t))
      .build();
    this.postMessageToTopic(STUDENT_PROFILE_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_PROFILE_API_TOPIC for GET_PROFILE_REQUEST Event.");
  }

  protected abstract void updateProfileRequestPayload(StudentProfileSagaData sagaData, T t);

  protected abstract String updateGetProfileRequestPayload(T t);
}
