package ca.bc.gov.educ.api.student.profile.saga.orchestrator.ump;

import ca.bc.gov.educ.api.student.profile.saga.constants.EventType;
import ca.bc.gov.educ.api.student.profile.saga.mappers.StudentSagaDataMapper;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.DigitalIdSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.StudentSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileCompleteSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileEmailSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileSagaData;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.STUDENT_PROFILE_COMPLETE_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.COMPLETED;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.*;

@Component
@Slf4j
public class StudentProfileCompleteSagaOrchestrator extends BaseProfileReqSagaOrchestrator<StudentProfileCompleteSagaData> {
  private static final StudentSagaDataMapper studentSagaDataMapper = StudentSagaDataMapper.mapper;

  @Override
  public void populateStepsToExecuteMap() {
    stepBuilder()
      .step(INITIATED, INITIATE_SUCCESS, GET_STUDENT, this::executeGetStudent)
      .step(GET_STUDENT, STUDENT_FOUND, UPDATE_STUDENT, this::executeUpdateStudent)
      .step(GET_STUDENT, STUDENT_NOT_FOUND, CREATE_STUDENT, this::executeCreateStudent)
      .step(CREATE_STUDENT, STUDENT_CREATED, GET_DIGITAL_ID, this::executeGetDigitalId)
      .step(UPDATE_STUDENT, STUDENT_UPDATED, GET_DIGITAL_ID, this::executeGetDigitalId)
      .step(GET_DIGITAL_ID, DIGITAL_ID_FOUND, UPDATE_DIGITAL_ID, this::executeUpdateDigitalId)
      .step(UPDATE_DIGITAL_ID, DIGITAL_ID_UPDATED, GET_STUDENT_PROFILE, this::executeGetProfileRequest)
      .step(GET_STUDENT_PROFILE, STUDENT_PROFILE_FOUND, UPDATE_STUDENT_PROFILE, this::executeUpdateProfileRequest)
      .step(UPDATE_STUDENT_PROFILE, STUDENT_PROFILE_UPDATED, NOTIFY_STUDENT_PROFILE_REQUEST_COMPLETE, this::executeNotifyStudentProfileComplete)
      .step(NOTIFY_STUDENT_PROFILE_REQUEST_COMPLETE, STUDENT_NOTIFIED, MARK_SAGA_COMPLETE, this::markSagaComplete);
  }

