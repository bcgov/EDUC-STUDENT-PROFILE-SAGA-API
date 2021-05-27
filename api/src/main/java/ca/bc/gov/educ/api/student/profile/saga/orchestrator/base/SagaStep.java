package ca.bc.gov.educ.api.student.profile.saga.orchestrator.base;

import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@FunctionalInterface
public interface SagaStep<T> {
  void apply(Event event, Saga saga, T sagaData) throws InterruptedException, TimeoutException, IOException;
}
