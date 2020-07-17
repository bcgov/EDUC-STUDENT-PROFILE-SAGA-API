package ca.bc.gov.educ.api.student.profile.saga.orchestrator.base;

import ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome;
import ca.bc.gov.educ.api.student.profile.saga.constants.EventType;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessageSubscriber;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.schedulers.EventTaskScheduler;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.NotificationEvent;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
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
public abstract class BaseOrchestrator<T> {
  protected static final String SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT = "system is going to execute next event :: {} for current event {}";
  public static final String SELF = "SELF";
  @Getter(PROTECTED)
  private final SagaService sagaService;
  @Getter(PROTECTED)
  private final MessagePublisher messagePublisher;
  protected final Class<T> clazz;

  @Getter(PROTECTED)
  private final String sagaName;
  @Getter(PROTECTED)
  private final String topicToSubscribe;

  protected final Map<EventType, List<SagaEventState<T>>> nextStepsToExecute = new LinkedHashMap<>();

  public BaseOrchestrator(SagaService sagaService, MessagePublisher messagePublisher, MessageSubscriber messageSubscriber, EventTaskScheduler taskScheduler, Class<T> clazz, String sagaName, String topicToSubscribe) {
    this.sagaService = sagaService;
    this.messagePublisher = messagePublisher;
    this.clazz = clazz;
    this.sagaName = sagaName;
    this.topicToSubscribe = topicToSubscribe;
    messageSubscriber.subscribe(topicToSubscribe, this::executeSagaEvent);
    taskScheduler.registerSagaOrchestrators(sagaName, this);
    populateStepsToExecuteMap();
  }

  protected List<SagaEventState<T>> createSingleCollectionEventState(EventOutcome eventOutcome, Boolean isCompensating, EventType nextEventType, SagaStep<T> stepToExecute) {
    List<SagaEventState<T>> eventStates = new ArrayList<>();
    eventStates.add(buildSagaEventState(eventOutcome, isCompensating, nextEventType, stepToExecute));
    return eventStates;
  }


  protected SagaEventState<T> buildSagaEventState(EventOutcome eventOutcome, Boolean isCompensating, EventType nextEventType, SagaStep<T> stepToExecute) {
    return SagaEventState.<T>builder().currentEventOutcome(eventOutcome).isCompensating(isCompensating).nextEventType(nextEventType).stepToExecute(stepToExecute).build();
  }


