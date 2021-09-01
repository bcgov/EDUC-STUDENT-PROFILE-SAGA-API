package ca.bc.gov.educ.api.student.profile.saga.orchestrator.ump;

import ca.bc.gov.educ.api.student.profile.saga.BaseSagaApiTest;
import ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome;
import ca.bc.gov.educ.api.student.profile.saga.constants.EventType;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaEventRepository;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.StudentSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileCompleteSagaData;
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
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.STUDENT_PROFILE_COMPLETE_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

public class StudentProfileCompleteSagaOrchestratorTest extends BaseSagaApiTest {

  @Autowired
  SagaRepository repository;
  @Autowired
  SagaEventRepository sagaEventRepository;

  @Autowired
  private SagaService sagaService;

  @Autowired
  private MessagePublisher messagePublisher;

  @Autowired
  StudentProfileCompleteSagaOrchestrator orchestrator;

  StudentProfileCompleteSagaData sagaData;
  @Captor
  ArgumentCaptor<byte[]> eventCaptor;

  Saga saga;
  private final String profileRequestID = UUID.randomUUID().toString();

  @Before
  public void setUp() throws JsonProcessingException {
    openMocks(this);
    this.sagaData = this.getSagaData(this.getPayload());
    this.saga = this.sagaService.createPenRequestSagaRecord(this.getSagaData(this.getPayload()), STUDENT_PROFILE_COMPLETE_SAGA.toString(), "OMISHRA",
        UUID.fromString(this.profileRequestID));
  }

