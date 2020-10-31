package ca.bc.gov.educ.api.student.profile.saga.orchestrator.ump;

import ca.bc.gov.educ.api.student.profile.saga.mappers.StudentProfileCommentsMapper;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessageSubscriber;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.schedulers.EventTaskScheduler;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileComments;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileCommentsSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileSagaData;
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
public class StudentProfileCommentsSagaOrchestrator extends BaseProfileReqSagaOrchestrator<StudentProfileCommentsSagaData> {
  private static final StudentProfileCommentsMapper mapper = StudentProfileCommentsMapper.mapper;

  @Autowired
  public StudentProfileCommentsSagaOrchestrator(SagaService sagaService, MessagePublisher messagePublisher, MessageSubscriber messageSubscriber, EventTaskScheduler taskScheduler) {
    super(sagaService, messagePublisher, messageSubscriber, taskScheduler, StudentProfileCommentsSagaData.class, STUDENT_PROFILE_COMMENTS_SAGA.toString(), STUDENT_PROFILE_COMMENTS_SAGA_TOPIC.toString());
  }

  @Override
  public void populateStepsToExecuteMap() {
    stepBuilder()
        .step(INITIATED, INITIATE_SUCCESS, ADD_STUDENT_PROFILE_COMMENT, this::executeAddStudentProfileRequestComments)
        .step(ADD_STUDENT_PROFILE_COMMENT, STUDENT_PROFILE_COMMENT_ADDED, GET_STUDENT_PROFILE, this::executeGetProfileRequest)
        .step(ADD_STUDENT_PROFILE_COMMENT, STUDENT_PROFILE_COMMENT_ALREADY_EXIST, GET_STUDENT_PROFILE, this::executeGetProfileRequest)
        .step(GET_STUDENT_PROFILE, STUDENT_PROFILE_FOUND, UPDATE_STUDENT_PROFILE, this::executeUpdateProfileRequest)
        .step(UPDATE_STUDENT_PROFILE, STUDENT_PROFILE_UPDATED, MARK_SAGA_COMPLETE, this::markSagaComplete);

  }
  /**
   * it will send a message to student profile request api topic to add a comment.
   *
   * @param event                           current event.
   * @param saga                            the model object.
   * @param studentProfileCommentsSagaData  the payload as the object.
   * @throws InterruptedException           if thread is interrupted.
   * @throws IOException                    if there is connectivity problem
   * @throws TimeoutException               if connection to messaging system times out.
   */
  protected void executeAddStudentProfileRequestComments(Event event, Saga saga, StudentProfileCommentsSagaData studentProfileCommentsSagaData) throws IOException, InterruptedException, TimeoutException {
    SagaEvent eventState = createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(ADD_STUDENT_PROFILE_COMMENT.toString());
    getSagaService().updateAttachedSagaWithEvents(saga, eventState);
    StudentProfileComments studentProfileComments = mapper.toComments(studentProfileCommentsSagaData);
    Event nextEvent = Event.builder().sagaId(saga.getSagaId())
        .eventType(ADD_STUDENT_PROFILE_COMMENT)
        .replyTo(getTopicToSubscribe())
        .eventPayload(JsonUtil.getJsonStringFromObject(studentProfileComments))
        .build();
    postMessageToTopic(STUDENT_PROFILE_API_TOPIC.toString(), nextEvent);
    log.info("message sent to STUDENT_PROFILE_API_TOPIC for ADD_STUDENT_PROFILE_COMMENT Event.");
  }

  /**
   * this method will update the payload according the saga type. here it will update for comment saga.
   *
   * @param studentProfileSagaData         the model object.
   * @param studentProfileCommentsSagaData the payload as the object.
   */
  @Override
  protected void updateProfileRequestPayload(StudentProfileSagaData studentProfileSagaData, StudentProfileCommentsSagaData studentProfileCommentsSagaData) {
    studentProfileSagaData.setStudentRequestStatusCode(studentProfileCommentsSagaData.getStudentProfileRequestStatusCode());
    studentProfileSagaData.setUpdateUser(studentProfileCommentsSagaData.getUpdateUser());
  }

  /**
   * this method will return the student profile request ID to be sent in the message.
   *
   * @param studentProfileCommentsSagaData  the payload as the object.
   * @return                                student profile request id as string value.
   */
  @Override
  protected String updateGetProfileRequestPayload(StudentProfileCommentsSagaData studentProfileCommentsSagaData) {
    return studentProfileCommentsSagaData.getStudentProfileRequestID();
  }

}
