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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome.SAGA_FORCE_STOPPED;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.FORCE_STOPPED;
import static lombok.AccessLevel.PRIVATE;

@RestController
@Slf4j
public class PenRequestSagaController extends BaseController implements PenRequestSagaEndpoint {
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
  public PenRequestSagaController(final SagaService sagaService, final PenRequestCompleteSagaOrchestrator penRequestCompleteSagaOrchestrator, final PenRequestCommentsSagaOrchestrator penRequestCommentsSagaOrchestrator, final PenRequestReturnSagaOrchestrator penRequestReturnSagaOrchestrator, final PenRequestRejectSagaOrchestrator penRequestRejectSagaOrchestrator, final PenRequestUnlinkSagaOrchestrator penRequestUnlinkSagaOrchestrator) {
    this.sagaService = sagaService;
    this.penRequestCompleteSagaOrchestrator = penRequestCompleteSagaOrchestrator;
    this.penRequestCommentsSagaOrchestrator = penRequestCommentsSagaOrchestrator;
    this.penRequestReturnSagaOrchestrator = penRequestReturnSagaOrchestrator;
    this.penRequestRejectSagaOrchestrator = penRequestRejectSagaOrchestrator;
    this.penRequestUnlinkSagaOrchestrator = penRequestUnlinkSagaOrchestrator;
  }

  @Override
  public ResponseEntity<String> completePENRequest(final PenRequestCompleteSagaData penRequestCompleteSagaData) {
    try {
      final var penRequestId = UUID.fromString(penRequestCompleteSagaData.getPenRequestID());
      final var sagaInProgress = this.getSagaService().findAllByPenRequestIdAndStatuses(penRequestId, this.getStatusesFilter());
      if (!sagaInProgress.isEmpty()) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      }
      final var saga = this.getSagaService().createPenRequestSagaRecord(penRequestCompleteSagaData, PEN_REQUEST_COMPLETE_SAGA.toString(), penRequestCompleteSagaData.getCreateUser(), penRequestId);
      this.getPenRequestCompleteSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .penRequestID(penRequestCompleteSagaData.getPenRequestID())
          .sagaId(saga.getSagaId())
          .build());
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<String> updatePenRequestAndAddComment(final PenRequestCommentsSagaData penRequestCommentsSagaData) {
    try {
      final var penRequestId = UUID.fromString(penRequestCommentsSagaData.getPenRetrievalRequestID());
      final var sagaInProgress = this.getSagaService().findAllByPenRequestIdAndStatuses(penRequestId, this.getStatusesFilter());
      if (!sagaInProgress.isEmpty()) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      }
      final Saga saga = this.getSagaService().createPenRequestSagaRecord(penRequestCommentsSagaData, PEN_REQUEST_COMMENTS_SAGA.toString(), penRequestCommentsSagaData.getCreateUser(), penRequestId);
      this.getPenRequestCommentsSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .penRequestID(penRequestCommentsSagaData.getPenRetrievalRequestID())
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
      final var penRequestId = UUID.fromString(penRequestReturnSagaData.getPenRetrievalRequestID());
      final var sagaInProgress = this.getSagaService().findAllByPenRequestIdAndStatuses(penRequestId, this.getStatusesFilter());
      if (!sagaInProgress.isEmpty()) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      }
      final Saga saga = this.getSagaService().createPenRequestSagaRecord(penRequestReturnSagaData, PEN_REQUEST_RETURN_SAGA.toString(), penRequestReturnSagaData.getCreateUser(), penRequestId);
      this.getPenRequestReturnSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .penRequestID(penRequestReturnSagaData.getPenRetrievalRequestID())
          .sagaId(saga.getSagaId())
          .build());
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<String> rejectPENRequest(final PenRequestRejectSagaData penRequestRejectSagaData) {
    try {
      final var penRequestId = UUID.fromString(penRequestRejectSagaData.getPenRetrievalRequestID());
      final var sagaInProgress = this.getSagaService().findAllByPenRequestIdAndStatuses(penRequestId, this.getStatusesFilter());
      if (!sagaInProgress.isEmpty()) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      }
      final Saga saga = this.getSagaService().createPenRequestSagaRecord(penRequestRejectSagaData, PEN_REQUEST_REJECT_SAGA.toString(), penRequestRejectSagaData.getCreateUser(), penRequestId);
      this.getPenRequestRejectSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .penRequestID(penRequestRejectSagaData.getPenRetrievalRequestID())
          .sagaId(saga.getSagaId())
          .build());
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  @Override
  @Transactional
  public ResponseEntity<String> unlinkPenRequest(final PenRequestUnlinkSagaData penRequestUnlinkSagaData) {
    try {
      final var penRequestId = UUID.fromString(penRequestUnlinkSagaData.getPenRetrievalRequestID());
      final var completeSagaInProgress = this.getSagaService().findByPenRequestIdAndStatusInAndSagaName(penRequestId, this.getStatusesFilter(), PEN_REQUEST_COMPLETE_SAGA.toString());
      if (completeSagaInProgress.isPresent()) { // force stop if a complete saga is in progress and then start the unlink saga.
        final var saga = completeSagaInProgress.get();
        saga.setStatus(FORCE_STOPPED.toString());
        saga.setSagaState(SAGA_FORCE_STOPPED.toString());
        this.getSagaService().updateSagaRecord(saga);
        this.getPenRequestCompleteSagaOrchestrator().publishSagaForceStopped(saga);
      }
      final Saga saga = this.getSagaService().createPenRequestSagaRecord(penRequestUnlinkSagaData, PEN_REQUEST_UNLINK_SAGA.toString(), penRequestUnlinkSagaData.getCreateUser(), UUID.fromString(penRequestUnlinkSagaData.getPenRetrievalRequestID()));
      this.getPenRequestUnlinkSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .penRequestID(penRequestUnlinkSagaData.getPenRetrievalRequestID())
          .sagaId(saga.getSagaId())
          .build());
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }
}

