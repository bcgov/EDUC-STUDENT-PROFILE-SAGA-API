package ca.bc.gov.educ.api.student.profile.saga.repository;

import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.SagaEvent;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaEventRepository extends CrudRepository<SagaEvent, UUID> {
  List<SagaEvent> findBySaga(Saga saga);

  Optional<SagaEvent> findBySagaAndSagaEventOutcomeAndSagaEventStateAndSagaStepNumber(Saga saga, String eventOutcome, String eventState, int stepNumber);

  Optional<SagaEvent> findBySagaAndSagaEventOutcomeAndSagaEventState(Saga saga, String eventOutcome, String eventState);

  @Transactional
  @Modifying
  @Query(value = "delete from STUDENT_PROFILE_SAGA_EVENT_STATES e where exists(select 1 from STUDENT_PROFILE_SAGA s where s.SAGA_ID = e.SAGA_ID and s.CREATE_DATE <= :createDate)", nativeQuery = true)
  void deleteBySagaCreateDateBefore(LocalDateTime createDate);
}