  @Test
  public void testExecuteGetStudent_givenEventAndSagaData_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(EventType.INITIATED)
        .eventOutcome(EventOutcome.INITIATE_SUCCESS)
        .eventPayload(this.getPayload())
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
      .eventPayload(this.getPayload())
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
        .eventPayload(this.getPayload())
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
  public void testExecuteGetProfileRequest_givenEventAndSagaData_shouldPostEventToProfileRequestApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(EventType.UPDATE_DIGITAL_ID)
        .eventOutcome(EventOutcome.DIGITAL_ID_UPDATED)
        .eventPayload(this.getPayload())
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.executeGetProfileRequest(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(STUDENT_PROFILE_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(GET_STUDENT_PROFILE);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(GET_STUDENT_PROFILE.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.UPDATE_DIGITAL_ID.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.DIGITAL_ID_UPDATED.toString());
  }

  @Test
  public void testExecuteUpdateProfileRequest_givenEventAndSagaData_shouldPostEventToProfileRequestApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(GET_STUDENT_PROFILE)
        .eventOutcome(EventOutcome.STUDENT_PROFILE_FOUND)
        .eventPayload(this.getPayload())
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.executeUpdateProfileRequest(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(STUDENT_PROFILE_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_STUDENT_PROFILE);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(UPDATE_STUDENT_PROFILE.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.GET_STUDENT_PROFILE.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_PROFILE_FOUND.toString());
  }

  @Test
  public void testExecuteGetDigitalId_givenEventAndSagaData_shouldPostEventToDigitalIDApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(EventType.CREATE_STUDENT)
        .eventOutcome(EventOutcome.STUDENT_CREATED)
        .eventPayload(this.getPayload())
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
        .eventPayload(this.getPayload())
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
  public void testExecuteNotifyStudent_givenEventAndSagaData_shouldPostEventToProfileEmailApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(UPDATE_STUDENT_PROFILE)
        .eventOutcome(EventOutcome.STUDENT_PROFILE_UPDATED)
        .eventPayload(this.getPayload())
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.executeNotifyStudentProfileComplete(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(PROFILE_REQUEST_EMAIL_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(NOTIFY_STUDENT_PROFILE_REQUEST_COMPLETE);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(NOTIFY_STUDENT_PROFILE_REQUEST_COMPLETE.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.UPDATE_STUDENT_PROFILE.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_PROFILE_UPDATED.toString());
  }

  String sagaDocResponse = "[{\"documentID\":\"0a613832-7b41-1c07-817b-8426218f00de\",\"documentTypeCode\":\"MARRIAGE\",\"fileName\":\"photo-1550330562-b055aa030d73.jfif\",\"fileExtension\":\"image/jpeg\",\"fileSize\":31698,\"createDate\":\"2021-08-26T13:28:16\"},{\"documentID\":\"0a613832-7b41-1c07-817b-8420476500d6\",\"documentTypeCode\":\"NAMECHANGE\",\"fileName\":\"file-example_PDF_1MB.pdf\",\"fileExtension\":\"application/pdf\",\"fileSize\":1042157,\"createDate\":\"2021-08-26T13:21:53\"},{\"documentID\":\"0a613e35-7b41-1c51-817b-8422154100cb\",\"documentTypeCode\":\"FORBIRTH\",\"fileName\":\"dummy__FirstName_LastName.pdf\",\"fileExtension\":\"application/pdf\",\"fileSize\":13264,\"createDate\":\"2021-08-26T13:23:51\"},{\"documentID\":\"0a613832-7b41-1c07-817b-8425e6b600dd\",\"documentTypeCode\":\"FORPASSPRT\",\"fileName\":\"british-columbia-drivers-licen.jpg\",\"fileExtension\":\"image/jpeg\",\"fileSize\":163080,\"createDate\":\"2021-08-26T13:28:01\"},{\"documentID\":\"0a613832-7b41-1c07-817b-841d701600d5\",\"documentTypeCode\":\"CADL\",\"fileName\":\"dsc02496.jpg\",\"fileExtension\":\"image/jpeg\",\"fileSize\":9552513,\"createDate\":\"2021-08-26T13:18:46\"},{\"documentID\":\"0a613e35-7b41-1c51-817b-84209a4b00c9\",\"documentTypeCode\":\"CAPASSPORT\",\"fileName\":\"photo-1579022287310-910c9a3604.jfif\",\"fileExtension\":\"image/jpeg\",\"fileSize\":106509,\"createDate\":\"2021-08-26T13:22:14\"},{\"documentID\":\"0a613e35-7b41-1c51-817b-841cc9bb00c8\",\"documentTypeCode\":\"CABIRTH\",\"fileName\":\"sample-jpg-file-for-testing.jpg\",\"fileExtension\":\"image/jpeg\",\"fileSize\":9774095,\"createDate\":\"2021-08-26T13:18:04\"},{\"documentID\":\"0a613e35-7b41-1c51-817b-842852db00cc\",\"documentTypeCode\":\"CABIRTH\",\"fileName\":\"dsc02547.jpg\",\"fileExtension\":\"image/jpeg\",\"fileSize\":8507890,\"createDate\":\"2021-08-26T13:30:40\"},{\"documentID\":\"0a613e35-7b41-1c51-817b-84219a8700ca\",\"documentTypeCode\":\"CAPASSPORT\",\"fileName\":\"file_example_PNG_2100kB.png\",\"fileExtension\":\"image/png\",\"fileSize\":2060826,\"createDate\":\"2021-08-26T13:23:18\"}]";
  String studentJson = "{\"studentID\":null,\"pen\":\"123456789\",\"legalFirstName\":\"om\",\"legalMiddleNames\":\"mishra\",\"legalLastName\":\"mishra\",\"dob\":\"2000-01-01\",\"sexCode\":\"M\",\"genderCode\":\"M\",\"usualFirstName\":null,\"usualMiddleNames\":null,\"usualLastName\":null,\"email\":\"om@gmail.com\",\"deceasedDate\":null,\"createUser\":\"OMISHRA\",\"updateUser\":\"OMISHRA\",\"localID\":null,\"postalCode\":null,\"gradeCode\":null,\"mincode\":null,\"emailVerified\":null,\"historyActivityCode\":\"UMP\",\"gradeYear\":null,\"demogCode\":\"A\",\"statusCode\":\"A\",\"memo\":null,\"trueStudentID\":null,\"documentTypeCode\":\"ABC\",\"dateOfConfirmation\":\"2021-08-30T09:16:49.2208031\"}\n";

  @Test
  public void testCreateStudent_givenEventAndSagaData_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
      .eventType(GET_STUDENT)
      .eventOutcome(EventOutcome.STUDENT_NOT_FOUND)
      .eventPayload("123456789")
      .sagaId(this.saga.getSagaId())
      .build();
    val sagaEvent = SagaEvent.builder().sagaEventState(GET_PROFILE_REQUEST_DOCUMENT_METADATA.toString())
      .sagaEventOutcome(EventOutcome.PROFILE_REQUEST_DOCUMENTS_FOUND.toString())
      .sagaEventResponse(this.sagaDocResponse)
      .sagaStepNumber(2)
      .saga(this.saga)
      .createDate(LocalDateTime.now())
      .updateDate(LocalDateTime.now())
      .createUser("test")
      .updateUser("test").build();
    this.sagaEventRepository.save(sagaEvent);
    this.orchestrator.executeCreateStudent(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(CREATE_STUDENT);
    val student = JsonUtil.getJsonObjectFromString(StudentSagaData.class, newEvent.getEventPayload());
    System.out.println(newEvent.getEventPayload());
    assertThat(student.getDemogCode()).isEqualTo("C");
    assertThat(student.getDocumentTypeCode()).isEqualTo("CABIRTH");
    assertThat(student.getDateOfConfirmation()).isNotBlank();
    assertThat(LocalDate.parse(student.getDateOfConfirmation().substring(0, 10))).isEqualTo(LocalDate.now());
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(CREATE_STUDENT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(2);
  }

  StudentProfileCompleteSagaData getSagaData(final String json) {
    try {
      return JsonUtil.getJsonObjectFromString(StudentProfileCompleteSagaData.class, json);
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
    val sagaEvent = SagaEvent.builder().sagaEventState(GET_PROFILE_REQUEST_DOCUMENT_METADATA.toString())
      .sagaEventOutcome(EventOutcome.PROFILE_REQUEST_DOCUMENTS_FOUND.toString())
      .sagaEventResponse(this.sagaDocResponse)
      .sagaStepNumber(2)
      .saga(this.saga)
      .createDate(LocalDateTime.now())
      .updateDate(LocalDateTime.now())
      .createUser("test")
      .updateUser("test").build();
    this.sagaEventRepository.save(sagaEvent);
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
    assertThat(sagaStates.size()).isEqualTo(2);
  }

  String getPayload() {
    return "{\n" +
      "  \"digitalID\": \"ac330603-715f-12b6-8171-6079a6270005\",\n" +
      "  \"studentProfileRequestID\":\"" + this.profileRequestID + "\",\n" +
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
      "  \"identityType\": \"BASIC\",\n" +
      "  \"createUser\": \"OMISHRA\",\n" +
      "  \"updateUser\": \"OMISHRA\"\n" +
      "}";
  }
}
