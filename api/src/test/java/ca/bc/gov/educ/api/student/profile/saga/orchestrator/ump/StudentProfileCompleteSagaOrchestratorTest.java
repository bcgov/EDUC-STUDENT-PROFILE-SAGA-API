package ca.bc.gov.educ.api.student.profile.saga.orchestrator.ump;

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

  StudentProfileCompleteSagaData getSagaData(final String json) {
    try {
      return JsonUtil.getJsonObjectFromString(StudentProfileCompleteSagaData.class, json);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
