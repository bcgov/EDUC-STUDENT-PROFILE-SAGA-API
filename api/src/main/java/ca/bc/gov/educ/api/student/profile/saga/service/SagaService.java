package ca.bc.gov.educ.api.student.profile.saga.service;

import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaEventRepository;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
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
  public SagaService(final SagaRepository sagaRepository, SagaEventRepository sagaEventRepository) {
    this.sagaRepository = sagaRepository;
    this.sagaEventRepository = sagaEventRepository;
  }


  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Saga createSagaRecord(Object sagaData, String sagaName, String apiName, String studentProfileRequestId) throws JsonProcessingException {
    Saga saga = getSaga(JsonUtil.getJsonStringFromObject(sagaData), sagaName, apiName, studentProfileRequestId);
    return getSagaRepository().save(saga);
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
  public void updateAttachedSagaWithEvents(Saga saga, SagaEvent sagaEvent) {
    saga.setUpdateDate(LocalDateTime.now());
    getSagaRepository().save(saga);
    val result = getSagaEventRepository()
        .findBySagaAndSagaEventOutcomeAndSagaEventStateAndSagaStepNumber(saga, sagaEvent.getSagaEventOutcome(), sagaEvent.getSagaEventState(), sagaEvent.getSagaStepNumber() - 1); //check if the previous step was same and had same outcome, and it is due to replay.
    if (result.isEmpty()) {
      getSagaEventRepository().save(sagaEvent);
    }
  }

  public Optional<Saga> findSagaById(UUID sagaId) {
    return getSagaRepository().findById(sagaId);
  }

  public List<SagaEvent> findAllSagaStates(Saga saga) {
    return getSagaEventRepository().findBySaga(saga);
  }

  // this needs to be committed to db before next steps are taken.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Saga updateAttachedSaga(Saga saga) {
    return getSagaRepository().save(saga);
  }

  private Saga getSaga(String payload, String sagaName, String apiName, String studentProfileRequestId) {
    return Saga
        .builder()
        .payload(payload)
        .sagaName(sagaName)
        .status(STARTED.toString())
        .sagaState(INITIATED.toString())
        .sagaCompensated(false)
        .studentProfileRequestId(studentProfileRequestId)
        .createDate(LocalDateTime.now())
        .createUser(apiName)
        .updateUser(apiName)
        .updateDate(LocalDateTime.now())
        .build();
  }

}
