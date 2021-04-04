package ca.bc.gov.educ.api.student.profile.saga.schedulers;

import ca.bc.gov.educ.api.student.profile.saga.BaseSagaApiTest;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaEventRepository;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.INITIATED;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

public class PurgeOldSagaRecordsSchedulerTest extends BaseSagaApiTest {


  @Autowired
  SagaRepository repository;

  @Autowired
  SagaEventRepository sagaEventRepository;

  @Autowired
  PurgeOldSagaRecordsScheduler purgeOldSagaRecordsScheduler;


  @Test
  public void pollSagaTableAndPurgeOldRecords_givenOldRecordsPresent_shouldBeDeleted() {
    final var payload = "{\n" +
        "  \"penRetrievalRequestID\": \"ac334a38-715f-1340-8171-607a59d0000a\",\n" +
        "  \"digitalID\": \"2827808f-dfde-4b9b-835c-10cf0130261c\",\n" +
        "  \"reviewer\": \"SHFOORD\",\n" +
        "  \"penRequestStatusCode\": \"SUBSREV\",\n" +
        "  \"createUser\": \"STAFF_ADMIN\",\n" +
        "  \"updateUser\": \"STAFF_ADMIN\"\n" +
        "}";
    final var saga = this.getSaga(payload, UUID.fromString("ac334a38-715f-1340-8171-607a59d0000a"));
    this.repository.save(saga);
    this.sagaEventRepository.save(this.getSagaEvent(saga,payload));
    this.purgeOldSagaRecordsScheduler.setSagaRecordStaleInDays(0);
    this.purgeOldSagaRecordsScheduler.pollSagaTableAndPurgeOldRecords();
    final var sagas = this.repository.findAll();
    assertThat(sagas).isEmpty();
  }


  private Saga getSaga(final String payload, final UUID penRequestId) {
    return Saga
        .builder()
        .payload(payload)
        .sagaName("PEN_REQUEST_RETURN_SAGA")
        .status(STARTED.toString())
        .sagaState(INITIATED.toString())
        .sagaCompensated(false)
        .createDate(LocalDateTime.now())
        .createUser("STUDENT_PROFILE_SAGA_API")
        .updateUser("STUDENT_PROFILE_SAGA_API")
        .updateDate(LocalDateTime.now())
        .penRequestId(penRequestId)
        .build();
  }
  private SagaEvent getSagaEvent(final Saga saga, final String payload) {
    return SagaEvent
        .builder()
        .sagaEventResponse(payload)
        .saga(saga)
        .sagaEventState("NOTIFY_STUDENT_PEN_REQUEST_RETURN")
        .sagaStepNumber(4)
        .sagaEventOutcome("STUDENT_NOTIFIED")
        .createDate(LocalDateTime.now())
        .createUser("STUDENT_PROFILE_SAGA_API")
        .updateUser("STUDENT_PROFILE_SAGA_API")
        .updateDate(LocalDateTime.now())
        .build();
  }
}
