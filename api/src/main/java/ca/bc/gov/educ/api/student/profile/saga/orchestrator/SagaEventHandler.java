package ca.bc.gov.educ.api.student.profile.saga.orchestrator;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import ca.bc.gov.educ.api.student.profile.saga.struct.Event;

public interface SagaEventHandler {
    void onSagaEvent(Event event) throws InterruptedException, IOException, TimeoutException;
}