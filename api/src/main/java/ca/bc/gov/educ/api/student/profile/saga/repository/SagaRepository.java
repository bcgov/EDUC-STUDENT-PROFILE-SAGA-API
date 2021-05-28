package ca.bc.gov.educ.api.student.profile.saga.repository;

import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaRepository extends JpaRepository<Saga, UUID>, JpaSpecificationExecutor<Saga> {
  List<Saga> findAllByStatusIn(List<String> statuses);

  List<Saga> findAllByPenRequestIdAndStatusIn(UUID penRequestId, List<String> statuses);

  List<Saga> findAllByProfileRequestIdAndStatusIn(UUID profileRequestId, List<String> statuses);

  Optional<Saga> findByPenRequestIdAndStatusInAndSagaName(UUID penRequestId, List<String> statuses, String sagaName);

  List<Saga> findAllByCreateDateBefore(LocalDateTime createDate);
}
