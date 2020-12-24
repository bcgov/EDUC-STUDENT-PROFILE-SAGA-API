package ca.bc.gov.educ.api.student.profile.saga.schedulers;

import ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static lombok.AccessLevel.PRIVATE;

/**
 * The type Event task scheduler.
 */
@Component
@Slf4j
public class EventTaskScheduler {
  @Getter(PRIVATE)
  private final Map<String, BaseOrchestrator<?>> sagaOrchestrators = new HashMap<>();
  @Getter(PRIVATE)
  private final SagaRepository sagaRepository;



  @Setter
  private List<String> statusFilters;

  /**
   * Instantiates a new Event task scheduler.
   *
   * @param sagaRepository the saga repository
   */
  @Autowired
  public EventTaskScheduler(SagaRepository sagaRepository) {
    this.sagaRepository = sagaRepository;
  }

  /**
   * Register saga orchestrators.
   *
   * @param sagaName     the saga name
   * @param orchestrator the orchestrator
   */
  public void registerSagaOrchestrators(String sagaName, BaseOrchestrator<?> orchestrator) {
    getSagaOrchestrators().put(sagaName, orchestrator);
  }

  /**
   * Poll event table and publish.
   *
   * @throws InterruptedException the interrupted exception
   * @throws IOException          the io exception
   * @throws TimeoutException     the timeout exception
   */
//Run the job every minute to check how many records are in IN_PROGRESS or STARTED status and has not been updated in last 5 minutes.
  @Scheduled(cron = "1 * * * * *")
  @SchedulerLock(name = "ProfileRequestSagaTablePoller",
      lockAtLeastFor = "55s", lockAtMostFor = "57s")
  public void pollEventTableAndPublish() throws InterruptedException, IOException, TimeoutException {
    var sagas = getSagaRepository().findAllByStatusIn(getStatusFilters());
    if (!sagas.isEmpty()) {
      for (val saga : sagas) {
        if (saga.getCreateDate().isBefore(LocalDateTime.now().minusMinutes(1))
            && getSagaOrchestrators().containsKey(saga.getSagaName())) {
          getSagaOrchestrators().get(saga.getSagaName()).replaySaga(saga);
        }
      }
    }
  }

  /**
   * Gets status filters.
   *
   * @return the status filters
   */
  public List<String> getStatusFilters() {
    if (statusFilters != null && !statusFilters.isEmpty()) {
      return statusFilters;
    } else {
      var statuses = new ArrayList<String>();
      statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
      statuses.add(SagaStatusEnum.STARTED.toString());
      return statuses;
    }
  }
}
