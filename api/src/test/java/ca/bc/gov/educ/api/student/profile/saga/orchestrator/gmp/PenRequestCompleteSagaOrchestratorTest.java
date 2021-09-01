package ca.bc.gov.educ.api.student.profile.saga.orchestrator.gmp;

import ca.bc.gov.educ.api.student.profile.saga.BaseSagaApiTest;
import ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome;
import ca.bc.gov.educ.api.student.profile.saga.constants.EventType;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaEventRepository;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.StudentSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestCompleteSagaData;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.PEN_REQUEST_COMPLETE_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

public class PenRequestCompleteSagaOrchestratorTest extends BaseSagaApiTest {

  @Autowired
  SagaRepository repository;
  @Autowired
  SagaEventRepository sagaEventRepository;

  @Autowired
  private SagaService sagaService;

  @Autowired
  private MessagePublisher messagePublisher;

  @Autowired
  PenRequestCompleteSagaOrchestrator orchestrator;

  PenRequestCompleteSagaData sagaData;
  @Captor
  ArgumentCaptor<byte[]> eventCaptor;

  Saga saga;
  private final String penRequestID = UUID.randomUUID().toString();

  @Before
  public void setUp() throws JsonProcessingException {
    openMocks(this);
    this.sagaData = this.getSagaData(this.getCompletePenRequestPayload());
    this.saga = this.sagaService.createPenRequestSagaRecord(this.getSagaData(this.getCompletePenRequestPayload()), PEN_REQUEST_COMPLETE_SAGA.toString(), "OMISHRA",
        UUID.fromString(this.penRequestID));
  }