  protected BaseOrchestrator<T> registerStepToExecute(EventType initEvent, EventOutcome outcome, Boolean isCompensating, EventType nextEvent, SagaStep<T> stepToExecute) {
    if (this.nextStepsToExecute.containsKey(initEvent)) {
      List<SagaEventState<T>> states = this.nextStepsToExecute.get(initEvent);
      states.add(buildSagaEventState(outcome, isCompensating, nextEvent, stepToExecute));
    } else {
      this.nextStepsToExecute.put(initEvent, createSingleCollectionEventState(outcome, isCompensating, nextEvent, stepToExecute));
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
  public BaseOrchestrator<T> step(EventType currentEvent, EventOutcome outcome, EventType nextEvent, SagaStep<T> stepToExecute) {
    return registerStepToExecute(currentEvent, outcome, false, nextEvent, stepToExecute);
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
  protected boolean isNotProcessedEvent(EventType currentEventType, Saga saga, Set<EventType> eventTypes) {
    EventType eventTypeInDB = EventType.valueOf(saga.getSagaState());
    List<EventType> events = new LinkedList<>(eventTypes);
    int dbEventIndex = events.indexOf(eventTypeInDB);
    int currentEventIndex = events.indexOf(currentEventType);
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
  protected SagaEvent createEventState(@NotNull Saga saga, @NotNull EventType eventType, @NotNull EventOutcome eventOutcome, String eventPayload) {
    var user = sagaName.length() > 32 ? sagaName.substring(0, 32) : sagaName;
    return SagaEvent.builder()
        .createDate(LocalDateTime.now())
        .createUser(user)
        .updateDate(LocalDateTime.now())
        .updateUser(user)
        .saga(saga)
        .sagaEventOutcome(eventOutcome.toString())
        .sagaEventState(eventType.toString())
        .sagaStepNumber(calculateStep(saga))
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
  protected void markSagaComplete(Event event, Saga saga, T sagaData) throws InterruptedException, TimeoutException, IOException {
    log.trace("payload is {}", sagaData);
    var finalEvent = new NotificationEvent();
    BeanUtils.copyProperties(event, finalEvent);
    finalEvent.setEventType(MARK_SAGA_COMPLETE);
    finalEvent.setEventOutcome(SAGA_COMPLETED);
    finalEvent.setSagaStatus(COMPLETED.toString());
    finalEvent.setStudentRequestID(saga.getProfileRequestId() != null ? saga.getProfileRequestId().toString() : "");
    finalEvent.setPenRequestID(saga.getPenRequestId() != null ? saga.getPenRequestId().toString() : "");
    finalEvent.setSagaName(getSagaName());
    finalEvent.setEventPayload(""); // no need to send payload as it is not required by the subscribers.
    postMessageToTopic(getTopicToSubscribe(), finalEvent);
    SagaEvent sagaEvent = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(COMPLETED.toString());
    saga.setStatus(COMPLETED.toString());
    saga.setUpdateDate(LocalDateTime.now());
    getSagaService().updateAttachedSagaWithEvents(saga, sagaEvent);

  }

  /**
   * calculate step number
   *
   * @param saga the model object.
   * @return step number that was calculated.
   */
  private int calculateStep(Saga saga) {
    val sagaStates = getSagaService().findAllSagaStates(saga);
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
  protected void postMessageToTopic(String topicName, Event nextEvent) throws InterruptedException, IOException, TimeoutException {
    getMessagePublisher().dispatchMessage(topicName, JsonUtil.getJsonStringFromObject(nextEvent).getBytes());
  }

  /**
   * it finds the last event that was processed successfully for this saga.
   *
   * @param eventStates event states corresponding to the Saga.
   * @return {@link SagaEvent} if found else null.
   */
  protected Optional<SagaEvent> findTheLastEventOccurred(List<SagaEvent> eventStates) {
    int step = eventStates.stream().map(SagaEvent::getSagaStepNumber).mapToInt(x -> x).max().orElse(0);
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
  @Async
  @Transactional
  public void replaySaga(Saga saga) throws IOException, InterruptedException, TimeoutException {
    var eventStates = getSagaService().findAllSagaStates(saga);
    T t = JsonUtil.getJsonObjectFromString(clazz, saga.getPayload());
    if (eventStates.isEmpty()) { //process did not start last time, lets start from beginning.
      replayFromBeginning(saga, t);
    } else {
      replayFromLastEvent(saga, eventStates, t);
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
  private void replayFromLastEvent(Saga saga, List<SagaEvent> eventStates, T t) throws InterruptedException, TimeoutException, IOException {
    val sagaEventOptional = findTheLastEventOccurred(eventStates);
    if (sagaEventOptional.isPresent()) {
      val sagaEvent = sagaEventOptional.get();
      log.trace(sagaEventOptional.toString());
      EventType currentEvent = EventType.valueOf(sagaEvent.getSagaEventState());
      EventOutcome eventOutcome = EventOutcome.valueOf(sagaEvent.getSagaEventOutcome());
      Event event = Event.builder()
          .eventOutcome(eventOutcome)
          .eventType(currentEvent)
          .eventPayload(sagaEvent.getSagaEventResponse())
          .build();
      Optional<SagaEventState<T>> sagaEventState = findNextSagaEventState(currentEvent, eventOutcome);
      if (sagaEventState.isPresent()) {
        log.trace(SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT, sagaEventState.get().getNextEventType(), event.toString());
        invokeNextEvent(event, saga, t, sagaEventState.get());
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
  private void replayFromBeginning(Saga saga, T t) throws InterruptedException, TimeoutException, IOException {
    Event event = Event.builder()
        .eventOutcome(INITIATE_SUCCESS)
        .eventType(INITIATED)
        .build();
    Optional<SagaEventState<T>> sagaEventState = findNextSagaEventState(INITIATED, INITIATE_SUCCESS);
    if (sagaEventState.isPresent()) {
      log.trace(SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT, sagaEventState.get().getNextEventType(), event.toString());
      invokeNextEvent(event, saga, t, sagaEventState.get());
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

  @Async
  @Transactional
  public void executeSagaEvent(@NotNull Event event) throws InterruptedException, IOException, TimeoutException {
    log.trace("executing saga event {}", event);
    if (sagaEventExecutionNotRequired(event)) {
      log.trace("Execution is not required for this message returning EVENT is :: {}", event.toString());
      return;
    }
    broadcastSagaInitiatedMessage(event);
    var sagaOptional = getSagaService().findSagaById(event.getSagaId());
    if (sagaOptional.isPresent() ) {
      val saga = sagaOptional.get();
      if(!COMPLETED.toString().equalsIgnoreCase(sagaOptional.get().getStatus()) && !FORCE_STOPPED.toString().equalsIgnoreCase(sagaOptional.get().getStatus())){//possible duplicate message or force stop scenario check
        var sagaEventState = findNextSagaEventState(event.getEventType(), event.getEventOutcome());
        log.trace("found next event as {}", sagaEventState);
        if (sagaEventState.isPresent()) {
          process(event, saga, sagaEventState.get());
        } else {
          log.error("This should not have happened, please check that both the saga api and all the participating apis are in sync in terms of events and their outcomes. {}", event.toString()); // more explicit error message,
        }
      }else {
        log.info("got message to process saga for saga ID :: {} but saga is already :: {}", saga.getSagaId() ,saga.getStatus());
      }
    }else {
      log.error("Saga process without DB record is not expected. {}", event);
    }
  }

  /**
   * DONT DO ANYTHING the message was broad-casted for the frontend listeners, that a saga process has started or completed or FORCE_STOPPED.
   * @param event the event object received from queue.
   * @return true if this message need not be processed further.
   */
  private boolean sagaEventExecutionNotRequired(@NotNull Event event) {
    return (event.getEventType() == INITIATED && event.getEventOutcome() == INITIATE_SUCCESS && SELF.equalsIgnoreCase(event.getReplyTo()))
        || (event.getEventType() == MARK_SAGA_COMPLETE && event.getEventOutcome() == SAGA_COMPLETED)
        || (event.getEventType() == UNLINK_PEN_REQUEST && event.getEventOutcome() == SAGA_FORCE_STOPPED);
  }

  private void broadcastSagaInitiatedMessage(@NotNull Event event) throws InterruptedException, IOException, TimeoutException {
    // !SELF.equalsIgnoreCase(event.getReplyTo()):- this check makes sure it is not broadcast-ed infinitely.
    if (event.getEventType() == INITIATED && event.getEventOutcome() == INITIATE_SUCCESS && !SELF.equalsIgnoreCase(event.getReplyTo())) {
      var notificationEvent = new NotificationEvent();
      BeanUtils.copyProperties(event, notificationEvent);
      notificationEvent.setSagaStatus(INITIATED.toString());
      notificationEvent.setReplyTo(SELF);
      notificationEvent.setSagaName(getSagaName());
      postMessageToTopic(getTopicToSubscribe(), notificationEvent);
    }
  }

  /**
   * this method finds the next event that needs to be executed.
   *
   * @param currentEvent current event
   * @param eventOutcome event outcome.
   * @return {@link Optional<SagaEventState>}
   */
  protected Optional<SagaEventState<T>> findNextSagaEventState(EventType currentEvent, EventOutcome eventOutcome) {
    val sagaEventStates = nextStepsToExecute.get(currentEvent);
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
  protected void process(@NotNull Event event, Saga saga, SagaEventState<T> sagaEventState) throws InterruptedException, TimeoutException, IOException {
    T sagaData = JsonUtil.getJsonObjectFromString(clazz, saga.getPayload());
    if (!saga.getSagaState().equalsIgnoreCase(COMPLETED.toString())
        && isNotProcessedEvent(event.getEventType(), saga, this.nextStepsToExecute.keySet())) {
      log.info(SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT, sagaEventState.getNextEventType(), event.toString());
      invokeNextEvent(event, saga, sagaData, sagaEventState);
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
  protected void invokeNextEvent(Event event, Saga saga, T sagaData, SagaEventState<T> sagaEventState) throws InterruptedException, TimeoutException, IOException {
    SagaStep<T> stepToExecute = sagaEventState.getStepToExecute();
    stepToExecute.apply(event, saga, sagaData);
  }

  public abstract void populateStepsToExecuteMap();
}
