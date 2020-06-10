package ca.bc.gov.educ.api.student.profile.saga.repository;

import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SagaRepository extends CrudRepository<Saga, UUID> {
  List<Saga> findAllByStatusIn(List<String> sagaState);

  List<Saga> findAll();
}
