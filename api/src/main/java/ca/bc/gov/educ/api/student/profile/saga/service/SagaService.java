package ca.bc.gov.educ.api.student.profile.saga.service;

import ca.bc.gov.educ.api.student.profile.saga.exception.SagaRuntimeException;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaEventRepository;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.INITIATED;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.STARTED;
import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
public class SagaService {
  @Getter(AccessLevel.PRIVATE)
  private final SagaRepository sagaRepository;
  @Getter(PRIVATE)
  private final SagaEventRepository sagaEventRepository;

  @Autowired
  public SagaService(final SagaRepository sagaRepository, final SagaEventRepository sagaEventRepository) {
    this.sagaRepository = sagaRepository;
    this.sagaEventRepository = sagaEventRepository;
  }


  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Saga createProfileRequestSagaRecord(final Object sagaData, final String sagaName, final String user, final UUID profileRequestId) throws JsonProcessingException {
    val saga = this.getSaga(JsonUtil.getJsonStringFromObject(sagaData), sagaName, user);
    saga.setProfileRequestId(profileRequestId);
    return this.getSagaRepository().save(saga);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Saga createPenRequestSagaRecord(final Object sagaData, final String sagaName, final String user, final UUID penRequestId) throws JsonProcessingException {
    val saga = this.getSaga(JsonUtil.getJsonStringFromObject(sagaData), sagaName, user);
    saga.setPenRequestId(penRequestId);
    return this.getSagaRepository().save(saga);
  }

  /**
   * no need to do a get here as it is an attached entity
   * first find the child record, if exist do not add. this scenario may occur in replay process,
   * so dont remove this check. removing this check will lead to duplicate records in the child table.
   *
   * @param saga the saga object.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(multiplier = 2, delay = 2000))
  public void updateAttachedSagaWithEvents(final Saga saga, final SagaEvent sagaEvent) {
    saga.setUpdateDate(LocalDateTime.now());
    this.getSagaRepository().save(saga);
    val result = this.getSagaEventRepository()
      .findBySagaAndSagaEventOutcomeAndSagaEventStateAndSagaStepNumber(saga, sagaEvent.getSagaEventOutcome(), sagaEvent.getSagaEventState(), sagaEvent.getSagaStepNumber() - 1); //check if the previous step was same and had same outcome, and it is due to replay.
    if (result.isEmpty()) {
      this.getSagaEventRepository().save(sagaEvent);
    }
  }

  public Optional<Saga> findSagaById(final UUID sagaId) {
    return this.getSagaRepository().findById(sagaId);
  }

  public List<SagaEvent> findAllSagaStates(final Saga saga) {
    return this.getSagaEventRepository().findBySaga(saga);
  }

  private Saga getSaga(final String payload, final String sagaName, final String user) {
    return Saga
      .builder()
      .payload(payload)
      .sagaName(sagaName)
      .status(STARTED.toString())
      .sagaState(INITIATED.toString())
      .sagaCompensated(false)
      .createDate(LocalDateTime.now())
      .createUser(user)
      .updateUser(user)
      .updateDate(LocalDateTime.now())
      .build();
  }

  public List<Saga> findAllByPenRequestIdAndStatuses(final UUID penRequestId, final List<String> statuses) {
    return this.getSagaRepository().findAllByPenRequestIdAndStatusIn(penRequestId, statuses);
  }

  public List<Saga> findAllByProfileRequestIdAndStatuses(final UUID profileRequestId, final List<String> statuses) {
    return this.getSagaRepository().findAllByProfileRequestIdAndStatusIn(profileRequestId, statuses);
  }

  public Optional<Saga> findByPenRequestIdAndStatusInAndSagaName(final UUID penRequestId, final List<String> statuses, final String sagaName) {
    return this.getSagaRepository().findByPenRequestIdAndStatusInAndSagaName(penRequestId, statuses, sagaName);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void updateSagaRecord(final Saga saga) { // saga here MUST be an attached entity
    this.getSagaRepository().save(saga);
  }

  @Transactional(propagation = Propagation.SUPPORTS)
  public Page<Saga> findAll(final Specification<Saga> studentSpecs, final Integer pageNumber, final Integer pageSize, final List<Sort.Order> sorts) {
    final Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
    try {
      return this.getSagaRepository().findAll(studentSpecs, paging);
    } catch (final Exception ex) {
      throw new SagaRuntimeException(ex);
    }
  }
}
