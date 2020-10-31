package ca.bc.gov.educ.api.student.profile.saga.orchestrator.ump;

import ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome;
import ca.bc.gov.educ.api.student.profile.saga.constants.EventType;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.gmp.PenRequestCompleteSagaOrchestrator;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaEventRepository;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestCompleteSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileCompleteSagaData;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.PEN_REQUEST_COMPLETE_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.STUDENT_PROFILE_COMPLETE_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class StudentProfileCompleteSagaOrchestratorTest {

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
    initMocks(this);
    sagaData = getSagaData(getPayload());
    saga = sagaService.createPenRequestSagaRecord(getSagaData(getPayload()), STUDENT_PROFILE_COMPLETE_SAGA.toString(), "OMISHRA",
        UUID.fromString(profileRequestID));
  }

  @After
  public void tearDown() {
    sagaEventRepository.deleteAll();
    repository.deleteAll();
  }
  @Test
  public void testExecuteGetStudent_givenEventAndSagaData_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    var event = Event.builder()
        .eventType(EventType.INITIATED)
        .eventOutcome(EventOutcome.INITIATE_SUCCESS)
        .eventPayload(getPayload())
        .sagaId(saga.getSagaId())
        .build();
    orchestrator.executeGetStudent(event, saga, sagaData);
    verify(messagePublisher, atLeastOnce()).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(GET_STUDENT);
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(GET_STUDENT.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
  }


  @Test
  public void testExecuteCreateStudent_givenEventAndSagaData_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    var event = Event.builder()
        .eventType(EventType.GET_STUDENT)
        .eventOutcome(EventOutcome.STUDENT_NOT_FOUND)
        .eventPayload(getPayload())
        .sagaId(saga.getSagaId())
        .build();
    orchestrator.executeCreateStudent(event, saga, sagaData);
    verify(messagePublisher, atLeastOnce()).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(CREATE_STUDENT);
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(CREATE_STUDENT.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.GET_STUDENT.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_NOT_FOUND.toString());
  }

  @Test
  public void testExecuteUpdateStudent_givenEventAndSagaData_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    var event = Event.builder()
        .eventType(EventType.GET_STUDENT)
        .eventOutcome(EventOutcome.STUDENT_FOUND)
        .eventPayload(getPayload())
        .sagaId(saga.getSagaId())
        .build();
    orchestrator.executeUpdateStudent(event, saga, sagaData);
    verify(messagePublisher, atLeastOnce()).dispatchMessage(eq(STUDENT_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_STUDENT);
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(UPDATE_STUDENT.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.GET_STUDENT.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_FOUND.toString());
  }

  @Test
  public void testExecuteGetProfileRequest_givenEventAndSagaData_shouldPostEventToProfileRequestApi() throws IOException, InterruptedException, TimeoutException {
    var event = Event.builder()
        .eventType(EventType.UPDATE_DIGITAL_ID)
        .eventOutcome(EventOutcome.DIGITAL_ID_UPDATED)
        .eventPayload(getPayload())
        .sagaId(saga.getSagaId())
        .build();
    orchestrator.executeGetProfileRequest(event, saga, sagaData);
    verify(messagePublisher, atLeastOnce()).dispatchMessage(eq(STUDENT_PROFILE_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(GET_STUDENT_PROFILE);
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(GET_STUDENT_PROFILE.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.UPDATE_DIGITAL_ID.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.DIGITAL_ID_UPDATED.toString());
  }

  @Test
  public void testExecuteUpdateProfileRequest_givenEventAndSagaData_shouldPostEventToProfileRequestApi() throws IOException, InterruptedException, TimeoutException {
    var event = Event.builder()
        .eventType(GET_STUDENT_PROFILE)
        .eventOutcome(EventOutcome.STUDENT_PROFILE_FOUND)
        .eventPayload(getPayload())
        .sagaId(saga.getSagaId())
        .build();
    orchestrator.executeUpdateProfileRequest(event, saga, sagaData);
    verify(messagePublisher, atLeastOnce()).dispatchMessage(eq(STUDENT_PROFILE_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_STUDENT_PROFILE);
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(UPDATE_STUDENT_PROFILE.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.GET_STUDENT_PROFILE.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_PROFILE_FOUND.toString());
  }

  @Test
  public void testExecuteGetDigitalId_givenEventAndSagaData_shouldPostEventToDigitalIDApi() throws IOException, InterruptedException, TimeoutException {
    var event = Event.builder()
        .eventType(EventType.CREATE_STUDENT)
        .eventOutcome(EventOutcome.STUDENT_CREATED)
        .eventPayload(getPayload())
        .sagaId(saga.getSagaId())
        .build();
    orchestrator.executeGetDigitalId(event, saga, sagaData);
    verify(messagePublisher, atLeastOnce()).dispatchMessage(eq(DIGITAL_ID_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(GET_DIGITAL_ID);
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(GET_DIGITAL_ID.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.CREATE_STUDENT.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_CREATED.toString());
  }
  @Test
  public void testExecuteUpdateDigitalId_givenEventAndSagaData_shouldPostEventToDigitalIDApi() throws IOException, InterruptedException, TimeoutException {
    var event = Event.builder()
        .eventType(GET_DIGITAL_ID)
        .eventOutcome(EventOutcome.DIGITAL_ID_FOUND)
        .eventPayload(getPayload())
        .sagaId(saga.getSagaId())
        .build();
    orchestrator.executeUpdateDigitalId(event, saga, sagaData);
    verify(messagePublisher, atLeastOnce()).dispatchMessage(eq(DIGITAL_ID_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(UPDATE_DIGITAL_ID);
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(UPDATE_DIGITAL_ID.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.GET_DIGITAL_ID.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.DIGITAL_ID_FOUND.toString());
  }



  @Test
  public void testExecuteNotifyStudent_givenEventAndSagaData_shouldPostEventToProfileEmailApi() throws IOException, InterruptedException, TimeoutException {
    var event = Event.builder()
        .eventType(UPDATE_STUDENT_PROFILE)
        .eventOutcome(EventOutcome.STUDENT_PROFILE_UPDATED)
        .eventPayload(getPayload())
        .sagaId(saga.getSagaId())
        .build();
    orchestrator.executeNotifyStudentProfileComplete(event, saga, sagaData);
    verify(messagePublisher, atLeastOnce()).dispatchMessage(eq(PROFILE_REQUEST_EMAIL_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(NOTIFY_STUDENT_PROFILE_REQUEST_COMPLETE);
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(NOTIFY_STUDENT_PROFILE_REQUEST_COMPLETE.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.UPDATE_STUDENT_PROFILE.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_PROFILE_UPDATED.toString());
  }


  String getPayload() {
    return "{\n" +
        "  \"digitalID\": \"ac330603-715f-12b6-8171-6079a6270005\",\n" +
        "  \"studentProfileRequestID\":\"" + profileRequestID + "\",\n" +
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

  StudentProfileCompleteSagaData getSagaData(String json) {
    try {
      return JsonUtil.getJsonObjectFromString(StudentProfileCompleteSagaData.class, json);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}