  @Test
  public void testExecuteGetPenRequest_givenEventAndSagaData_shouldPostEventToPenRequestApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(EventType.UPDATE_DIGITAL_ID)
        .eventOutcome(EventOutcome.DIGITAL_ID_UPDATED)
        .eventPayload(this.getCompletePenRequestPayload())
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.executeGetPenRequest(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(PEN_REQUEST_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(GET_PEN_REQUEST);
    assertThat(newEvent.getEventPayload()).isEqualTo(this.penRequestID);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(GET_PEN_REQUEST.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.UPDATE_DIGITAL_ID.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.DIGITAL_ID_UPDATED.toString());
  }

  @Test
  public void testExecuteUpdatePenRequest_givenEventAndSagaData_shouldPostEventToPenRequestApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(EventType.GET_PEN_REQUEST)
        .eventOutcome(EventOutcome.PEN_REQUEST_FOUND)
        .eventPayload(this.getCompletePenRequestPayload())
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.executeUpdatePenRequest(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(PEN_REQUEST_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_PEN_REQUEST);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(UPDATE_PEN_REQUEST.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.GET_PEN_REQUEST.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_REQUEST_FOUND.toString());
  }

  @Test
  public void testExecuteGetDigitalId_givenEventAndSagaData_shouldPostEventToDigitalIDApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(EventType.CREATE_STUDENT)
        .eventOutcome(EventOutcome.STUDENT_CREATED)
        .eventPayload(this.getCompletePenRequestPayload())
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.executeGetDigitalId(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(DIGITAL_ID_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(GET_DIGITAL_ID);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(GET_DIGITAL_ID.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.CREATE_STUDENT.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_CREATED.toString());
  }
  @Test
  public void testExecuteUpdateDigitalId_givenEventAndSagaData_shouldPostEventToDigitalIDApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(GET_DIGITAL_ID)
        .eventOutcome(EventOutcome.DIGITAL_ID_FOUND)
        .eventPayload(this.getCompletePenRequestPayload())
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.executeUpdateDigitalId(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(DIGITAL_ID_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_DIGITAL_ID);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(UPDATE_DIGITAL_ID.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.GET_DIGITAL_ID.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.DIGITAL_ID_FOUND.toString());
  }

  @Test
  public void testExecuteGetStudent_givenEventAndSagaData_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(EventType.INITIATED)
        .eventOutcome(EventOutcome.INITIATE_SUCCESS)
        .eventPayload(this.getCompletePenRequestPayload())
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.executeGetStudent(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(GET_STUDENT);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(GET_STUDENT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
  }


  @Test
  public void testExecuteCreateStudent_givenEventAndSagaData_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
      .eventType(EventType.GET_STUDENT)
      .eventOutcome(EventOutcome.STUDENT_NOT_FOUND)
      .eventPayload(this.getCompletePenRequestPayload())
      .sagaId(this.saga.getSagaId())
      .build();
    this.orchestrator.executeCreateStudent(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(CREATE_STUDENT);
    val studentCreate = JsonUtil.getJsonObjectFromString(StudentSagaData.class, newEvent.getEventPayload());
    assertThat(studentCreate.getStatusCode()).isEqualTo("A");
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(CREATE_STUDENT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.GET_STUDENT.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_NOT_FOUND.toString());
  }

  @Test
  public void testExecuteUpdateStudent_givenEventAndSagaData_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(EventType.GET_STUDENT)
        .eventOutcome(EventOutcome.STUDENT_FOUND)
        .eventPayload(this.getCompletePenRequestPayload())
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.executeUpdateStudent(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_STUDENT);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(UPDATE_STUDENT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.GET_STUDENT.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_FOUND.toString());
  }

  @Test
  public void testExecuteNotifyStudent_givenEventAndSagaData_shouldPostEventToProfileEmailApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(UPDATE_PEN_REQUEST)
        .eventOutcome(EventOutcome.PEN_REQUEST_UPDATED)
        .eventPayload(this.getCompletePenRequestPayload())
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.executeNotifyStudentPenRequestComplete(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(PROFILE_REQUEST_EMAIL_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(NOTIFY_STUDENT_PEN_REQUEST_COMPLETE);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(NOTIFY_STUDENT_PEN_REQUEST_COMPLETE.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.UPDATE_PEN_REQUEST.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_REQUEST_UPDATED.toString());
  }

  String studentJson = "{\"studentID\":null,\"pen\":\"123456789\",\"legalFirstName\":\"om\",\"legalMiddleNames\":\"mishra\",\"legalLastName\":\"mishra\",\"dob\":\"2000-01-01\",\"sexCode\":\"M\",\"genderCode\":\"M\",\"usualFirstName\":null,\"usualMiddleNames\":null,\"usualLastName\":null,\"email\":\"om@gmail.com\",\"deceasedDate\":null,\"createUser\":\"OMISHRA\",\"updateUser\":\"OMISHRA\",\"localID\":null,\"postalCode\":null,\"gradeCode\":null,\"mincode\":null,\"emailVerified\":null,\"historyActivityCode\":\"UMP\",\"gradeYear\":null,\"demogCode\":\"A\",\"statusCode\":\"A\",\"memo\":null,\"trueStudentID\":null,\"documentTypeCode\":\"ABC\",\"dateOfConfirmation\":\"2021-08-30T09:16:49.2208031\"}\n";

  @Test
  public void testCreateStudent_givenEventAndSagaData_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
      .eventType(GET_STUDENT)
      .eventOutcome(EventOutcome.STUDENT_NOT_FOUND)
      .eventPayload("123456789")
      .sagaId(this.saga.getSagaId())
      .build();
    this.sagaData.setDocumentTypeCode("CABIRTH");
    this.orchestrator.executeCreateStudent(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(CREATE_STUDENT);
    val student = JsonUtil.getJsonObjectFromString(StudentSagaData.class, newEvent.getEventPayload());
    assertThat(student.getDemogCode()).isEqualTo("C");
    assertThat(student.getDocumentTypeCode()).isEqualTo("CABIRTH");
    assertThat(student.getDateOfConfirmation()).isNotBlank();
    assertThat(LocalDate.parse(student.getDateOfConfirmation().substring(0, 10))).isEqualTo(LocalDate.now());
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(CREATE_STUDENT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
  }

  PenRequestCompleteSagaData getSagaData(final String json) {
    try {
      return JsonUtil.getJsonObjectFromString(PenRequestCompleteSagaData.class, json);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testUpdateStudent_givenEventAndSagaData_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
      .eventType(GET_STUDENT)
      .eventOutcome(EventOutcome.STUDENT_FOUND)
      .eventPayload(this.studentJson)
      .sagaId(this.saga.getSagaId())
      .build();
    this.sagaData.setDocumentTypeCode("CABIRTH");
    this.orchestrator.executeUpdateStudent(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_STUDENT);
    val student = JsonUtil.getJsonObjectFromString(StudentSagaData.class, newEvent.getEventPayload());
    assertThat(student.getDemogCode()).isEqualTo("C");
    assertThat(student.getDocumentTypeCode()).isEqualTo("CABIRTH");
    assertThat(student.getDateOfConfirmation()).isNotBlank();
    assertThat(LocalDate.parse(student.getDateOfConfirmation().substring(0, 10))).isEqualTo(LocalDate.now());
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(UPDATE_STUDENT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
  }

  String getCompletePenRequestPayload() {
    return "{\n" +
      "  \"digitalID\": \"ac330603-715f-12b6-8171-6079a6270005\",\n" +
      "  \"penRequestID\":\"" + this.penRequestID + "\",\n" +
      "  \"pen\": \"123456789\",\n" +
      "  \"legalFirstName\": \"om\",\n" +
      "  \"legalMiddleNames\": \"mishra\",\n" +
      "  \"legalLastName\": \"mishra\",\n" +
      "  \"dob\": \"2000-01-01\",\n" +
      "  \"sexCode\": \"M\",\n" +
      "  \"genderCode\": \"M\",\n" +
      "  \"email\": \"om@gmail.com\",\n" +
      "  \"createUser\": \"om\",\n" +
      "  \"updateUser\": \"om\",\n" +
      "  \"bcscAutoMatchOutcome\": \"ONEMATCH\",\n" +
      "  \"penRequestStatusCode\": \"MANUAL\",\n" +
      "  \"statusUpdateDate\": \"2020-04-17T22:29:00\",\n" +
      "  \"identityType\": \"BASIC\",\n" +
      "  \"createUser\": \"OMISHRA\",\n" +
      "  \"updateUser\": \"OMISHRA\"\n" +
      "}";
  }
}
