package ca.bc.gov.educ.api.student.profile.saga.schedulers;

import ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.base.Orchestrator;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final Map<String, Orchestrator> sagaOrchestrators = new HashMap<>();
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
  public EventTaskScheduler(final SagaRepository sagaRepository, final List<Orchestrator> orchestrators) {
    this.sagaRepository = sagaRepository;
    orchestrators.forEach(orchestrator -> this.registerSagaOrchestrators(orchestrator.getSagaName(), orchestrator));
  }

  /**
   * Register saga orchestrators.
   *
   * @param sagaName     the saga name
   * @param orchestrator the orchestrator
   */
  public void registerSagaOrchestrators(final String sagaName, final Orchestrator orchestrator) {
    this.getSagaOrchestrators().put(sagaName, orchestrator);
  }

  /**
   * Poll event table and publish.
   *
   * @throws InterruptedException the interrupted exception
   * @throws IOException          the io exception
   * @throws TimeoutException     the timeout exception
   */
//Run the job every minute to check how many records are in IN_PROGRESS or STARTED status and has not been updated in last 5 minutes.
  @Scheduled(cron = "${scheduled.jobs.poll.uncompleted.saga.records.cron}")
  @SchedulerLock(name = "ProfileRequestSagaTablePoller",
      lockAtLeastFor = "${scheduled.jobs.poll.uncompleted.saga.records.cron.lockAtLeastFor}", lockAtMostFor = "$" +
      "{scheduled.jobs.poll.uncompleted.saga.records.cron.lockAtMostFor}")
  public void pollEventTableAndPublish() throws InterruptedException, IOException, TimeoutException {
    LockAssert.assertLocked();
    final var sagas = this.getSagaRepository().findAllByStatusIn(this.getStatusFilters());
    if (!sagas.isEmpty()) {
      for (val saga : sagas) {
        if (saga.getCreateDate().isBefore(LocalDateTime.now().minusMinutes(1))
          && this.getSagaOrchestrators().containsKey(saga.getSagaName())) {
          this.getSagaOrchestrators().get(saga.getSagaName()).replaySaga(saga);
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
    if (this.statusFilters != null && !this.statusFilters.isEmpty()) {
      return this.statusFilters;
    } else {
      final var statuses = new ArrayList<String>();
      statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
      statuses.add(SagaStatusEnum.STARTED.toString());
      return statuses;
    }
  }
}
