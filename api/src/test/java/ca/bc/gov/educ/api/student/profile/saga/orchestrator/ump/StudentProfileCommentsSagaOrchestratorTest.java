package ca.bc.gov.educ.api.student.profile.saga.orchestrator.ump;

import ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome;
import ca.bc.gov.educ.api.student.profile.saga.constants.EventType;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.gmp.PenRequestCommentsSagaOrchestrator;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaEventRepository;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestCommentsSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileCommentsSagaData;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
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
import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.UPDATE_PEN_REQUEST;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.PEN_REQUEST_COMPLETE_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.STUDENT_PROFILE_COMMENTS_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.PEN_REQUEST_API_TOPIC;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.STUDENT_PROFILE_API_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class StudentProfileCommentsSagaOrchestratorTest {

  @Autowired
  SagaRepository repository;
  @Autowired
  SagaEventRepository sagaEventRepository;

  @Autowired
  private SagaService sagaService;

  @Autowired
  private MessagePublisher messagePublisher;

  @Autowired
  StudentProfileCommentsSagaOrchestrator orchestrator;

  StudentProfileCommentsSagaData sagaData;
  @Captor
  ArgumentCaptor<byte[]> eventCaptor;

  Saga saga;
  private final String profileRequestID = UUID.randomUUID().toString();

  @Before
  public void setUp() throws Exception {
    openMocks(this);
    sagaData = getSagaData(getPayload());
    saga = sagaService.createProfileRequestSagaRecord(getSagaData(getPayload()), STUDENT_PROFILE_COMMENTS_SAGA.toString(), "OMISHRA",
        UUID.fromString(profileRequestID));
  }

  @After
  public void tearDown() {
    sagaEventRepository.deleteAll();
    repository.deleteAll();
  }

  @Test
  public void testExecuteAddProfileRequestComments_givenEventAndSagaData_shouldPostEventToPenRequestApi() throws IOException, InterruptedException, TimeoutException {
    var event = Event.builder()
        .eventType(INITIATED)
        .eventOutcome(EventOutcome.INITIATE_SUCCESS)
        .eventPayload(getPayload())
        .sagaId(saga.getSagaId())
        .build();
    orchestrator.executeAddStudentProfileRequestComments(event, saga, sagaData);
    verify(messagePublisher, atLeastOnce()).dispatchMessage(eq(STUDENT_PROFILE_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(ADD_STUDENT_PROFILE_COMMENT);
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId()).orElseThrow();
    assertThat(sagaFromDB.getCreateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getUpdateUser()).isEqualTo("OMISHRA");
    assertThat(sagaFromDB.getSagaState()).isEqualTo(ADD_STUDENT_PROFILE_COMMENT.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
  }

  @Test
  public void testExecuteGetProfileRequest_givenEventAndSagaData_shouldPostEventToPenRequestApi() throws IOException, InterruptedException, TimeoutException {
    var event = Event.builder()
        .eventType(ADD_STUDENT_PROFILE_COMMENT)
        .eventOutcome(EventOutcome.STUDENT_PROFILE_COMMENT_ADDED)
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
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.ADD_STUDENT_PROFILE_COMMENT.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.STUDENT_PROFILE_COMMENT_ADDED.toString());
  }

  @Test
  public void testExecuteUpdateProfileRequest_givenEventAndSagaData_shouldPostEventToPenRequestApi() throws IOException, InterruptedException, TimeoutException {
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

  String getPayload() {
    return  "{\n" +
        "  \"commentContent\": \"Hi\",\n" +
        "  \"studentProfileRequestID\":\"" + profileRequestID + "\",\n" +
        "  \"commentTimestamp\": \"2020-04-18T19:57:00\",\n" +
        "  \"studentProfileRequestStatusCode\": \"SUBSREV\",\n" +
        "  \"createUser\": \"OMISHRA\",\n" +
        "  \"updateUser\": \"OMISHRA\"\n" +
        "}";
  }

  StudentProfileCommentsSagaData getSagaData(String json) {
    try {
      return JsonUtil.getJsonObjectFromString(StudentProfileCommentsSagaData.class, json);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}