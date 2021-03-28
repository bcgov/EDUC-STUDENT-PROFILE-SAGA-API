package ca.bc.gov.educ.api.student.profile.saga.orchestrator.base;

import ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome;
import ca.bc.gov.educ.api.student.profile.saga.constants.EventType;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.NotificationEvent;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.COMPLETED;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.FORCE_STOPPED;
import static lombok.AccessLevel.PROTECTED;

@Slf4j
public abstract class BaseOrchestrator<T> implements SagaEventHandler, Orchestrator {
  protected static final String SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT = "system is going to execute next event :: {} for current event {}";
  public static final String SELF = "SELF";
  @Getter(PROTECTED)
  private final SagaService sagaService;
  @Getter(PROTECTED)
  private final MessagePublisher messagePublisher;
  protected final Class<T> clazz;

  @Getter
  private final String sagaName;
  @Getter
  private final String topicToSubscribe;

  protected final Map<EventType, List<SagaEventState<T>>> nextStepsToExecute = new LinkedHashMap<>();

  protected BaseOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, final Class<T> clazz, final String sagaName, final String topicToSubscribe) {
    this.sagaService = sagaService;
    this.messagePublisher = messagePublisher;
    this.clazz = clazz;
    this.sagaName = sagaName;
    this.topicToSubscribe = topicToSubscribe;
    this.populateStepsToExecuteMap();
  }

  protected List<SagaEventState<T>> createSingleCollectionEventState(final EventOutcome eventOutcome, final Boolean isCompensating, final EventType nextEventType, final SagaStep<T> stepToExecute) {
    final List<SagaEventState<T>> eventStates = new ArrayList<>();
    eventStates.add(this.buildSagaEventState(eventOutcome, isCompensating, nextEventType, stepToExecute));
    return eventStates;
  }


  protected SagaEventState<T> buildSagaEventState(final EventOutcome eventOutcome, final Boolean isCompensating, final EventType nextEventType, final SagaStep<T> stepToExecute) {
    return SagaEventState.<T>builder().currentEventOutcome(eventOutcome).isCompensating(isCompensating).nextEventType(nextEventType).stepToExecute(stepToExecute).build();
  }


  protected BaseOrchestrator<T> registerStepToExecute(final EventType initEvent, final EventOutcome outcome, final Boolean isCompensating, final EventType nextEvent, final SagaStep<T> stepToExecute) {
    if (this.nextStepsToExecute.containsKey(initEvent)) {
      final List<SagaEventState<T>> states = this.nextStepsToExecute.get(initEvent);
      states.add(this.buildSagaEventState(outcome, isCompensating, nextEvent, stepToExecute));
    } else {
      this.nextStepsToExecute.put(initEvent, this.createSingleCollectionEventState(outcome, isCompensating, nextEvent, stepToExecute));
    }
    return this;
  }

  /**
   * @param currentEvent  the event that has occurred.
   * @param outcome       outcome of the event.
   * @param nextEvent     next event that will occur.
   * @param stepToExecute which method to execute for the next event. it is a lambda function.
   * @return {@link BaseOrchestrator}
   */
  public BaseOrchestrator<T> step(final EventType currentEvent, final EventOutcome outcome, final EventType nextEvent, final SagaStep<T> stepToExecute) {
    return this.registerStepToExecute(currentEvent, outcome, false, nextEvent, stepToExecute);
  }

  /**
   * this is a simple and convenient method to trigger builder pattern in the child classes.
   *
   * @return {@link BaseOrchestrator}
   */
  public BaseOrchestrator<T> stepBuilder() {
    return this;
  }

  /**
   * this method will check if the event is not already processed. this could happen in SAGAs due to duplicate messages.
   * Application should be able to handle this.
   *
   * @param currentEventType current event.
   * @param saga             the model object.
   * @param eventTypes       event types stored in the hashmap
   * @return true or false based on whether the current event with outcome received from the queue is already processed or not.
   */
  protected boolean isNotProcessedEvent(final EventType currentEventType, final Saga saga, final Set<EventType> eventTypes) {
    final EventType eventTypeInDB = EventType.valueOf(saga.getSagaState());
    final List<EventType> events = new LinkedList<>(eventTypes);
    final int dbEventIndex = events.indexOf(eventTypeInDB);
    final int currentEventIndex = events.indexOf(currentEventType);
    return currentEventIndex >= dbEventIndex;
  }

  /**
   * creates the PenRequestSagaEventState object
   *
   * @param saga         the payload.
   * @param eventType    event type
   * @param eventOutcome outcome
   * @param eventPayload payload.
   * @return {@link SagaEvent}
   */
  protected SagaEvent createEventState(@NotNull final Saga saga, @NotNull final EventType eventType, @NotNull final EventOutcome eventOutcome, final String eventPayload) {
    final var user = this.sagaName.length() > 32 ? this.sagaName.substring(0, 32) : this.sagaName;
    return SagaEvent.builder()
      .createDate(LocalDateTime.now())
      .createUser(user)
      .updateDate(LocalDateTime.now())
      .updateUser(user)
      .saga(saga)
      .sagaEventOutcome(eventOutcome.toString())
      .sagaEventState(eventType.toString())
      .sagaStepNumber(this.calculateStep(saga))
      .sagaEventResponse(eventPayload == null ? "" : eventPayload)
      .build();
  }

  /**
   * This method updates the DB and marks the process as complete.
   *
   * @param event    the current event.
   * @param saga     the saga model object.
   * @param sagaData the payload string as object.
   */
  protected void markSagaComplete(final Event event, final Saga saga, final T sagaData) throws InterruptedException, TimeoutException, IOException {
    log.trace("payload is {}", sagaData);
    final var finalEvent = new NotificationEvent();
    BeanUtils.copyProperties(event, finalEvent);
    finalEvent.setEventType(MARK_SAGA_COMPLETE);
    finalEvent.setEventOutcome(SAGA_COMPLETED);
    finalEvent.setSagaStatus(COMPLETED.toString());
    finalEvent.setStudentRequestID(saga.getProfileRequestId() != null ? saga.getProfileRequestId().toString() : "");
    finalEvent.setPenRequestID(saga.getPenRequestId() != null ? saga.getPenRequestId().toString() : "");
    finalEvent.setSagaName(this.getSagaName());
    finalEvent.setEventPayload(""); // no need to send payload as it is not required by the subscribers.
    this.postMessageToTopic(this.getTopicToSubscribe(), finalEvent);
    final SagaEvent sagaEvent = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(COMPLETED.toString());
    saga.setStatus(COMPLETED.toString());
    saga.setUpdateDate(LocalDateTime.now());
    this.getSagaService().updateAttachedSagaWithEvents(saga, sagaEvent);

  }

  /**
   * calculate step number
   *
   * @param saga the model object.
   * @return step number that was calculated.
   */
  private int calculateStep(final Saga saga) {
    val sagaStates = this.getSagaService().findAllSagaStates(saga);
    return (sagaStates.size() + 1);
  }

  /**
   * convenient method to post message to topic, to be used by child classes.
   *
   * @param topicName topic name where the message will be posted.
   * @param nextEvent the next event object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void postMessageToTopic(final String topicName, final Event nextEvent) throws InterruptedException, IOException, TimeoutException {
    this.getMessagePublisher().dispatchMessage(topicName, JsonUtil.getJsonStringFromObject(nextEvent).getBytes());
  }

  /**
   * it finds the last event that was processed successfully for this saga.
   *
   * @param eventStates event states corresponding to the Saga.
   * @return {@link SagaEvent} if found else null.
   */
  protected Optional<SagaEvent> findTheLastEventOccurred(final List<SagaEvent> eventStates) {
    final int step = eventStates.stream().map(SagaEvent::getSagaStepNumber).mapToInt(x -> x).max().orElse(0);
    return eventStates.stream().filter(element -> element.getSagaStepNumber() == step).findFirst();
  }

  /**
   * this method is called from the cron job , which will replay the saga process based on its current state.
   *
   * @param saga the model object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  @Override
  @Async("async-task-executor")
  @Transactional
  public void replaySaga(final Saga saga) throws IOException, InterruptedException, TimeoutException {
    final var eventStates = this.getSagaService().findAllSagaStates(saga);
    final T t = JsonUtil.getJsonObjectFromString(this.clazz, saga.getPayload());
    if (eventStates.isEmpty()) { //process did not start last time, lets start from beginning.
      this.replayFromBeginning(saga, t);
    } else {
      this.replayFromLastEvent(saga, eventStates, t);
    }
  }

  /**
   * This method will restart the saga process from where it was left the last time. which could occur due to various reasons
   *
   * @param saga        the model object.
   * @param eventStates the event states corresponding to the saga
   * @param t           the payload string as an object
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  private void replayFromLastEvent(final Saga saga, final List<SagaEvent> eventStates, final T t) throws InterruptedException, TimeoutException, IOException {
    val sagaEventOptional = this.findTheLastEventOccurred(eventStates);
    if (sagaEventOptional.isPresent()) {
      val sagaEvent = sagaEventOptional.get();
      log.trace(sagaEventOptional.toString());
      final EventType currentEvent = EventType.valueOf(sagaEvent.getSagaEventState());
      final EventOutcome eventOutcome = EventOutcome.valueOf(sagaEvent.getSagaEventOutcome());
      final Event event = Event.builder()
        .sagaId(saga.getSagaId())
        .eventOutcome(eventOutcome)
        .eventType(currentEvent)
        .eventPayload(sagaEvent.getSagaEventResponse())
        .build();
      final Optional<SagaEventState<T>> sagaEventState = this.findNextSagaEventState(currentEvent, eventOutcome);
      if (sagaEventState.isPresent()) {
        log.trace(SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT, sagaEventState.get().getNextEventType(), event.toString());
        this.invokeNextEvent(event, saga, t, sagaEventState.get());
      }
    }
  }

  /**
   * This method will restart the saga process from the beginning. which could occur due to various reasons
   *
   * @param saga the model object.
   * @param t    the payload string as an object
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  private void replayFromBeginning(final Saga saga, final T t) throws InterruptedException, TimeoutException, IOException {
    final Event event = Event.builder()
      .eventOutcome(INITIATE_SUCCESS)
      .eventType(INITIATED)
      .sagaId(saga.getSagaId())
      .penRequestID(saga.getPenRequestId() != null ? saga.getPenRequestId().toString() : "")
      .studentRequestID(saga.getProfileRequestId() != null ? saga.getProfileRequestId().toString() : "")
      .build();
    final Optional<SagaEventState<T>> sagaEventState = this.findNextSagaEventState(INITIATED, INITIATE_SUCCESS);
    if (sagaEventState.isPresent()) {
      log.trace(SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT, sagaEventState.get().getNextEventType(), event.toString());
      this.invokeNextEvent(event, saga, t, sagaEventState.get());
    }
  }

  /**
   * this method is called if there is a new message on this specific topic which this service is listening.
   *
   * @param event the event in the topic received as a json string and then converted to {@link Event}
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */

  @Override
  @Async("async-task-executor")
  @Transactional
  public void executeSagaEvent(@NotNull final Event event) throws InterruptedException, IOException, TimeoutException {
    log.trace("executing saga event {}", event);
    if (this.sagaEventExecutionNotRequired(event)) {
      log.trace("Execution is not required for this message returning EVENT is :: {}", event.toString());
      return;
    }
    this.broadcastSagaInitiatedMessage(event);
    final var sagaOptional = this.getSagaService().findSagaById(event.getSagaId());
    if (sagaOptional.isPresent()) {
      val saga = sagaOptional.get();
      if (!COMPLETED.toString().equalsIgnoreCase(sagaOptional.get().getStatus()) && !FORCE_STOPPED.toString().equalsIgnoreCase(sagaOptional.get().getStatus())) {//possible duplicate message or force stop scenario check
        final var sagaEventState = this.findNextSagaEventState(event.getEventType(), event.getEventOutcome());
        log.trace("found next event as {}", sagaEventState);
        if (sagaEventState.isPresent()) {
          this.process(event, saga, sagaEventState.get());
        } else {
          log.error("This should not have happened, please check that both the saga api and all the participating apis are in sync in terms of events and their outcomes. {}", event.toString()); // more explicit error message,
        }
      } else {
        log.info("got message to process saga for saga ID :: {} but saga is already :: {}", saga.getSagaId(), saga.getStatus());
      }
    } else {
      log.error("Saga process without DB record is not expected. {}", event);
    }
  }

  /**
   * DONT DO ANYTHING the message was broad-casted for the frontend listeners, that a saga process has started or completed or FORCE_STOPPED.
   *
   * @param event the event object received from queue.
   * @return true if this message need not be processed further.
   */
  private boolean sagaEventExecutionNotRequired(@NotNull final Event event) {
    return (event.getEventType() == INITIATED && event.getEventOutcome() == INITIATE_SUCCESS && SELF.equalsIgnoreCase(event.getReplyTo()))
      || (event.getEventType() == MARK_SAGA_COMPLETE && event.getEventOutcome() == SAGA_COMPLETED)
      || (event.getEventType() == UNLINK_PEN_REQUEST && event.getEventOutcome() == SAGA_FORCE_STOPPED);
  }

  private void broadcastSagaInitiatedMessage(@NotNull final Event event) throws InterruptedException, IOException, TimeoutException {
    // !SELF.equalsIgnoreCase(event.getReplyTo()):- this check makes sure it is not broadcast-ed infinitely.
    if (event.getEventType() == INITIATED && event.getEventOutcome() == INITIATE_SUCCESS && !SELF.equalsIgnoreCase(event.getReplyTo())) {
      final var notificationEvent = new NotificationEvent();
      BeanUtils.copyProperties(event, notificationEvent);
      notificationEvent.setSagaStatus(INITIATED.toString());
      notificationEvent.setReplyTo(SELF);
      notificationEvent.setSagaName(this.getSagaName());
      this.postMessageToTopic(this.getTopicToSubscribe(), notificationEvent);
    }
  }

  /**
   * this method finds the next event that needs to be executed.
   *
   * @param currentEvent current event
   * @param eventOutcome event outcome.
   * @return {@link Optional<SagaEventState>}
   */
  protected Optional<SagaEventState<T>> findNextSagaEventState(final EventType currentEvent, final EventOutcome eventOutcome) {
    val sagaEventStates = this.nextStepsToExecute.get(currentEvent);
    return sagaEventStates == null ? Optional.empty() : sagaEventStates.stream().filter(el -> el.getCurrentEventOutcome() == eventOutcome).findFirst();
  }

  /**
   * this method starts the process of saga event execution.
   *
   * @param event          the current event.
   * @param saga           the model object.
   * @param sagaEventState the next next event from {@link BaseOrchestrator#nextStepsToExecute}
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void process(@NotNull final Event event, final Saga saga, final SagaEventState<T> sagaEventState) throws InterruptedException, TimeoutException, IOException {
    final T sagaData = JsonUtil.getJsonObjectFromString(this.clazz, saga.getPayload());
    if (!saga.getSagaState().equalsIgnoreCase(COMPLETED.toString())
      && this.isNotProcessedEvent(event.getEventType(), saga, this.nextStepsToExecute.keySet())) {
      log.info(SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT, sagaEventState.getNextEventType(), event.toString());
      this.invokeNextEvent(event, saga, sagaData, sagaEventState);
    } else {
      log.info("ignoring this message as we have already processed it or it is completed. {}", event.toString()); // it is expected to receive duplicate message in saga pattern, system should be designed to handle duplicates.
    }
  }

  /**
   * this method will invoke the next event in the {@link BaseOrchestrator#nextStepsToExecute}
   *
   * @param event          the current event.
   * @param saga           the model object.
   * @param sagaData       the payload string
   * @param sagaEventState the next next event from {@link BaseOrchestrator#nextStepsToExecute}
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void invokeNextEvent(final Event event, final Saga saga, final T sagaData, final SagaEventState<T> sagaEventState) throws InterruptedException, TimeoutException, IOException {
    final SagaStep<T> stepToExecute = sagaEventState.getStepToExecute();
    stepToExecute.apply(event, saga, sagaData);
  }

  public abstract void populateStepsToExecuteMap();
}
