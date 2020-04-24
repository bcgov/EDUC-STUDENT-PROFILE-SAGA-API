package ca.bc.gov.educ.api.student.profile.saga.poll;

import ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.BaseOrchestrator;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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

@Component
@Slf4j
public class EventTaskScheduler {
  @Getter(PRIVATE)
  private final Map<String, BaseOrchestrator<?>>  sagaOrchestrators = new HashMap<>();
  @Getter(PRIVATE)
  private final SagaRepository sagaRepository;

  @Autowired
  public EventTaskScheduler(SagaRepository sagaRepository) {
    this.sagaRepository = sagaRepository;
  }

  public void registerSagaOrchestrators(String sagaName, BaseOrchestrator<?> orchestrator) {
    getSagaOrchestrators().put(sagaName, orchestrator);
  }

  //Run the job every minute to check how many records are in IN_PROGRESS or STARTED status and has not been updated in last 5 minutes.
  @Scheduled(cron = "1 * * * * *")
  @SchedulerLock(name = "PenRequestSagaTablePoller",
          lockAtLeastFor = "950ms", lockAtMostFor = "980ms")
  public void pollEventTableAndPublish() throws InterruptedException, IOException, TimeoutException {
    List<String> status = new ArrayList<>();
    status.add(SagaStatusEnum.IN_PROGRESS.toString());
    status.add(SagaStatusEnum.STARTED.toString());
    List<Saga> sagas = getSagaRepository().findAllByStatusIn(status);
    if (!sagas.isEmpty()) {
      for (Saga saga : sagas) {
        if (saga.getUpdateDate().isBefore(LocalDateTime.now().minusMinutes(5))) {
          if(getSagaOrchestrators().containsKey(saga.getSagaName())) {
            getSagaOrchestrators().get(saga.getSagaName()).replaySaga(saga);
          }
        }
      }
    }
  }

}
