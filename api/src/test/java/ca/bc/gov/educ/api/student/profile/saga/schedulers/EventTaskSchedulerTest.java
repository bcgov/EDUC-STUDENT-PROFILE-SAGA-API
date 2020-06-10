package ca.bc.gov.educ.api.student.profile.saga.schedulers;

import ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.StudentProfileRejectSagaOrchestrator;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.StudentProfileReturnSagaOrchestrator;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaEventRepository;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.INITIATED;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.STUDENT_PROFILE_REJECT_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.STUDENT_PROFILE_RETURN_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.STARTED;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings("java:S100")
public class EventTaskSchedulerTest {

  public static final String PROF_REQ_ID = "ac335214-7252-1946-8172-589e58000004";
  public static final String REJECTION_REASON_REJECTED = "  \"rejectionReason\": \"rejected\"\n";
  @MockBean
  private StudentProfileReturnSagaOrchestrator returnSagaOrchestrator;
  @MockBean
  private StudentProfileRejectSagaOrchestrator rejectSagaOrchestrator;

  private EventTaskScheduler eventTaskScheduler;
  @MockBean
  private SagaRepository repository;
  @MockBean
  private SagaEventRepository sagaEventRepository;
  public static final String PAYLOAD_STR = "  \"studentProfileRequestID\": \"ac335214-7252-1946-8172-589e58000004\",\n" +
      "  \"createUser\": \"om\",\n" +
      "  \"updateUser\": \"om\",\n" +
      "  \"email\": \"omprkshmishra@gmail.com\",\n" +
      "  \"identityType\": \"BASIC\",\n";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    eventTaskScheduler = new EventTaskScheduler(repository);
    eventTaskScheduler.registerSagaOrchestrators(STUDENT_PROFILE_RETURN_SAGA.toString(), returnSagaOrchestrator);
    eventTaskScheduler.registerSagaOrchestrators(STUDENT_PROFILE_REJECT_SAGA.toString(), rejectSagaOrchestrator);
  }

  @After
  public void tearDown() {
    sagaEventRepository.deleteAll();
    repository.deleteAll();
  }

  @Test
  public void testPollEventTableAndPublish_givenReturnSagaRecordInSTARTEDStateForMoreThan5Minutes_shouldBeProcessed() throws InterruptedException, TimeoutException, IOException {
    String payload = "{\n" +
        PAYLOAD_STR +
        "}";
    Saga dummyRecord = createDummySagaRecord(payload, STUDENT_PROFILE_RETURN_SAGA.toString(), PROF_REQ_ID);
    var dummyList = new ArrayList<Saga>();
    dummyList.add(dummyRecord);
    var statuses = new ArrayList<String>();
    statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
    statuses.add(SagaStatusEnum.STARTED.toString());
    eventTaskScheduler.setStatusFilters(statuses);
    when(repository.findAllByStatusIn(statuses)).thenReturn(dummyList);

    doNothing().when(returnSagaOrchestrator).replaySaga(dummyRecord);
    repository.save(dummyRecord);
    eventTaskScheduler.pollEventTableAndPublish();
    verify(returnSagaOrchestrator, atLeastOnce()).replaySaga(dummyRecord);
  }

  @Test
  public void testPollEventTableAndPublish_givenReturnSagaRecordInPROGRESSStateForMoreThan5Minutes_shouldBeProcessed() throws InterruptedException, TimeoutException, IOException {
    String payload = "{\n" +
        PAYLOAD_STR +
        "}";
    Saga dummyRecord = createDummySagaRecord(payload, STUDENT_PROFILE_RETURN_SAGA.toString(), PROF_REQ_ID);
    dummyRecord.setStatus(IN_PROGRESS.toString());
    var dummyList = new ArrayList<Saga>();
    dummyList.add(dummyRecord);
    var statuses = new ArrayList<String>();
    statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
    statuses.add(SagaStatusEnum.STARTED.toString());
    eventTaskScheduler.setStatusFilters(statuses);
    when(repository.findAllByStatusIn(statuses)).thenReturn(dummyList);

    doNothing().when(returnSagaOrchestrator).replaySaga(dummyRecord);
    repository.save(dummyRecord);
    eventTaskScheduler.pollEventTableAndPublish();
    verify(returnSagaOrchestrator, atLeastOnce()).replaySaga(dummyRecord);
  }

  @Test
  public void testPollEventTableAndPublish_givenReturnSagaRecordInPROGRESSStateForLessThan5Minutes_shouldNotBeProcessed() throws InterruptedException, TimeoutException, IOException {
    String payload = "{\n" +
        PAYLOAD_STR +
        "}";
    Saga dummyRecord = createDummySagaRecord(payload, STUDENT_PROFILE_RETURN_SAGA.toString(), PROF_REQ_ID);
    dummyRecord.setStatus(IN_PROGRESS.toString());
    dummyRecord.setUpdateDate(LocalDateTime.now());
    var dummyList = new ArrayList<Saga>();
    dummyList.add(dummyRecord);
    var statuses = new ArrayList<String>();
    statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
    statuses.add(SagaStatusEnum.STARTED.toString());
    eventTaskScheduler.setStatusFilters(statuses);
    when(repository.findAllByStatusIn(statuses)).thenReturn(dummyList);

    doNothing().when(returnSagaOrchestrator).replaySaga(dummyRecord);
    repository.save(dummyRecord);
    eventTaskScheduler.pollEventTableAndPublish();
    verify(returnSagaOrchestrator, never()).replaySaga(dummyRecord);
  }

  @Test
  public void testPollEventTableAndPublish_givenRejectSagaRecordInSTARTEDStateForMoreThan5Minutes_shouldBeProcessed() throws InterruptedException, TimeoutException, IOException {
    String payload = "{\n" +
        PAYLOAD_STR +
        REJECTION_REASON_REJECTED +
        "}";
    Saga dummyRecord = createDummySagaRecord(payload, STUDENT_PROFILE_REJECT_SAGA.toString(), PROF_REQ_ID);
    var dummyList = new ArrayList<Saga>();
    dummyList.add(dummyRecord);
    var statuses = new ArrayList<String>();
    statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
    statuses.add(SagaStatusEnum.STARTED.toString());
    eventTaskScheduler.setStatusFilters(statuses);
    when(repository.findAllByStatusIn(statuses)).thenReturn(dummyList);

    doNothing().when(rejectSagaOrchestrator).replaySaga(dummyRecord);
    repository.save(dummyRecord);
    eventTaskScheduler.pollEventTableAndPublish();
    verify(rejectSagaOrchestrator, atLeastOnce()).replaySaga(dummyRecord);
  }

  @Test
  public void testPollEventTableAndPublish_givenRejectSagaRecordInPROGRESSStateForMoreThan5Minutes_shouldBeProcessed() throws InterruptedException, TimeoutException, IOException {
    String payload = "{\n" +
        PAYLOAD_STR +
        REJECTION_REASON_REJECTED +
        "}";
    Saga dummyRecord = createDummySagaRecord(payload, STUDENT_PROFILE_REJECT_SAGA.toString(), PROF_REQ_ID);
    dummyRecord.setStatus(IN_PROGRESS.toString());
    var dummyList = new ArrayList<Saga>();
    dummyList.add(dummyRecord);
    var statuses = new ArrayList<String>();
    statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
    statuses.add(SagaStatusEnum.STARTED.toString());
    eventTaskScheduler.setStatusFilters(statuses);
    when(repository.findAllByStatusIn(statuses)).thenReturn(dummyList);

    doNothing().when(rejectSagaOrchestrator).replaySaga(dummyRecord);
    repository.save(dummyRecord);
    eventTaskScheduler.pollEventTableAndPublish();
    verify(rejectSagaOrchestrator, atLeastOnce()).replaySaga(dummyRecord);
  }

  @Test
  public void testPollEventTableAndPublish_givenRejectSagaRecordInPROGRESSStateForLessThan5Minutes_shouldNotBeProcessed() throws InterruptedException, TimeoutException, IOException {
    String payload = "{\n" +
        PAYLOAD_STR +
        REJECTION_REASON_REJECTED +
        "}";
    Saga dummyRecord = createDummySagaRecord(payload, STUDENT_PROFILE_REJECT_SAGA.toString(), PROF_REQ_ID);
    dummyRecord.setStatus(IN_PROGRESS.toString());
    dummyRecord.setUpdateDate(LocalDateTime.now());
    var dummyList = new ArrayList<Saga>();
    dummyList.add(dummyRecord);
    var statuses = new ArrayList<String>();
    statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
    statuses.add(SagaStatusEnum.STARTED.toString());
    eventTaskScheduler.setStatusFilters(statuses);
    when(repository.findAllByStatusIn(statuses)).thenReturn(dummyList);

    doNothing().when(rejectSagaOrchestrator).replaySaga(dummyRecord);
    repository.save(dummyRecord);
    eventTaskScheduler.pollEventTableAndPublish();
    verify(rejectSagaOrchestrator, never()).replaySaga(dummyRecord);
  }

  private Saga createDummySagaRecord(String payload, String sagaName, String profReqId) {
    return Saga
        .builder()
        .payload(payload)
        .sagaName(sagaName)
        .status(STARTED.toString())
        .sagaState(INITIATED.toString())
        .sagaCompensated(false)
        .studentProfileRequestId(profReqId)
        .createDate(LocalDateTime.now())
        .createUser("test")
        .updateUser("test")
        .updateDate(LocalDateTime.now().minusMinutes(10))
        .build();
  }
}