package ca.bc.gov.educ.api.student.profile.saga.controller;

import ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome;
import ca.bc.gov.educ.api.student.profile.saga.constants.EventType;
import ca.bc.gov.educ.api.student.profile.saga.endpoint.StudentProfileSagaEndpoint;
import ca.bc.gov.educ.api.student.profile.saga.exception.SagaRuntimeException;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.ump.StudentProfileCommentsSagaOrchestrator;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.ump.StudentProfileCompleteSagaOrchestrator;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.ump.StudentProfileRejectSagaOrchestrator;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.ump.StudentProfileReturnSagaOrchestrator;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileCommentsSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileCompleteSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileRequestRejectActionSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileReturnActionSagaData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RestController;

import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.*;
import static lombok.AccessLevel.PRIVATE;

@RestController
@EnableResourceServer
@Slf4j
public class StudentProfileSagaController implements StudentProfileSagaEndpoint {

  @Getter(PRIVATE)
  private final SagaService sagaService;

  @Getter(PRIVATE)
  private final StudentProfileCompleteSagaOrchestrator studentProfileCompleteSagaOrchestrator;

  @Getter(PRIVATE)
  private final StudentProfileRejectSagaOrchestrator studentProfileRejectSagaOrchestrator;

  @Getter(PRIVATE)
  private final StudentProfileCommentsSagaOrchestrator studentProfileCommentsSagaOrchestrator;

  @Getter(PRIVATE)
  private final StudentProfileReturnSagaOrchestrator studentProfileReturnSagaOrchestrator;

  private static final String STUDENT_PROFILE_SAGA_API = "STUDENT_PROFILE_SAGA_API";

  @Autowired
  public StudentProfileSagaController(final SagaService sagaService, final StudentProfileCompleteSagaOrchestrator studentProfileCompleteSagaOrchestrator, StudentProfileRejectSagaOrchestrator studentProfileRejectSagaOrchestrator, StudentProfileCommentsSagaOrchestrator studentProfileCommentsSagaOrchestrator, StudentProfileReturnSagaOrchestrator studentProfileReturnSagaOrchestrator) {
    this.sagaService = sagaService;
    this.studentProfileCompleteSagaOrchestrator = studentProfileCompleteSagaOrchestrator;
    this.studentProfileRejectSagaOrchestrator = studentProfileRejectSagaOrchestrator;
    this.studentProfileCommentsSagaOrchestrator = studentProfileCommentsSagaOrchestrator;
    this.studentProfileReturnSagaOrchestrator = studentProfileReturnSagaOrchestrator;
  }

  @Override
  public ResponseEntity<Void> completeStudentProfile(StudentProfileCompleteSagaData studentProfileCompleteSagaData) {
    try {
      final Saga saga = getSagaService().createSagaRecord(studentProfileCompleteSagaData, STUDENT_PROFILE_COMPLETE_SAGA.toString(), STUDENT_PROFILE_SAGA_API);
      getStudentProfileCompleteSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .sagaId(saga.getSagaId())
          .build());
      return ResponseEntity.noContent().build();
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<Void> submitStudentProfileComment(StudentProfileCommentsSagaData studentProfileCommentsSagaData) {
    try {
      final Saga saga = getSagaService().createSagaRecord(studentProfileCommentsSagaData, STUDENT_PROFILE_COMMENTS_SAGA.toString(), STUDENT_PROFILE_SAGA_API);
      getStudentProfileCommentsSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .sagaId(saga.getSagaId())
          .build());
      return ResponseEntity.noContent().build();
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<Void> rejectStudentProfile(StudentProfileRequestRejectActionSagaData studentProfileRequestRejectActionSagaData) {
    try {
      final Saga saga = getSagaService().createSagaRecord(studentProfileRequestRejectActionSagaData, STUDENT_PROFILE_REJECT_SAGA.toString(), STUDENT_PROFILE_SAGA_API);
      getStudentProfileRejectSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .sagaId(saga.getSagaId())
          .build());
      return ResponseEntity.noContent().build();
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<Void> returnStudentProfile(StudentProfileReturnActionSagaData studentProfileReturnActionSagaData) {
    try {
      final Saga saga = getSagaService().createSagaRecord(studentProfileReturnActionSagaData, STUDENT_PROFILE_RETURN_SAGA.toString(), STUDENT_PROFILE_SAGA_API);
      getStudentProfileReturnSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .sagaId(saga.getSagaId())
          .build());
      return ResponseEntity.noContent().build();
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }


}

