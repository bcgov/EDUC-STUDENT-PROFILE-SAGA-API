package ca.bc.gov.educ.api.student.profile.saga.orchestrator.gmp;


import ca.bc.gov.educ.api.student.profile.saga.constants.EventType;
import ca.bc.gov.educ.api.student.profile.saga.mappers.v1.StudentSagaDataMapper;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.DigitalIdSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.NotificationEvent;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.StudentSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenReqEmailSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestCompleteSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestSagaData;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.PEN_REQUEST_COMPLETE_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.FORCE_STOPPED;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.*;


@Component
@Slf4j
public class PenRequestCompleteSagaOrchestrator extends BasePenReqSagaOrchestrator<PenRequestCompleteSagaData> {
  private static final StudentSagaDataMapper studentSagaDataMapper = StudentSagaDataMapper.mapper;
  private static final String HISTORY_ACTIVITY_CODE_GMP = "GMP";


  @Autowired
  public PenRequestCompleteSagaOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher) {
    super(sagaService, messagePublisher, PenRequestCompleteSagaData.class, PEN_REQUEST_COMPLETE_SAGA.toString(), PEN_REQUEST_COMPLETE_SAGA_TOPIC.toString());
  }

  /**
   * this is the source of truth for this particular saga flow.
   */
  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
      .step(INITIATED, INITIATE_SUCCESS, GET_PEN_REQUEST_DOCUMENT_METADATA, this::executeGetPenRequestDocuments)
      .step(GET_PEN_REQUEST_DOCUMENT_METADATA, PEN_REQUEST_DOCUMENTS_FOUND, GET_STUDENT, this::executeGetStudent)
      .step(GET_PEN_REQUEST_DOCUMENT_METADATA, PEN_REQUEST_DOCUMENTS_NOT_FOUND, GET_STUDENT, this::executeGetStudent)
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

  private void executeGetPenRequestDocuments(final Event event, final Saga saga, final PenRequestCompleteSagaData penRequestCompleteSagaData) throws IOException, InterruptedException, TimeoutException {
    val eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setStatus(IN_PROGRESS.toString());
    saga.setSagaState(GET_PEN_REQUEST_DOCUMENT_METADATA.toString()); // set current event as saga state.
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(GET_PEN_REQUEST_DOCUMENT_METADATA)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(penRequestCompleteSagaData.getPenRequestID())
      .build();
    this.postMessageToTopic(PEN_REQUEST_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PEN_REQUEST_API_TOPIC for GET_PEN_REQUEST_DOCUMENT_METADATA Event.");
  }

  /**
   * this method will update the payload according the saga type. here it will update for comment saga.
   *
   * @param penRequestSagaData         the model object.
   * @param penRequestCompleteSagaData the payload as the object.
   */
  @Override
  protected void updatePenRequestPayload(final PenRequestSagaData penRequestSagaData, final PenRequestCompleteSagaData penRequestCompleteSagaData) {
    penRequestSagaData.setPen(penRequestCompleteSagaData.getPen());
    penRequestSagaData.setReviewer(penRequestCompleteSagaData.getReviewer());
    penRequestSagaData.setCompleteComment(penRequestCompleteSagaData.getCompleteComment());
    penRequestSagaData.setDemogChanged(penRequestCompleteSagaData.getDemogChanged());
    penRequestSagaData.setBcscAutoMatchDetails(penRequestCompleteSagaData.getBcscAutoMatchDetails());
    penRequestSagaData.setBcscAutoMatchOutcome(penRequestCompleteSagaData.getBcscAutoMatchOutcome());
    penRequestSagaData.setPenRequestStatusCode(penRequestCompleteSagaData.getPenRequestStatusCode());
    penRequestSagaData.setStatusUpdateDate(LocalDateTime.now().toString());
    penRequestSagaData.setUpdateUser(penRequestCompleteSagaData.getUpdateUser());
  }

  /**
   * this method will return the pen request IDm to be send in the message.
   *
   * @param penRequestCompleteSagaData the payload as the object.
   * @return pen request id as string value.
   */
  @Override
  protected String updateGetPenRequestPayload(final PenRequestCompleteSagaData penRequestCompleteSagaData) {
    return penRequestCompleteSagaData.getPenRequestID();
  }


  /**
   * this is called after either create student or update student.
   * update student id in the original payload, if the previous event was create student. if previous event was update student ,
   * system has already updated the student id in original payload, please look at {@link #executeUpdateStudent method}.
   *
   * @param event                      current event
   * @param saga                       the model object.
   * @param penRequestCompleteSagaData the payload as object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void executeGetDigitalId(final Event event, final Saga saga, final PenRequestCompleteSagaData penRequestCompleteSagaData) throws InterruptedException, TimeoutException, IOException {

    if (event.getEventType() == CREATE_STUDENT) {
      val studentDataFromEventResponse = JsonUtil.getJsonObjectFromString(StudentSagaData.class, event.getEventPayload());
      penRequestCompleteSagaData.setStudentID(studentDataFromEventResponse.getStudentID()); //update the payload of the original event request with student id.
      saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestCompleteSagaData));
    }
    val eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(GET_DIGITAL_ID.toString()); // set current event as saga state.
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(GET_DIGITAL_ID)
      .replyTo(PEN_REQUEST_COMPLETE_SAGA_TOPIC.toString())
      .eventPayload(penRequestCompleteSagaData.getDigitalID())
      .build();
    this.postMessageToTopic(DIGITAL_ID_API_TOPIC.toString(), nextEvent);
    log.info("message sent to DIGITAL_ID_API_TOPIC for GET_DIGITAL_ID Event.");
  }

  /**
   * This event will be after get student event, if student is found via pen.
   * we will be passing in the student data to update which we got from saga payload.
   * IF there were attached documents to the pen request just mark the student demog code as C
   *
   * @param event                      current event
   * @param saga                       the model object.
   * @param penRequestCompleteSagaData the payload as object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void executeUpdateStudent(final Event event, final Saga saga, final PenRequestCompleteSagaData penRequestCompleteSagaData) throws IOException, InterruptedException, TimeoutException {
    val studentDataFromEventResponse = JsonUtil.getJsonObjectFromString(StudentSagaData.class, event.getEventPayload());
    //update only the fields which are updated through gmp form.
    studentDataFromEventResponse.setLegalFirstName(penRequestCompleteSagaData.getLegalFirstName());
    studentDataFromEventResponse.setLegalLastName(penRequestCompleteSagaData.getLegalLastName());
    studentDataFromEventResponse.setLegalMiddleNames(penRequestCompleteSagaData.getLegalMiddleNames());
    studentDataFromEventResponse.setUsualFirstName(penRequestCompleteSagaData.getUsualFirstName());
    studentDataFromEventResponse.setUsualLastName(penRequestCompleteSagaData.getUsualLastName());
    studentDataFromEventResponse.setUsualMiddleNames(penRequestCompleteSagaData.getUsualMiddleNames());
    studentDataFromEventResponse.setDob(penRequestCompleteSagaData.getDob());
    studentDataFromEventResponse.setEmail(penRequestCompleteSagaData.getEmail());
    studentDataFromEventResponse.setEmailVerified(penRequestCompleteSagaData.getEmailVerified());
    studentDataFromEventResponse.setGenderCode(penRequestCompleteSagaData.getSexCode());
    studentDataFromEventResponse.setSexCode(penRequestCompleteSagaData.getSexCode());
    studentDataFromEventResponse.setUpdateUser(penRequestCompleteSagaData.getUpdateUser());
    studentDataFromEventResponse.setHistoryActivityCode(HISTORY_ACTIVITY_CODE_GMP); // always GMP
    if (penRequestCompleteSagaData.getIsDocumentReviewed() != null && penRequestCompleteSagaData.getIsDocumentReviewed()) {
      studentDataFromEventResponse.setDemogCode(DEMOG_CODE_CONFIRMED);
    }
    penRequestCompleteSagaData.setStudentID(studentDataFromEventResponse.getStudentID()); //update the payload of the original event request with student id.
    saga.setSagaState(UPDATE_STUDENT.toString());
    saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestCompleteSagaData));
    val eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    log.info("message sent to STUDENT_API_TOPIC for UPDATE_STUDENT Event.");
    this.delegateMessagePostingForStudent(saga, studentDataFromEventResponse, UPDATE_STUDENT);
  }

  /**
   * This event will be after get student event, if student is not found via pen.
   *
   * @param event                      current event
   * @param saga                       the model object.
   * @param penRequestCompleteSagaData the payload as object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void executeCreateStudent(final Event event, final Saga saga, final PenRequestCompleteSagaData penRequestCompleteSagaData) throws IOException, InterruptedException, TimeoutException {
    val eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(CREATE_STUDENT.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val studentSagaData = studentSagaDataMapper.toStudentSaga(penRequestCompleteSagaData);
    studentSagaData.setHistoryActivityCode(HISTORY_ACTIVITY_CODE_GMP); // always GMP
    studentSagaData.setStatusCode("A"); // Always active pen is provided to the student upon GMP completion.
    if (penRequestCompleteSagaData.getIsDocumentReviewed() != null && penRequestCompleteSagaData.getIsDocumentReviewed()) {
      studentSagaData.setDemogCode(DEMOG_CODE_CONFIRMED);
    }
    studentSagaData.setUpdateUser(penRequestCompleteSagaData.getUpdateUser());
    studentSagaData.setCreateUser(penRequestCompleteSagaData.getCreateUser());
    log.info("message sent to STUDENT_API_TOPIC for CREATE_STUDENT Event.");
    this.delegateMessagePostingForStudent(saga, studentSagaData, CREATE_STUDENT);

  }

  /**
   * common method for posting message to student api topic.
   *
   * @param saga            the model object.
   * @param studentSagaData the payload which will be passed to student api topic as json string.
   * @param eventType       the type of event whether CREATE_STUDENT or UPDATE_STUDENT
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  private void delegateMessagePostingForStudent(final Saga saga, final StudentSagaData studentSagaData, final EventType eventType) throws InterruptedException, IOException, TimeoutException {
    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(eventType)
      .replyTo(PEN_REQUEST_COMPLETE_SAGA_TOPIC.toString())
      .eventPayload(JsonUtil.getJsonStringFromObject(studentSagaData))
      .build();
    this.postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);

  }

  /**
   * this method will send a message to student api topic to get student details based on PEN.
   *
   * @param event                      current event
   * @param saga                       the model object.
   * @param penRequestCompleteSagaData the payload as object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void executeGetStudent(final Event event, final Saga saga, final PenRequestCompleteSagaData penRequestCompleteSagaData) throws IOException, InterruptedException, TimeoutException {
    if (event.getEventOutcome() == PEN_REQUEST_DOCUMENTS_FOUND) {
      penRequestCompleteSagaData.setIsDocumentReviewed(true);
    }
    val eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(GET_STUDENT.toString()); // set current event as saga state.
    saga.setPayload(JsonUtil.getJsonStringFromObject(penRequestCompleteSagaData)); // save the updated payload to DB.
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(GET_STUDENT)
      .replyTo(PEN_REQUEST_COMPLETE_SAGA_TOPIC.toString())
      .eventPayload(penRequestCompleteSagaData.getPen())
      .build();
    this.postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_API_TOPIC for GET_STUDENT Event.");
  }


  /**
   * this is executed after get digital id, so the event response would contain the entire digital id payload, this method will only update the student Id.
   *
   * @param event                      current event
   * @param saga                       the model object.
   * @param penRequestCompleteSagaData the payload as object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void executeUpdateDigitalId(final Event event, final Saga saga, final PenRequestCompleteSagaData penRequestCompleteSagaData) throws IOException, InterruptedException, TimeoutException {
    val eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(UPDATE_DIGITAL_ID.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val digitalIdSagaData = JsonUtil.getJsonObjectFromString(DigitalIdSagaData.class, event.getEventPayload());
    digitalIdSagaData.setStudentID(penRequestCompleteSagaData.getStudentID());
    digitalIdSagaData.setUpdateUser(penRequestCompleteSagaData.getUpdateUser());
    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(UPDATE_DIGITAL_ID)
      .replyTo(PEN_REQUEST_COMPLETE_SAGA_TOPIC.toString())
      .eventPayload(JsonUtil.getJsonStringFromObject(digitalIdSagaData))
      .build();
    this.postMessageToTopic(DIGITAL_ID_API_TOPIC.toString(), nextEvent);
    log.info("message sent to DIGITAL_ID_API_TOPIC for UPDATE_DIGITAL_ID Event.");

  }

  /**
   * this method will send message to pen request email api to send email to the student that the pen request is now completed.
   *
   * @param event                      current event
   * @param saga                       the model object.
   * @param penRequestCompleteSagaData the payload as object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void executeNotifyStudentPenRequestComplete(final Event event, final Saga saga, final PenRequestCompleteSagaData penRequestCompleteSagaData) throws IOException, InterruptedException, TimeoutException {
    val eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(NOTIFY_STUDENT_PEN_REQUEST_COMPLETE.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(NOTIFY_STUDENT_PEN_REQUEST_COMPLETE)
      .replyTo(PEN_REQUEST_COMPLETE_SAGA_TOPIC.toString())
      .eventPayload(this.buildPenReqEmailSagaData(penRequestCompleteSagaData))
      .build();
    this.postMessageToTopic(PROFILE_REQUEST_EMAIL_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PROFILE_REQUEST_EMAIL_API_TOPIC for NOTIFY_STUDENT_PEN_REQUEST_COMPLETE Event.");

  }

  /**
   * this method builds the email payload as json string.
   *
   * @param penRequestCompleteSagaData the payload as object.
   * @return the string representation of the json object.
   * @throws JsonProcessingException if it is unable to convert the object to json string.
   */
  private String buildPenReqEmailSagaData(final PenRequestCompleteSagaData penRequestCompleteSagaData) throws JsonProcessingException {
    val penReqEmailSagaData = PenReqEmailSagaData.builder()
      .emailAddress(penRequestCompleteSagaData.getEmail())
      .firstName(penRequestCompleteSagaData.getLegalFirstName())
      .demographicsChanged("Y".equalsIgnoreCase(penRequestCompleteSagaData.getDemogChanged()))
      .identityType(penRequestCompleteSagaData.getIdentityType())
      .build();
    return JsonUtil.getJsonStringFromObject(penReqEmailSagaData);
  }

  /**
   * this method is called when the complete saga is in progress and staff members clicks on unlink. system will force stop the existing saga and start unlink saga.
   *
   * @param saga the model object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  @Transactional
  public void publishSagaForceStopped(final Saga saga) throws InterruptedException, TimeoutException, IOException {

    final var forceStoppedEvent = new NotificationEvent();
    forceStoppedEvent.setEventType(UNLINK_PEN_REQUEST);
    forceStoppedEvent.setEventOutcome(SAGA_FORCE_STOPPED);
    forceStoppedEvent.setSagaStatus(FORCE_STOPPED.toString());
    forceStoppedEvent.setPenRequestID(saga.getPenRequestId() != null ? saga.getPenRequestId().toString() : "");
    forceStoppedEvent.setSagaName(this.getSagaName());
    forceStoppedEvent.setSagaId(saga.getSagaId());
    this.postMessageToTopic(this.getTopicToSubscribe(), forceStoppedEvent);
  }

}
