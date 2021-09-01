package ca.bc.gov.educ.api.student.profile.saga.repository;

import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.SagaEvent;
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
}
