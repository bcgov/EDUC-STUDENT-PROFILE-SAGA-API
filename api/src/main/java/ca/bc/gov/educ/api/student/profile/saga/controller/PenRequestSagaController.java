package ca.bc.gov.educ.api.student.profile.saga.controller;

import ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome;
import ca.bc.gov.educ.api.student.profile.saga.constants.EventType;
import ca.bc.gov.educ.api.student.profile.saga.endpoint.PenRequestSagaEndpoint;
import ca.bc.gov.educ.api.student.profile.saga.exception.SagaRuntimeException;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.gmp.*;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.*;
import static lombok.AccessLevel.PRIVATE;

@RestController
@EnableResourceServer
@Slf4j
public class PenRequestSagaController extends BaseController implements PenRequestSagaEndpoint {
  private static final String STUDENT_PROFILE_SAGA_API = "STUDENT_PROFILE_SAGA_API";
  @Getter(PRIVATE)
  private final SagaService sagaService;

  @Getter(PRIVATE)
  private final PenRequestCompleteSagaOrchestrator penRequestCompleteSagaOrchestrator;


  @Getter(PRIVATE)
  private final PenRequestCommentsSagaOrchestrator penRequestCommentsSagaOrchestrator;

  @Getter(PRIVATE)
  private final PenRequestReturnSagaOrchestrator penRequestReturnSagaOrchestrator;

  @Getter(PRIVATE)
  private final PenRequestRejectSagaOrchestrator penRequestRejectSagaOrchestrator;

  @Getter(PRIVATE)
  private final PenRequestUnlinkSagaOrchestrator penRequestUnlinkSagaOrchestrator;

  @Autowired
  public PenRequestSagaController(final SagaService sagaService, final PenRequestCompleteSagaOrchestrator penRequestCompleteSagaOrchestrator, PenRequestCommentsSagaOrchestrator penRequestCommentsSagaOrchestrator, PenRequestReturnSagaOrchestrator penRequestReturnSagaOrchestrator, PenRequestRejectSagaOrchestrator penRequestRejectSagaOrchestrator, PenRequestUnlinkSagaOrchestrator penRequestUnlinkSagaOrchestrator) {
    this.sagaService = sagaService;
    this.penRequestCompleteSagaOrchestrator = penRequestCompleteSagaOrchestrator;
    this.penRequestCommentsSagaOrchestrator = penRequestCommentsSagaOrchestrator;
    this.penRequestReturnSagaOrchestrator = penRequestReturnSagaOrchestrator;
    this.penRequestRejectSagaOrchestrator = penRequestRejectSagaOrchestrator;
    this.penRequestUnlinkSagaOrchestrator = penRequestUnlinkSagaOrchestrator;
  }

  @Override
  public ResponseEntity<String> completePENRequest(PenRequestCompleteSagaData penRequestCompleteSagaData) {
    try {
      var penRequestId = UUID.fromString(penRequestCompleteSagaData.getPenRequestID());
      var sagaInProgress = getSagaService().findAllByPenRequestIdAndStatuses(penRequestId, getStatusesFilter());
      if (!sagaInProgress.isEmpty()) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      }
      var saga = getSagaService().createPenRequestSagaRecord(penRequestCompleteSagaData, PEN_REQUEST_COMPLETE_SAGA.toString(), STUDENT_PROFILE_SAGA_API, penRequestId);
      getPenRequestCompleteSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .sagaId(saga.getSagaId())
          .build());
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<String> updatePenRequestAndAddComment(PenRequestCommentsSagaData penRequestCommentsSagaData) {
    try {
      var penRequestId = UUID.fromString(penRequestCommentsSagaData.getPenRetrievalRequestID());
      var sagaInProgress = getSagaService().findAllByPenRequestIdAndStatuses(penRequestId, getStatusesFilter());
      if (!sagaInProgress.isEmpty()) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      }
      final Saga saga = getSagaService().createPenRequestSagaRecord(penRequestCommentsSagaData, PEN_REQUEST_COMMENTS_SAGA.toString(), STUDENT_PROFILE_SAGA_API, penRequestId);
      getPenRequestCommentsSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .sagaId(saga.getSagaId())
          .build());
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<String> returnPENRequest(final PenRequestReturnSagaData penRequestReturnSagaData) {
    try {
      var penRequestId = UUID.fromString(penRequestReturnSagaData.getPenRetrievalRequestID());
      var sagaInProgress = getSagaService().findAllByPenRequestIdAndStatuses(penRequestId, getStatusesFilter());
      if (!sagaInProgress.isEmpty()) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      }
      final Saga saga = getSagaService().createPenRequestSagaRecord(penRequestReturnSagaData, PEN_REQUEST_RETURN_SAGA.toString(), STUDENT_PROFILE_SAGA_API, penRequestId);
      getPenRequestReturnSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .sagaId(saga.getSagaId())
          .build());
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<String> rejectPENRequest(PenRequestRejectSagaData penRequestRejectSagaData) {
    try {
      var penRequestId = UUID.fromString(penRequestRejectSagaData.getPenRetrievalRequestID());
      var sagaInProgress = getSagaService().findAllByPenRequestIdAndStatuses(penRequestId, getStatusesFilter());
      if (!sagaInProgress.isEmpty()) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      }
      final Saga saga = getSagaService().createPenRequestSagaRecord(penRequestRejectSagaData, PEN_REQUEST_REJECT_SAGA.toString(), STUDENT_PROFILE_SAGA_API, penRequestId);
      getPenRequestRejectSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .sagaId(saga.getSagaId())
          .build());
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<String> unlinkPenRequest(PenRequestUnlinkSagaData penRequestUnlinkSagaData) {
    try {
      final Saga saga = getSagaService().createPenRequestSagaRecord(penRequestUnlinkSagaData, PEN_REQUEST_UNLINK_SAGA.toString(), STUDENT_PROFILE_SAGA_API, UUID.fromString(penRequestUnlinkSagaData.getPenRetrievalRequestID()));
      getPenRequestUnlinkSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .sagaId(saga.getSagaId())
          .build());
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }
}

