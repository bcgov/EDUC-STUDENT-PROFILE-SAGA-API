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
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileReturnActionSagaData;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.PEN_REQUEST_RETURN_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.PROFILE_REQUEST_EMAIL_API_TOPIC;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.STUDENT_PROFILE_API_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

public class StudentProfileReturnSagaOrchestratorTest extends BaseSagaApiTest {


  @Autowired
  SagaRepository repository;
  @Autowired
  SagaEventRepository sagaEventRepository;

  @Autowired
  private SagaService sagaService;

  @Autowired
  private MessagePublisher messagePublisher;

  @Autowired
  StudentProfileReturnSagaOrchestrator orchestrator;

  StudentProfileReturnActionSagaData sagaData;

  @Captor
  ArgumentCaptor<byte[]> eventCaptor;

  Saga saga;

  private final String penRequestID = UUID.randomUUID().toString();

  @Before
  public void setUp() throws Exception {
    openMocks(this);
    this.sagaData = this.getSagaData(this.getPayload());
    this.saga = this.sagaService.createPenRequestSagaRecord(this.getSagaData(this.getPayload()), PEN_REQUEST_RETURN_SAGA.toString(), "OMISHRA",
        UUID.fromString(this.penRequestID));
  }



  @Test
  public void testExecuteAddProfileRequestComments_givenEventAndSagaData_shouldPostEventToProfileRequestApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(INITIATED)
        .eventOutcome(EventOutcome.INITIATE_SUCCESS)
        .eventPayload(this.getPayload())
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.executeAddProfileRequestComments(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(STUDENT_PROFILE_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(ADD_STUDENT_PROFILE_COMMENT);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(ADD_STUDENT_PROFILE_COMMENT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
  }

  @Test
  public void testExecuteGetProfileRequest_givenEventAndSagaData_shouldPostEventToProfileRequestApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(INITIATED)
        .eventOutcome(EventOutcome.INITIATE_SUCCESS)
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
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
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
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(GET_STUDENT_PROFILE.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_PROFILE_FOUND.toString());
  }

  @Test
  public void testExecuteNotifyStudent_givenEventAndSagaData_shouldPostEventToProfileEmailApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(UPDATE_STUDENT_PROFILE)
        .eventOutcome(EventOutcome.STUDENT_PROFILE_UPDATED)
        .eventPayload(this.getPayload())
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.executeNotifyStudentProfileRequestReturned(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(PROFILE_REQUEST_EMAIL_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(NOTIFY_STUDENT_PROFILE_REQUEST_RETURN);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(NOTIFY_STUDENT_PROFILE_REQUEST_RETURN.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(UPDATE_STUDENT_PROFILE.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_PROFILE_UPDATED.toString());
  }
  String getPayload() {
    return "{\n" +
        "  \"studentProfileRequestID\": \"ac334a38-715f-1340-8171-607a59d0000a\",\n" +
        "  \"email\": \"someplace@gmail.com\",\n" +
        "  \"identityType\": \"BASIC\",\n" +
        "  \"createUser\": \"OMISHRA\",\n" +
        "  \"updateUser\": \"OMISHRA\"\n" +
        "}";
  }

  StudentProfileReturnActionSagaData getSagaData(final String json) {
    try {
      return JsonUtil.getJsonObjectFromString(StudentProfileReturnActionSagaData.class, json);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
