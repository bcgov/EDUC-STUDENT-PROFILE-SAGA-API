package ca.bc.gov.educ.api.student.profile.saga.orchestrator.gmp;

import ca.bc.gov.educ.api.student.profile.saga.BaseSagaApiTest;
import ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome;
import ca.bc.gov.educ.api.student.profile.saga.constants.EventType;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaEventRepository;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestUnlinkSagaData;
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
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.PEN_REQUEST_UNLINK_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.DIGITAL_ID_API_TOPIC;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.PEN_REQUEST_API_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

public class PenRequestUnlinkSagaOrchestratorTest extends BaseSagaApiTest {


  @Autowired
  SagaRepository repository;
  @Autowired
  SagaEventRepository sagaEventRepository;

  @Autowired
  private SagaService sagaService;

  @Autowired
  private MessagePublisher messagePublisher;

  @Autowired
  PenRequestUnlinkSagaOrchestrator orchestrator;

  PenRequestUnlinkSagaData sagaData;
  @Captor
  ArgumentCaptor<byte[]> eventCaptor;

  Saga saga;

  private final String penRequestID = UUID.randomUUID().toString();

  @Before
  public void setUp() throws Exception {
    openMocks(this);
    this.sagaData = this.getSagaData(this.getUnlinkPayload());
    this.saga = this.sagaService.createPenRequestSagaRecord(this.getSagaData(this.getUnlinkPayload()), PEN_REQUEST_UNLINK_SAGA.toString(), "OMISHRA",
        UUID.fromString(this.penRequestID));
  }



  @Test
  public void testExecuteGetDigitalId_givenEventAndSagaData_shouldPostEventToDigitalIDApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(INITIATED)
        .eventOutcome(EventOutcome.INITIATE_SUCCESS)
        .eventPayload(this.getUnlinkPayload())
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
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
  }
  @Test
  public void testExecuteUpdateDigitalId_givenEventAndSagaData_shouldPostEventToDigitalIDApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(GET_DIGITAL_ID)
        .eventOutcome(EventOutcome.DIGITAL_ID_FOUND)
        .eventPayload(this.getUnlinkPayload())
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
  public void testExecuteGetPenRequest_givenEventAndSagaData_shouldPostEventToPenRequestApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(EventType.UPDATE_DIGITAL_ID)
        .eventOutcome(EventOutcome.DIGITAL_ID_UPDATED)
        .eventPayload(this.getUnlinkPayload())
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
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.UPDATE_DIGITAL_ID.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.DIGITAL_ID_UPDATED.toString());
  }

  @Test
  public void testExecuteUpdatePenRequest_givenEventAndSagaData_shouldPostEventToPenRequestApi() throws IOException, InterruptedException, TimeoutException {
    final var event = Event.builder()
        .eventType(GET_PEN_REQUEST)
        .eventOutcome(EventOutcome.PEN_REQUEST_FOUND)
        .eventPayload(this.getUnlinkPayload())
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

  String getUnlinkPayload() {
    return "{\n" +
        "  \"penRetrievalRequestID\": \"ac334a38-715f-1340-8171-607a59d0000a\",\n" +
        "  \"email\": \"someplace@gmail.com\",\n" +
        "  \"penRequestStatusCode\": \"RETURNED\",\n" +
        "  \"identityType\": \"BASIC\",\n" +
        "  \"commentContent\": \"Need More Info\",\n" +
        "  \"createUser\": \"OMISHRA\",\n" +
        "  \"updateUser\": \"OMISHRA\"\n" +
        "}";
  }

  PenRequestUnlinkSagaData getSagaData(final String json) {
    try {
      return JsonUtil.getJsonObjectFromString(PenRequestUnlinkSagaData.class, json);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
