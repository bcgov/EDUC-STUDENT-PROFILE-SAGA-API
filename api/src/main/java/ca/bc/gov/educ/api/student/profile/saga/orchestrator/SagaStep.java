package ca.bc.gov.educ.api.student.profile.saga.orchestrator;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.struct.Event;

public interface SagaStep<T> {
    void apply(Event event, Saga saga, T sagaData) throws InterruptedException, TimeoutException, IOException;
}