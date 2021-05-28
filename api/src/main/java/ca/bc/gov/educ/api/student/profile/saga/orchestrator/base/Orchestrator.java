package ca.bc.gov.educ.api.student.profile.saga.orchestrator.base;


import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * The interface Orchestrator.
 */
public interface Orchestrator {


  /**
   * Gets saga name.
   *
   * @return the saga name
   */
  String getSagaName();

  /**
   * Replay saga.
   *
   * @param saga the saga
   * @throws IOException          the io exception
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   */
  void replaySaga(Saga saga) throws IOException, InterruptedException, TimeoutException;
}
