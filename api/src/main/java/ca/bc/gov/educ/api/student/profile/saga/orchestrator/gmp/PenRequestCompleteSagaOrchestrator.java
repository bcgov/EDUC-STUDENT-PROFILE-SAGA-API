package ca.bc.gov.educ.api.student.profile.saga.orchestrator.gmp;


import ca.bc.gov.educ.api.student.profile.saga.constants.EventType;
import ca.bc.gov.educ.api.student.profile.saga.mappers.StudentSagaDataMapper;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessageSubscriber;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.schedulers.EventTaskScheduler;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.NotificationEvent;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenReqEmailSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestCompleteSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.DigitalIdSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.StudentSagaData;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.*;


@Component
@Slf4j
public class PenRequestCompleteSagaOrchestrator extends BasePenReqSagaOrchestrator<PenRequestCompleteSagaData> {
  private static final StudentSagaDataMapper studentSagaDataMapper = StudentSagaDataMapper.mapper;

  @Autowired
  public PenRequestCompleteSagaOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, final MessageSubscriber messageSubscriber, final EventTaskScheduler taskScheduler) {
    super(sagaService, messagePublisher, messageSubscriber, taskScheduler, PenRequestCompleteSagaData.class, PEN_REQUEST_COMPLETE_SAGA.toString(), PEN_REQUEST_COMPLETE_SAGA_TOPIC.toString());
  }

  /**
   * this is the source of truth for this particular saga flow.
   */
  @Override
  public void populateStepsToExecuteMap() {
    stepBuilder()
        .step(INITIATED, INITIATE_SUCCESS, GET_STUDENT, this::executeGetStudent)
        .step(GET_STUDENT, STUDENT_FOUND, UPDATE_STUDENT, this::executeUpdateStudent)
        .step(GET_STUDENT, STUDENT_NOT_FOUND, CREATE_STUDENT, this::executeCreateStudent)
        .step(CREATE_STUDENT, STUDENT_CREATED, GET_DIGITAL_ID, this::executeGetDigitalId)
        .step(UPDATE_STUDENT, STUDENT_UPDATED, GET_DIGITAL_ID, this::executeGetDigitalId)
        .step(GET_DIGITAL_ID, DIGITAL_ID_FOUND, UPDATE_DIGITAL_ID, this::executeUpdateDigitalId)
        .step(UPDATE_DIGITAL_ID, DIGITAL_ID_UPDATED, GET_PEN_REQUEST, this::executeGetPenRequest)
        .step(GET_PEN_REQUEST, PEN_REQUEST_FOUND, UPDATE_PEN_REQUEST, this::executeUpdatePenRequest)
        .step(UPDATE_PEN_REQUEST, PEN_REQUEST_UPDATED, NOTIFY_STUDENT_PEN_REQUEST_COMPLETE, this::executeNotifyStudentPenRequestComplete)
        .step(NOTIFY_STUDENT_PEN_REQUEST_COMPLETE, STUDENT_NOTIFIED, MARK_SAGA_COMPLETE, this::markSagaComplete);
  }

  /**
   * this method will update the payload according the saga type. here it will update for comment saga.
   *
   * @param penRequestSagaData         the model object.
   * @param penRequestCompleteSagaData the payload as the object.
   */
  @Override
  protected void updatePenRequestPayload(PenRequestSagaData penRequestSagaData, PenRequestCompleteSagaData penRequestCompleteSagaData) {
    penRequestSagaData.setPen(penRequestCompleteSagaData.getPen());
    penRequestSagaData.setReviewer(penRequestCompleteSagaData.getReviewer());
    penRequestSagaData.setCompleteComment(penRequestCompleteSagaData.getCompleteComment());
    penRequestSagaData.setDemogChanged(penRequestCompleteSagaData.getDemogChanged());
    penRequestSagaData.setBcscAutoMatchDetails(penRequestCompleteSagaData.getBcscAutoMatchDetails());
    penRequestSagaData.setBcscAutoMatchOutcome(penRequestCompleteSagaData.getBcscAutoMatchOutcome());
    penRequestSagaData.setPenRequestStatusCode(penRequestCompleteSagaData.getPenRequestStatusCode());
    penRequestSagaData.setStatusUpdateDate(LocalDateTime.now().toString());
    penRequestSagaData.setUpdateUser(PEN_REQUEST_COMPLETE_SAGA.toString());
  }

  /**
   * this method will return the pen request IDm to be send in the message.
   *
   * @param penRequestCompleteSagaData the payload as the object.
   * @return pen request id as string value.
   */
  @Override
  protected String updateGetPenRequestPayload(PenRequestCompleteSagaData penRequestCompleteSagaData) {
    return penRequestCompleteSagaData.getPenRequestID();
  }


  /**
   * this is called after either create student or update student.
   * update student id in the original payload, if the previous event was create student. if previous event was update student ,
   * system has already updated the student id in original payload, please look at {@link #executeUpdateStudent method}.
   *
   * @param event                      current event
   * @param saga             the model object.
   * @param penRequestCompleteSagaData the payload as object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  private void executeGetDigitalId(Event event, Saga saga, PenRequestCompleteSagaData penRequestCompleteSagaData) throws InterruptedException, TimeoutException, IOException {

    if (event.getEventType() == CREATE_STUDENT) {
      StudentSagaData studentDataFromEventResponse = JsonUtil.getJsonObjectFromString(StudentSagaData.class, event.getEventPayload());
      penRequestCompleteSagaData.setStudentID(studentDataFromEventResponse.getStudentID()); //update the payload of the original event request with student id.
      saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestCompleteSagaData));
    }
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(GET_DIGITAL_ID.toString()); // set current event as saga state.
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
            .eventType(GET_DIGITAL_ID)
            .replyTo(PEN_REQUEST_COMPLETE_SAGA_TOPIC.toString())
            .eventPayload(penRequestCompleteSagaData.getDigitalID())
            .build();
    postMessageToTopic(DIGITAL_ID_API_TOPIC.toString(), nextEvent);
    log.info("message sent to DIGITAL_ID_API_TOPIC for GET_DIGITAL_ID Event.");
  }

  /**
   * This event will be after get student event, if student is found via pen.
   * we will be passing in the student data to update which we got from saga payload.
   *
   * @param event                      current event
   * @param saga             the model object.
   * @param penRequestCompleteSagaData the payload as object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  private void executeUpdateStudent(Event event, Saga saga, PenRequestCompleteSagaData penRequestCompleteSagaData) throws IOException, InterruptedException, TimeoutException {
    StudentSagaData studentSagaData = studentSagaDataMapper.toStudentSaga(penRequestCompleteSagaData); // get the student data from saga payload.
    StudentSagaData studentDataFromEventResponse = JsonUtil.getJsonObjectFromString(StudentSagaData.class, event.getEventPayload());
    studentSagaData.setStudentID(studentDataFromEventResponse.getStudentID()); // update the student ID so that update call will have proper identifier.
    studentSagaData.setUpdateUser(PEN_REQUEST_COMPLETE_SAGA.toString());
    penRequestCompleteSagaData.setStudentID(studentDataFromEventResponse.getStudentID()); //update the payload of the original event request with student id.
    saga.setSagaState(UPDATE_STUDENT.toString());
    saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestCompleteSagaData));
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    log.info("message sent to STUDENT_API_TOPIC for UPDATE_STUDENT Event.");
    delegateMessagePostingForStudent(saga, studentSagaData, UPDATE_STUDENT);
  }

  /**
   * This event will be after get student event, if student is not found via pen.
   *
   * @param event                      current event
   * @param saga             the model object.
   * @param penRequestCompleteSagaData the payload as object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  private void executeCreateStudent(Event event, Saga saga, PenRequestCompleteSagaData penRequestCompleteSagaData) throws IOException, InterruptedException, TimeoutException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(CREATE_STUDENT.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    StudentSagaData studentSagaData = studentSagaDataMapper.toStudentSaga(penRequestCompleteSagaData);
    studentSagaData.setUpdateUser(PEN_REQUEST_COMPLETE_SAGA.toString());
    studentSagaData.setCreateUser(PEN_REQUEST_COMPLETE_SAGA.toString());
    log.info("message sent to STUDENT_API_TOPIC for CREATE_STUDENT Event.");
    delegateMessagePostingForStudent(saga, studentSagaData, CREATE_STUDENT);

  }

  /**
   * common method for posting message to student api topic.
   *
   * @param saga  the model object.
   * @param studentSagaData the payload which will be passed to student api topic as json string.
   * @param eventType       the type of event whether CREATE_STUDENT or UPDATE_STUDENT
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  private void delegateMessagePostingForStudent(Saga saga, StudentSagaData studentSagaData, EventType eventType) throws InterruptedException, IOException, TimeoutException {
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
            .eventType(eventType)
            .replyTo(PEN_REQUEST_COMPLETE_SAGA_TOPIC.toString())
            .eventPayload(JsonUtil.getJsonStringFromObject(studentSagaData))
            .build();
    postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);

  }

  /**
   * this method will send a message to student api topic to get student details based on PEN.
   *
   * @param event                      current event
   * @param saga             the model object.
   * @param penRequestCompleteSagaData the payload as object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  private void executeGetStudent(Event event, Saga saga, PenRequestCompleteSagaData penRequestCompleteSagaData) throws IOException, InterruptedException, TimeoutException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setStatus(IN_PROGRESS.toString());
    saga.setSagaState(GET_STUDENT.toString()); // set current event as saga state.
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
            .eventType(GET_STUDENT)
            .replyTo(PEN_REQUEST_COMPLETE_SAGA_TOPIC.toString())
            .eventPayload(penRequestCompleteSagaData.getPen())
            .build();
    postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_API_TOPIC for GET_STUDENT Event.");
  }


  /**
   * this is executed after get digital id, so the event response would contain the entire digital id payload, this method will only update the student Id.
   *
   * @param event                      current event
   * @param saga             the model object.
   * @param penRequestCompleteSagaData the payload as object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  private void executeUpdateDigitalId(Event event, Saga saga, PenRequestCompleteSagaData penRequestCompleteSagaData) throws IOException, InterruptedException, TimeoutException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(UPDATE_DIGITAL_ID.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    DigitalIdSagaData digitalIdSagaData = JsonUtil.getJsonObjectFromString(DigitalIdSagaData.class, event.getEventPayload());
    digitalIdSagaData.setStudentID(penRequestCompleteSagaData.getStudentID());
    digitalIdSagaData.setUpdateUser(PEN_REQUEST_COMPLETE_SAGA.toString());
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
            .eventType(UPDATE_DIGITAL_ID)
            .replyTo(PEN_REQUEST_COMPLETE_SAGA_TOPIC.toString())
            .eventPayload(JsonUtil.getJsonStringFromObject(digitalIdSagaData))
            .build();
    postMessageToTopic(DIGITAL_ID_API_TOPIC.toString(), nextEvent);
    log.info("message sent to DIGITAL_ID_API_TOPIC for UPDATE_DIGITAL_ID Event.");

  }

  /**
   * this method will send message to pen request email api to send email to the student that the pen request is now completed.
   *
   * @param event                      current event
   * @param saga             the model object.
   * @param penRequestCompleteSagaData the payload as object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  private void executeNotifyStudentPenRequestComplete(Event event, Saga saga, PenRequestCompleteSagaData penRequestCompleteSagaData) throws IOException, InterruptedException, TimeoutException {
    SagaEvent eventStates = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(NOTIFY_STUDENT_PEN_REQUEST_COMPLETE.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
            .eventType(NOTIFY_STUDENT_PEN_REQUEST_COMPLETE)
            .replyTo(PEN_REQUEST_COMPLETE_SAGA_TOPIC.toString())
            .eventPayload(buildPenReqEmailSagaData(penRequestCompleteSagaData))
            .build();
    postMessageToTopic(PROFILE_REQUEST_EMAIL_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PROFILE_REQUEST_EMAIL_API_TOPIC for NOTIFY_STUDENT_PEN_REQUEST_COMPLETE Event.");

  }

  /**
   * this method builds the email payload as json string.
   *
   * @param penRequestCompleteSagaData the payload as object.
   * @return the string representation of the json object.
   * @throws JsonProcessingException if it is unable to convert the object to json string.
   */
  private String buildPenReqEmailSagaData(PenRequestCompleteSagaData penRequestCompleteSagaData) throws JsonProcessingException {
    PenReqEmailSagaData penReqEmailSagaData = PenReqEmailSagaData.builder()
            .emailAddress(penRequestCompleteSagaData.getEmail())
            .firstName(penRequestCompleteSagaData.getLegalFirstName())
            .demographicsChanged("Y".equalsIgnoreCase(penRequestCompleteSagaData.getDemogChanged()))
            .identityType(penRequestCompleteSagaData.getIdentityType())
            .build();
    return JsonUtil.getJsonStringFromObject(penReqEmailSagaData);
  }

  /**
   * this method is called when the complete saga is in progress and staff members clicks on unlink. system will force stop the existing saga and start unlink saga.
   * @param saga the model object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  @Transactional
  public void publishSagaForceStopped(final Saga saga) throws InterruptedException, TimeoutException, IOException {

    var forceStoppedEvent = new NotificationEvent();
    forceStoppedEvent.setEventType(UNLINK_PEN_REQUEST);
    forceStoppedEvent.setEventOutcome(SAGA_FORCE_STOPPED);
    forceStoppedEvent.setSagaStatus(FORCE_STOPPED.toString());
    forceStoppedEvent.setPenRequestID(saga.getPenRequestId() != null ? saga.getPenRequestId().toString() : "");
    forceStoppedEvent.setSagaName(getSagaName());
    forceStoppedEvent.setSagaId(saga.getSagaId());
    postMessageToTopic(getTopicToSubscribe(), forceStoppedEvent);
  }

}
