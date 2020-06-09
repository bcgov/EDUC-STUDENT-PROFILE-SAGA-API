package ca.bc.gov.educ.api.student.profile.saga.orchestrator;

import ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome;
import ca.bc.gov.educ.api.student.profile.saga.constants.EventType;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessageSubscriber;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.poll.EventTaskScheduler;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.Event;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome.INITIATE_SUCCESS;
import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.COMPLETED;
import static lombok.AccessLevel.PROTECTED;

@Slf4j
public abstract class BaseOrchestrator<T> {
  protected static final String SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT = "system is going to execute next event :: {} for current event {}";
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

  protected List<SagaEventState<T>> createSingleCollectionEventState(EventOutcome eventOutcome, Boolean isCompensating, EventType nextEventType, SagaStep<T> stepToExecute) {
    return Collections.singletonList(buildSagaEventState(eventOutcome, isCompensating, nextEventType, stepToExecute));
  }


  protected SagaEventState<T> buildSagaEventState(EventOutcome eventOutcome, Boolean isCompensating, EventType nextEventType, SagaStep<T> stepToExecute) {
    return SagaEventState.<T>builder().currentEventOutcome(eventOutcome).isCompensating(isCompensating).nextEventType(nextEventType).stepToExecute(stepToExecute).build();
  }

  public BaseOrchestrator(SagaService sagaService, MessagePublisher messagePublisher, MessageSubscriber messageSubscriber, EventTaskScheduler taskScheduler, Class<T> clazz, String sagaName, String topicToSubscribe) {
    this.sagaService = sagaService;
    this.messagePublisher = messagePublisher;
    this.clazz = clazz;
    this.sagaName = sagaName;
    this.topicToSubscribe = topicToSubscribe;
    messageSubscriber.subscribe(topicToSubscribe, this::executeSagaEvent);
    taskScheduler.registerSagaOrchestrators(sagaName, this);
    populateNextStepsMap();
  }


  protected BaseOrchestrator<T> registerStepToExecute(EventType initEvent, EventOutcome outcome, Boolean isCompensating, EventType nextEvent, SagaStep<T> stepToExecute) {
    if(this.nextStepsToExecute.containsKey(initEvent)) {
      List<SagaEventState<T>> states = this.nextStepsToExecute.get(initEvent);
      states.add(buildSagaEventState(outcome, isCompensating, nextEvent, stepToExecute));
    } else {
      this.nextStepsToExecute.put(initEvent, createSingleCollectionEventState(outcome, isCompensating, nextEvent, stepToExecute));
    }
    return this;
  }
  protected BaseOrchestrator<T> step(EventType initEvent, EventOutcome outcome, EventType nextEvent, SagaStep<T> stepToExecute) {
    return registerStepToExecute(initEvent, outcome, false, nextEvent, stepToExecute);
  }
  /**
   * this is a simple and convenient method to trigger builder pattern in the child classes.
   *
   * @return {@link BaseOrchestrator}
   */
  protected BaseOrchestrator<T> stepBuilder() {
    return this;
  }

  protected boolean isNotProcessedEvent(EventType currentEventType, Saga saga, Set<EventType> eventTypes) {
    EventType eventTypeInDB = EventType.valueOf(saga.getSagaState());
    List<EventType> events = new LinkedList<>(eventTypes);
    int dbEventIndex = events.indexOf(eventTypeInDB);
    int currentEventIndex = events.indexOf(currentEventType);
    return currentEventIndex >= dbEventIndex;
  }

  protected SagaEvent createEventState(@NotNull Saga saga, @NotNull EventType eventType, @NotNull EventOutcome eventOutcome, String eventPayload) {
    return SagaEvent.builder()
            .createDate(LocalDateTime.now())
            .createUser(sagaName)
            .updateDate(LocalDateTime.now())
            .updateUser(sagaName)
            .saga(saga)
            .sagaEventOutcome(eventOutcome.toString())
            .sagaEventState(eventType.toString())
            .sagaStepNumber(calculateStep(saga))
            .sagaEventResponse(eventPayload == null ? "" : eventPayload)
            .build();
  }

  protected void markSagaComplete(Event event, Saga saga, T sagaData) {
    log.trace("payload is {}", sagaData);
    SagaEvent sagaEvent = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(COMPLETED.toString());
    saga.setStatus(COMPLETED.toString());
    saga.setUpdateDate(LocalDateTime.now());
    getSagaService().updateAttachedSagaWithEvents(saga, sagaEvent);
  }

  private int calculateStep(Saga saga) {
    val sagaStates = getSagaService().findAllSagaStates(saga);
    return (sagaStates.size() + 1);
  }

  protected void postMessageToTopic(String topicName, Event nextEvent) throws InterruptedException, IOException, TimeoutException {
    getMessagePublisher().dispatchMessage(topicName, JsonUtil.getJsonStringFromObject(nextEvent).getBytes());
  }

  protected Optional<SagaEvent> findTheLastEventOccurred(List<SagaEvent> eventStates) {
    int step = eventStates.stream().map(SagaEvent::getSagaStepNumber).mapToInt(x -> x).max().orElse(0);
    return eventStates.stream().filter(element -> element.getSagaStepNumber() == step).findFirst();
  }

  @Async
  @Transactional
  public void replaySaga(Saga saga) throws IOException, InterruptedException, TimeoutException {
    List<SagaEvent> eventStates = getSagaService().findAllSagaStates(saga);
    T t = JsonUtil.getJsonObjectFromString(clazz, saga.getPayload());
    if (eventStates.isEmpty()) { //process did not start last time, lets start from beginning.
      replayFromBeginning(saga, t);
    } else {
      replayFromLastEvent(saga, eventStates, t);
    }
  }

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


  @Async
  @Transactional
  public void executeSagaEvent(@NotNull Event event) throws InterruptedException, IOException, TimeoutException {
    log.trace("executing saga event {}", event);
    Optional<Saga> sagaOptional = getSagaService().findSagaById(event.getSagaId());
    if (sagaOptional.isPresent() && !COMPLETED.toString().equalsIgnoreCase(sagaOptional.get().getStatus())) { //possible duplicate message.
      val saga = sagaOptional.get();
      Optional<SagaEventState<T>> sagaEventState = findNextSagaEventState(event.getEventType(), event.getEventOutcome());
      log.trace("found next event as {}", sagaEventState);
      if (sagaEventState.isPresent()) {
        process(event, saga, sagaEventState.get());
      } else {
        log.error("This should not have happened, please check that both the saga api and all the participating apis are in sync in terms of events and their outcomes. {}", event.toString()); // more explicit error message,
      }
    }
  }

  protected Optional<SagaEventState<T>> findNextSagaEventState(EventType currentEvent, EventOutcome eventOutcome) {
    val sagaEventStates = nextStepsToExecute.get(currentEvent);
    return sagaEventStates.stream().filter(el -> el.getCurrentEventOutcome() == eventOutcome).findFirst();
  }

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

  protected void invokeNextEvent(Event event, Saga saga, T sagaData, SagaEventState<T> sagaEventState) throws InterruptedException, TimeoutException, IOException {
    SagaStep<T> stepToExecute = sagaEventState.getStepToExecute();
    stepToExecute.apply(event, saga, sagaData);
  }

  protected abstract void populateNextStepsMap();
}
