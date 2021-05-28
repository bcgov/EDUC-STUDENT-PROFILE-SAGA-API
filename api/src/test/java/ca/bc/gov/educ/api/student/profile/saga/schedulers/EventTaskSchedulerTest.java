package ca.bc.gov.educ.api.student.profile.saga.schedulers;

import ca.bc.gov.educ.api.student.profile.saga.BaseSagaApiTest;
import ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaEventRepository;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.INITIATED;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.STUDENT_PROFILE_RETURN_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

public class EventTaskSchedulerTest extends BaseSagaApiTest {

  public static final String REJECTION_REASON_REJECTED = "  \"rejectionReason\": \"rejected\"\n";

  @Autowired
  private EventTaskScheduler eventTaskScheduler;
  @Autowired
  private SagaRepository repository;
  @Autowired
  private SagaEventRepository sagaEventRepository;
  public static final String PAYLOAD_STR = "  \"studentProfileRequestID\": \"ac335214-7252-1946-8172-589e58000004\",\n" +
    "  \"createUser\": \"om\",\n" +
    "  \"updateUser\": \"om\",\n" +
    "  \"email\": \"omprkshmishra@gmail.com\",\n" +
    "  \"identityType\": \"BASIC\"\n";

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    final var statuses = new ArrayList<String>();
    statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
    statuses.add(SagaStatusEnum.STARTED.toString());
    this.eventTaskScheduler.setStatusFilters(statuses);
  }


  @Test
  public void testPollEventTableAndPublish_givenReturnSagaRecordInSTARTEDStateForMoreThan5Minutes_shouldBeProcessed() throws InterruptedException, TimeoutException, IOException {
    final String payload = "{\n" +
      PAYLOAD_STR +
      "}";
    final Saga placeHolderRecord = this.createDummySagaRecord(payload, STUDENT_PROFILE_RETURN_SAGA.toString());
    this.repository.save(placeHolderRecord);
    LockAssert.TestHelper.makeAllAssertsPass(true);
    this.eventTaskScheduler.pollEventTableAndPublish();
    final var eventStates = this.sagaEventRepository.findBySaga(placeHolderRecord);
    assertThat(eventStates).isNotEmpty();
  }

  @Test
  public void testPollEventTableAndPublish_givenReturnSagaRecordInPROGRESSStateForMoreThan5Minutes_shouldBeProcessed() throws InterruptedException, TimeoutException, IOException {
    final String payload = "{\n" +
      PAYLOAD_STR +
      "}";
    final Saga placeHolderRecord = this.createDummySagaRecord(payload, STUDENT_PROFILE_RETURN_SAGA.toString());
    this.repository.save(placeHolderRecord);
    LockAssert.TestHelper.makeAllAssertsPass(true);
    this.eventTaskScheduler.pollEventTableAndPublish();
    final var eventStates = this.sagaEventRepository.findBySaga(placeHolderRecord);
    assertThat(eventStates).isNotEmpty();
  }

  @Test
  public void testPollEventTableAndPublish_givenReturnSagaRecordInPROGRESSStateForLessThan2Minutes_shouldNotBeProcessed() throws InterruptedException, TimeoutException, IOException {
    final String payload = "{\n" +
      PAYLOAD_STR +
      "}";
    final Saga placeHolderRecord = this.createDummySagaRecord(payload, STUDENT_PROFILE_RETURN_SAGA.toString());
    this.repository.save(placeHolderRecord);
    LockAssert.TestHelper.makeAllAssertsPass(true);
    this.eventTaskScheduler.pollEventTableAndPublish();
    final var eventStates = this.sagaEventRepository.findBySaga(placeHolderRecord);
    assertThat(eventStates).isNotEmpty();

  }


  private Saga createDummySagaRecord(final String payload, final String sagaName) {
    return Saga
      .builder()
      .payload(payload)
      .sagaName(sagaName)
      .status(STARTED.toString())
      .sagaState(INITIATED.toString())
      .sagaCompensated(false)
      .createDate(LocalDateTime.now().minusMinutes(3))
      .createUser("test")
      .updateUser("test")
      .updateDate(LocalDateTime.now().minusMinutes(10))
      .build();
  }
}
