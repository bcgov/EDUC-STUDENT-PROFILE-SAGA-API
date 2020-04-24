package ca.bc.gov.educ.api.student.profile.saga.orchestrator;

import ca.bc.gov.educ.api.student.profile.saga.mappers.StudentProfileCommentsMapper;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessageSubscriber;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.poll.EventTaskScheduler;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.*;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.STUDENT_PROFILE_API_TOPIC;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.STUDENT_PROFILE_COMMENTS_SAGA_TOPIC;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.STUDENT_PROFILE_COMMENTS_SAGA;

@Component
@Slf4j
public class StudentProfileCommentsSagaOrchestrator extends BaseOrchestrator<StudentProfileCommentsSagaData> {
  private static final StudentProfileCommentsMapper mapper = StudentProfileCommentsMapper.mapper;

  @Override
  protected void populateNextStepsMap() {
    registerStepToExecute(INITIATED, INITIATE_SUCCESS, ADD_PEN_REQUEST_COMMENT, this::executeAddPenRequestComments);

    registerStepToExecute(ADD_PEN_REQUEST_COMMENT, PEN_REQUEST_COMMENT_ADDED, GET_PEN_REQUEST, this::executeGetPenRequest);
    registerStepToExecute(ADD_PEN_REQUEST_COMMENT, PEN_REQUEST_COMMENT_ALREADY_EXIST, GET_PEN_REQUEST, this::executeGetPenRequest);

    registerStepToExecute(GET_PEN_REQUEST, PEN_REQUEST_FOUND, UPDATE_PEN_REQUEST, this::executeUpdatePenRequest);
    registerStepToExecute(UPDATE_PEN_REQUEST, PEN_REQUEST_UPDATED, MARK_SAGA_COMPLETE, this::markSagaComplete);
  }

  @Autowired
  public StudentProfileCommentsSagaOrchestrator(SagaService sagaService, MessagePublisher messagePublisher, MessageSubscriber messageSubscriber, EventTaskScheduler taskScheduler) {
    super(sagaService, messagePublisher, messageSubscriber, taskScheduler, StudentProfileCommentsSagaData.class, STUDENT_PROFILE_COMMENTS_SAGA.toString(), STUDENT_PROFILE_COMMENTS_SAGA_TOPIC.toString());
  }

  private void executeAddPenRequestComments(Event event, Saga saga, StudentProfileCommentsSagaData studentProfileCommentsSagaData) throws IOException, InterruptedException, TimeoutException {
    SagaEvent eventState = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(ADD_PEN_REQUEST_COMMENT.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventState);
    StudentProfileComments studentProfileComments = mapper.toPenReqComments(studentProfileCommentsSagaData);
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
            .eventType(ADD_PEN_REQUEST_COMMENT)
            .replyTo(getTopicToSubscribe())
            .eventPayload(JsonUtil.getJsonStringFromObject(studentProfileComments))
            .build();
    postMessageToTopic(STUDENT_PROFILE_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_PROFILE_API_TOPIC for ADD_PEN_REQUEST_COMMENT Event.");
  }

  protected void executeGetPenRequest(Event event, Saga saga, StudentProfileCommentsSagaData studentProfileCommentsSagaData) throws InterruptedException, TimeoutException, IOException {
    SagaEvent eventState = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(GET_PEN_REQUEST.toString()); // set current event as saga state.
    getSagaService().updateAttachedSagaWithEvents(saga, eventState);
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
            .eventType(GET_PEN_REQUEST)
            .replyTo(getTopicToSubscribe())
            .eventPayload(studentProfileCommentsSagaData.getPenRetrievalRequestID())
            .build();
    postMessageToTopic(STUDENT_PROFILE_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_PROFILE_API_TOPIC for GET_PEN_REQUEST Event.");
  }

  protected void executeUpdatePenRequest(Event event, Saga saga, StudentProfileCommentsSagaData studentProfileCommentsSagaData) throws IOException, InterruptedException, TimeoutException {
    SagaEvent eventState = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(UPDATE_PEN_REQUEST.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventState);
    StudentProfileSagaData penRequestSagaData = JsonUtil.getJsonObjectFromString(StudentProfileSagaData.class, event.getEventPayload());
    updateStudentProfilePayload(penRequestSagaData, studentProfileCommentsSagaData);
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
            .eventType(UPDATE_PEN_REQUEST)
            .replyTo(getTopicToSubscribe())
            .eventPayload(JsonUtil.getJsonStringFromObject(penRequestSagaData))
            .build();
    postMessageToTopic(STUDENT_PROFILE_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_PROFILE_API_TOPIC for UPDATE_PEN_REQUEST Event.");
  }

  protected void updateStudentProfilePayload(StudentProfileSagaData studentProfileSagaData, StudentProfileCommentsSagaData studentProfileCommentsSagaData) {
    studentProfileSagaData.setPenRequestStatusCode(studentProfileCommentsSagaData.getPenRequestStatusCode());
    studentProfileSagaData.setUpdateUser(getSagaName());
  }

}