  @Autowired
  public StudentProfileCompleteSagaOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher) {
    super(sagaService, messagePublisher, StudentProfileCompleteSagaData.class, STUDENT_PROFILE_COMPLETE_SAGA.toString(), STUDENT_PROFILE_COMPLETE_SAGA_TOPIC.toString());
  }

  @Override
  protected void updateProfileRequestPayload(StudentProfileSagaData studentProfileSagaData, StudentProfileCompleteSagaData studentProfileCompleteSagaData) {
    studentProfileSagaData.setStudentRequestStatusCode(COMPLETED.toString());
    studentProfileSagaData.setReviewer(studentProfileCompleteSagaData.getReviewer());
    studentProfileSagaData.setCompleteComment(studentProfileCompleteSagaData.getCompleteComment());
    studentProfileSagaData.setStatusUpdateDate(LocalDateTime.now().toString());
    studentProfileSagaData.setUpdateUser(getSagaName());
    studentProfileSagaData.setUpdateUser(studentProfileCompleteSagaData.getUpdateUser());
  }

  @Override
  protected String updateGetProfileRequestPayload(StudentProfileCompleteSagaData studentProfileCompleteSagaData) {
    return studentProfileCompleteSagaData.getStudentProfileRequestID();
  }

  /**
   * this is called after either create student or update student.
   * update student id in the original payload, if the previous event was create student. if previous event was update student ,
   * system has already updated the student id in original payload, please look at {@link #executeUpdateStudent method}.
   */

  protected void executeGetDigitalId(Event event, Saga saga, StudentProfileCompleteSagaData studentProfileCompleteSagaData) throws InterruptedException, TimeoutException, IOException {

    if (event.getEventType() == CREATE_STUDENT) {
      StudentSagaData studentDataFromEventResponse = JsonUtil.getJsonObjectFromString(StudentSagaData.class, event.getEventPayload());
      studentProfileCompleteSagaData.setStudentID(studentDataFromEventResponse.getStudentID()); //update the payload of the original event request with student id.
      saga.setPayload(JsonUtil.getJsonStringFromObject(studentProfileCompleteSagaData));
    }
    SagaEvent eventState = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(GET_DIGITAL_ID.toString()); // set current event as saga state.
    getSagaService().updateAttachedSagaWithEvents(saga, eventState);
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(GET_DIGITAL_ID)
      .replyTo(getTopicToSubscribe())
      .eventPayload(studentProfileCompleteSagaData.getDigitalID())
      .build();
    postMessageToTopic(DIGITAL_ID_API_TOPIC.toString(), nextEvent);
    log.info("message sent to DIGITAL_ID_API_TOPIC for GET_DIGITAL_ID Event.");
  }

  /**
   * This event will be after get student event, if student is found via pen.
   * we will be passing in the student data to update which we got from saga payload.
   */
  protected void executeUpdateStudent(Event event, Saga saga, StudentProfileCompleteSagaData studentProfileCompleteSagaData) throws IOException, InterruptedException, TimeoutException {
    StudentSagaData studentDataFromEventResponse = JsonUtil.getJsonObjectFromString(StudentSagaData.class, event.getEventPayload());
    //update only the fields which are updated through ump form.
    studentDataFromEventResponse.setLegalFirstName(studentProfileCompleteSagaData.getLegalFirstName());
    studentDataFromEventResponse.setLegalLastName(studentProfileCompleteSagaData.getLegalLastName());
    studentDataFromEventResponse.setLegalMiddleNames(studentProfileCompleteSagaData.getLegalMiddleNames());
    studentDataFromEventResponse.setUsualFirstName(studentProfileCompleteSagaData.getUsualFirstName());
    studentDataFromEventResponse.setUsualLastName(studentProfileCompleteSagaData.getUsualLastName());
    studentDataFromEventResponse.setUsualMiddleNames(studentProfileCompleteSagaData.getUsualMiddleNames());
    studentDataFromEventResponse.setDob(studentProfileCompleteSagaData.getDob());
    studentDataFromEventResponse.setEmail(studentProfileCompleteSagaData.getEmail());
    studentDataFromEventResponse.setEmailVerified(studentProfileCompleteSagaData.getEmailVerified());
    studentDataFromEventResponse.setGenderCode(studentProfileCompleteSagaData.getSexCode());
    studentDataFromEventResponse.setSexCode(studentProfileCompleteSagaData.getSexCode());
    studentDataFromEventResponse.setUpdateUser(studentProfileCompleteSagaData.getUpdateUser());
    studentDataFromEventResponse.setHistoryActivityCode(studentProfileCompleteSagaData.getHistoryActivityCode());
    studentProfileCompleteSagaData.setStudentID(studentDataFromEventResponse.getStudentID()); //update the payload of the original event request with student id.
    saga.setSagaState(UPDATE_STUDENT.toString());
    saga.setPayload(JsonUtil.getJsonStringFromObject(studentProfileCompleteSagaData));
    SagaEvent eventState = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    getSagaService().updateAttachedSagaWithEvents(saga, eventState);
    log.info("message sent to STUDENT_API_TOPIC for UPDATE_STUDENT Event.");
    delegateMessagePostingForStudent(saga, studentDataFromEventResponse, UPDATE_STUDENT);
  }

  /**
   * This event will be after get student event, if student is not found via pen.
   */
  protected void executeCreateStudent(Event event, Saga saga, StudentProfileCompleteSagaData studentProfileCompleteSagaData) throws IOException, InterruptedException, TimeoutException {
    SagaEvent eventState = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(CREATE_STUDENT.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventState);
    StudentSagaData studentSagaData = studentSagaDataMapper.toStudentSaga(studentProfileCompleteSagaData);
    studentSagaData.setUpdateUser(studentProfileCompleteSagaData.getUpdateUser());
    studentSagaData.setCreateUser(studentProfileCompleteSagaData.getCreateUser());
    log.info("message sent to STUDENT_API_TOPIC for CREATE_STUDENT Event.");
    delegateMessagePostingForStudent(saga, studentSagaData, CREATE_STUDENT);
  }

  private void delegateMessagePostingForStudent(Saga saga, StudentSagaData studentSagaData, EventType eventType) throws InterruptedException, IOException, TimeoutException {
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(eventType)
      .replyTo(getTopicToSubscribe())
      .eventPayload(JsonUtil.getJsonStringFromObject(studentSagaData))
      .build();
    postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);

  }


  protected void executeGetStudent(Event event, Saga saga, StudentProfileCompleteSagaData studentProfileCompleteSagaData) throws IOException, InterruptedException, TimeoutException {
    SagaEvent eventState = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setStatus(IN_PROGRESS.toString());
    saga.setSagaState(GET_STUDENT.toString()); // set current event as saga state.
    getSagaService().updateAttachedSagaWithEvents(saga, eventState);
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(GET_STUDENT)
      .replyTo(getTopicToSubscribe())
      .eventPayload(studentProfileCompleteSagaData.getPen())
      .build();
    postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_API_TOPIC for GET_STUDENT Event.");
  }


  /**
   * this is executed after get digital id, so the event response would contain the entire digital id payload, this method will only update the student Id.
   */
  protected void executeUpdateDigitalId(Event event, Saga saga, StudentProfileCompleteSagaData studentProfileCompleteSagaData) throws IOException, InterruptedException, TimeoutException {
    SagaEvent eventState = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(UPDATE_DIGITAL_ID.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventState);
    DigitalIdSagaData digitalIdSagaData = JsonUtil.getJsonObjectFromString(DigitalIdSagaData.class, event.getEventPayload());
    digitalIdSagaData.setStudentID(studentProfileCompleteSagaData.getStudentID());
    digitalIdSagaData.setUpdateUser(studentProfileCompleteSagaData.getUpdateUser());
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(UPDATE_DIGITAL_ID)
      .replyTo(getTopicToSubscribe())
      .eventPayload(JsonUtil.getJsonStringFromObject(digitalIdSagaData))
      .build();
    postMessageToTopic(DIGITAL_ID_API_TOPIC.toString(), nextEvent);
    log.info("message sent to DIGITAL_ID_API_TOPIC for UPDATE_DIGITAL_ID Event.");

  }

  protected void executeNotifyStudentProfileComplete(Event event, Saga saga, StudentProfileCompleteSagaData studentProfileCompleteSagaData) throws IOException, InterruptedException, TimeoutException {
    SagaEvent eventState = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(NOTIFY_STUDENT_PROFILE_REQUEST_COMPLETE.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventState);
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(NOTIFY_STUDENT_PROFILE_REQUEST_COMPLETE)
      .replyTo(getTopicToSubscribe())
      .eventPayload(buildPenReqEmailSagaData(studentProfileCompleteSagaData))
      .build();
    postMessageToTopic(PROFILE_REQUEST_EMAIL_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PROFILE_REQUEST_EMAIL_API_TOPIC for NOTIFY_STUDENT_PROFILE_REQUEST_COMPLETE Event.");

  }

  private String buildPenReqEmailSagaData(StudentProfileCompleteSagaData studentProfileCompleteSagaData) throws JsonProcessingException {
    StudentProfileEmailSagaData penReqEmailSagaData = StudentProfileEmailSagaData.builder()
      .emailAddress(studentProfileCompleteSagaData.getEmail())
      .firstName(studentProfileCompleteSagaData.getLegalFirstName())
      .identityType(studentProfileCompleteSagaData.getIdentityType())
      .build();
    return JsonUtil.getJsonStringFromObject(penReqEmailSagaData);
  }
}
