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
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestRejectSagaData;
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
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.PEN_REQUEST_REJECT_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.PEN_REQUEST_API_TOPIC;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.PROFILE_REQUEST_EMAIL_API_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

public class PenRequestRejectSagaOrchestratorTest extends BaseSagaApiTest {

  @Autowired
  SagaRepository repository;
  @Autowired
  SagaEventRepository sagaEventRepository;

  @Autowired
  private SagaService sagaService;

  @Autowired
  private MessagePublisher messagePublisher;

  @Autowired
  PenRequestRejectSagaOrchestrator orchestrator;

  PenRequestRejectSagaData sagaData;
  @Captor
  ArgumentCaptor<byte[]> eventCaptor;

  Saga saga;

  private final String penRequestID = UUID.randomUUID().toString();


  @Before
  public void setUp() throws Exception {
    openMocks(this);
    this.sagaData = this.getSagaData(this.getRejectPenRequestPayload());
    this.saga = this.sagaService.createPenRequestSagaRecord(this.getSagaData(this.getRejectPenRequestPayload()), PEN_REQUEST_REJECT_SAGA.toString(), "OMISHRA",
        UUID.fromString(this.penRequestID));
  }


  @Test
  public void testExecuteGetPenRequest_givenEventAndSagaData_shouldPostEventToPenRequestApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(INITIATED)
        .eventOutcome(EventOutcome.INITIATE_SUCCESS)
        .eventPayload(this.getRejectPenRequestPayload())
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.executeGetPenRequest(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(PEN_REQUEST_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(GET_PEN_REQUEST);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(GET_PEN_REQUEST.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
  }
  @Test
  public void testExecuteUpdatePenRequest_givenEventAndSagaData_shouldPostEventToPenRequestApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(GET_PEN_REQUEST)
        .eventOutcome(EventOutcome.PEN_REQUEST_FOUND)
        .eventPayload(this.getRejectPenRequestPayload())
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
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(GET_PEN_REQUEST.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_REQUEST_FOUND.toString());
  }

  @Test
  public void testExecuteNotifyStudent_givenEventAndSagaData_shouldPostEventToProfileEmailApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(UPDATE_PEN_REQUEST)
        .eventOutcome(EventOutcome.PEN_REQUEST_UPDATED)
        .eventPayload(this.getRejectPenRequestPayload())
        .sagaId(this.saga.getSagaId())
        .build();
    this.orchestrator.executeNotifyStudentPenRequestReject(event, this.saga, this.sagaData);
    verify(this.messagePublisher, atLeastOnce()).dispatchMessage(eq(PROFILE_REQUEST_EMAIL_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(NOTIFY_STUDENT_PEN_REQUEST_REJECT);
    final var sagaFromDB = this.sagaService.findSagaById(this.saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(NOTIFY_STUDENT_PEN_REQUEST_REJECT.toString());
    final var sagaStates = this.sagaService.findAllSagaStates(this.saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(UPDATE_PEN_REQUEST.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.PEN_REQUEST_UPDATED.toString());
  }



  String getRejectPenRequestPayload() {
    return "{\n" +
        "  \"penRetrievalRequestID\": \"ac334a38-715f-1340-8171-607a59d0000a\",\n" +
        "  \"email\": \"someplace@gmail.com\",\n" +
        "  \"penRequestStatusCode\": \"REJECTED\",\n" +
        "  \"identityType\": \"BASIC\",\n" +
        "  \"rejectionReason\": \"Can't find you\",\n" +
        "  \"createUser\": \"OMISHRA\",\n" +
        "  \"updateUser\": \"OMISHRA\"\n" +
        "}";
  }

  PenRequestRejectSagaData getSagaData(final String json) {
    try {
      return JsonUtil.getJsonObjectFromString(PenRequestRejectSagaData.class, json);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
