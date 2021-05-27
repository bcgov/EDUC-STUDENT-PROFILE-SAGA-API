package ca.bc.gov.educ.api.student.profile.saga.controller.v1;

import ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome;
import ca.bc.gov.educ.api.student.profile.saga.constants.EventType;
import ca.bc.gov.educ.api.student.profile.saga.controller.BaseController;
import ca.bc.gov.educ.api.student.profile.saga.endpoint.v1.StudentProfileSagaEndpoint;
import ca.bc.gov.educ.api.student.profile.saga.exception.EntityNotFoundException;
import ca.bc.gov.educ.api.student.profile.saga.exception.SagaRuntimeException;
import ca.bc.gov.educ.api.student.profile.saga.mappers.v1.SagaMapper;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.ump.StudentProfileCommentsSagaOrchestrator;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.ump.StudentProfileCompleteSagaOrchestrator;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.ump.StudentProfileRejectSagaOrchestrator;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.ump.StudentProfileReturnSagaOrchestrator;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaSearchService;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileCommentsSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileCompleteSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileRequestRejectActionSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileReturnActionSagaData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.*;
import static lombok.AccessLevel.PRIVATE;

@RestController
@Slf4j
public class StudentProfileSagaController extends BaseController implements StudentProfileSagaEndpoint {

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

  private final SagaSearchService sagaSearchService;

  @Autowired
  public StudentProfileSagaController(final SagaService sagaService, final StudentProfileCompleteSagaOrchestrator studentProfileCompleteSagaOrchestrator, final StudentProfileRejectSagaOrchestrator studentProfileRejectSagaOrchestrator, final StudentProfileCommentsSagaOrchestrator studentProfileCommentsSagaOrchestrator, final StudentProfileReturnSagaOrchestrator studentProfileReturnSagaOrchestrator, final SagaSearchService sagaSearchService) {
    this.sagaService = sagaService;
    this.studentProfileCompleteSagaOrchestrator = studentProfileCompleteSagaOrchestrator;
    this.studentProfileRejectSagaOrchestrator = studentProfileRejectSagaOrchestrator;
    this.studentProfileCommentsSagaOrchestrator = studentProfileCommentsSagaOrchestrator;
    this.studentProfileReturnSagaOrchestrator = studentProfileReturnSagaOrchestrator;
    this.sagaSearchService = sagaSearchService;
  }

  @Override
  public ResponseEntity<String> completeStudentProfile(final StudentProfileCompleteSagaData studentProfileCompleteSagaData) {
    try {
      final var profileRequestId = UUID.fromString(studentProfileCompleteSagaData.getStudentProfileRequestID());
      final var sagaInProgress = this.getSagaService().findAllByProfileRequestIdAndStatuses(profileRequestId, this.getStatusesFilter());
      if (!sagaInProgress.isEmpty()) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      }
      final Saga saga = this.getSagaService().createProfileRequestSagaRecord(studentProfileCompleteSagaData, STUDENT_PROFILE_COMPLETE_SAGA.toString(), studentProfileCompleteSagaData.getCreateUser(), profileRequestId);
      this.getStudentProfileCompleteSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .sagaId(saga.getSagaId())
          .studentRequestID(studentProfileCompleteSagaData.getStudentProfileRequestID())
          .build());
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<String> submitStudentProfileComment(final StudentProfileCommentsSagaData studentProfileCommentsSagaData) {
    try {
      final var profileRequestId = UUID.fromString(studentProfileCommentsSagaData.getStudentProfileRequestID());
      final var sagaInProgress = this.getSagaService().findAllByProfileRequestIdAndStatuses(profileRequestId, this.getStatusesFilter());
      if (!sagaInProgress.isEmpty()) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      }
      final Saga saga = this.getSagaService().createProfileRequestSagaRecord(studentProfileCommentsSagaData, STUDENT_PROFILE_COMMENTS_SAGA.toString(), studentProfileCommentsSagaData.getCreateUser(), profileRequestId);
      this.getStudentProfileCommentsSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .studentRequestID(studentProfileCommentsSagaData.getStudentProfileRequestID())
          .sagaId(saga.getSagaId())
          .build());
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<String> rejectStudentProfile(final StudentProfileRequestRejectActionSagaData studentProfileRequestRejectActionSagaData) {
    try {
      final var profileRequestId = UUID.fromString(studentProfileRequestRejectActionSagaData.getStudentProfileRequestID());
      final var sagaInProgress = this.getSagaService().findAllByProfileRequestIdAndStatuses(profileRequestId, this.getStatusesFilter());
      if (!sagaInProgress.isEmpty()) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      }
      final Saga saga = this.getSagaService().createProfileRequestSagaRecord(studentProfileRequestRejectActionSagaData, STUDENT_PROFILE_REJECT_SAGA.toString(), studentProfileRequestRejectActionSagaData.getCreateUser(), profileRequestId);
      this.getStudentProfileRejectSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .studentRequestID(studentProfileRequestRejectActionSagaData.getStudentProfileRequestID())
          .sagaId(saga.getSagaId())
          .build());
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<String> returnStudentProfile(final StudentProfileReturnActionSagaData studentProfileReturnActionSagaData) {
    try {
      final var profileRequestId = UUID.fromString(studentProfileReturnActionSagaData.getStudentProfileRequestID());
      final var sagaInProgress = this.getSagaService().findAllByProfileRequestIdAndStatuses(profileRequestId, this.getStatusesFilter());
      if (!sagaInProgress.isEmpty()) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      }
      final Saga saga = this.getSagaService().createProfileRequestSagaRecord(studentProfileReturnActionSagaData, STUDENT_PROFILE_RETURN_SAGA.toString(), studentProfileReturnActionSagaData.getCreateUser(), profileRequestId);
      this.getStudentProfileReturnSagaOrchestrator().executeSagaEvent(Event.builder()
          .eventType(EventType.INITIATED)
          .eventOutcome(EventOutcome.INITIATE_SUCCESS)
          .studentRequestID(studentProfileReturnActionSagaData.getStudentProfileRequestID())
          .sagaId(saga.getSagaId())
          .build());
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<ca.bc.gov.educ.api.student.profile.saga.struct.Saga> getSagaBySagaID(final UUID sagaID) {
    return this.getSagaService().findSagaById(sagaID).map(SagaMapper.mapper::toStruct).map(ResponseEntity::ok).orElseThrow(() -> new EntityNotFoundException(Saga.class, "sagaID", sagaID.toString()));
  }

  @Override
  public List<SagaEvent> getSagaEventsBySagaID(final UUID sagaID) {
    return this.getSagaService().findAllSagaStates(Saga.builder().sagaId(sagaID).build()).stream().map(SagaMapper.mapper::toEventStruct).collect(Collectors.toList());
  }

  @Override
  public Page<ca.bc.gov.educ.api.student.profile.saga.struct.Saga> findAll(final Integer pageNumber, final Integer pageSize, final String sortCriteriaJson, final String searchCriteriaListJson) {
    val sorts = new ArrayList<Sort.Order>();
    val specs = this.sagaSearchService.setSpecificationAndSortCriteria(sortCriteriaJson, searchCriteriaListJson, sorts);
    return this.sagaService.findAll(specs, pageNumber, pageSize, sorts).map(SagaMapper.mapper::toStruct);
  }

}

