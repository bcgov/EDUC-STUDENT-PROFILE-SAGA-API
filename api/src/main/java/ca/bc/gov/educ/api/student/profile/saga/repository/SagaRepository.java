package ca.bc.gov.educ.api.student.profile.saga.repository;

import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaRepository extends CrudRepository<Saga, UUID> {
  List<Saga> findAllByStatusIn(List<String> statuses);
  List<Saga> findAllByPenRequestIdAndStatusIn(UUID penRequestId, List<String> statuses);
  List<Saga> findAllByProfileRequestIdAndStatusIn(UUID profileRequestId, List<String> statuses);
  List<Saga> findAll();
  Optional<Saga> findByPenRequestIdAndStatusInAndSagaName(UUID penRequestId, List<String> statuses, String sagaName);
  List<Saga> findAllByCreateDateBefore(LocalDateTime createDate);
}
