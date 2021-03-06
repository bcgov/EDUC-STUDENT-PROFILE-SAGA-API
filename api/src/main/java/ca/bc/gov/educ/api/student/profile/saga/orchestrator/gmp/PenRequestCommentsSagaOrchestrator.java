package ca.bc.gov.educ.api.student.profile.saga.orchestrator.gmp;

import ca.bc.gov.educ.api.student.profile.saga.mappers.v1.PenRequestCommentsMapper;
import ca.bc.gov.educ.api.student.profile.saga.messaging.MessagePublisher;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestCommentsSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestSagaData;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventOutcome.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.*;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaEnum.PEN_REQUEST_COMMENTS_SAGA;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.PEN_REQUEST_API_TOPIC;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.PEN_REQUEST_COMMENTS_SAGA_TOPIC;

@Component
@Slf4j
public class PenRequestCommentsSagaOrchestrator extends BasePenReqSagaOrchestrator<PenRequestCommentsSagaData> {
  private static final PenRequestCommentsMapper mapper = PenRequestCommentsMapper.mapper;


  @Autowired
  public PenRequestCommentsSagaOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher) {
    super(sagaService, messagePublisher, PenRequestCommentsSagaData.class, PEN_REQUEST_COMMENTS_SAGA.toString(), PEN_REQUEST_COMMENTS_SAGA_TOPIC.toString());
  }

  /**
   * this is the source of truth for this particular saga flow.
   */
  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
      .step(INITIATED, INITIATE_SUCCESS, ADD_PEN_REQUEST_COMMENT, this::executeAddPenRequestComments)
      .step(ADD_PEN_REQUEST_COMMENT, PEN_REQUEST_COMMENT_ADDED, GET_PEN_REQUEST, this::executeGetPenRequest)
      .step(ADD_PEN_REQUEST_COMMENT, PEN_REQUEST_COMMENT_ALREADY_EXIST, GET_PEN_REQUEST, this::executeGetPenRequest)
      .step(GET_PEN_REQUEST, PEN_REQUEST_FOUND, UPDATE_PEN_REQUEST, this::executeUpdatePenRequest)
      .step(UPDATE_PEN_REQUEST, PEN_REQUEST_UPDATED, MARK_SAGA_COMPLETE, this::markSagaComplete);
  }

  /**
   * it will send a message to pen request api topic to add a comment.
   *
   * @param event                      current event.
   * @param saga                       the model object.
   * @param penRequestCommentsSagaData the payload as the object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void executeAddPenRequestComments(final Event event, final Saga saga, final PenRequestCommentsSagaData penRequestCommentsSagaData) throws IOException, InterruptedException, TimeoutException {
    val eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(ADD_PEN_REQUEST_COMMENT.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val penRequestComments = mapper.toPenReqComments(penRequestCommentsSagaData);
    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(ADD_PEN_REQUEST_COMMENT)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(JsonUtil.getJsonStringFromObject(penRequestComments))
      .build();
    this.postMessageToTopic(PEN_REQUEST_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PEN_REQUEST_API_TOPIC for ADD_PEN_REQUEST_COMMENT Event.");
  }

  /**
   * this method will update the payload according the saga type. here it will update for comment saga.
   *
   * @param penRequestSagaData         the model object.
   * @param penRequestCommentsSagaData the payload as the object.
   */
  @Override
  protected void updatePenRequestPayload(final PenRequestSagaData penRequestSagaData, final PenRequestCommentsSagaData penRequestCommentsSagaData) {
    penRequestSagaData.setPenRequestStatusCode(penRequestCommentsSagaData.getPenRequestStatusCode());
    penRequestSagaData.setUpdateUser(penRequestCommentsSagaData.getUpdateUser());
    penRequestSagaData.setStatusUpdateDate(LocalDateTime.now().withNano(0).toString());
  }

  /**
   * this method will return the pen request IDm to be send in the message.
   *
   * @param penRequestCommentsSagaData the payload as the object.
   * @return pen request id as string value.
   */
  @Override
  protected String updateGetPenRequestPayload(final PenRequestCommentsSagaData penRequestCommentsSagaData) {
    return penRequestCommentsSagaData.getPenRetrievalRequestID();
  }

}
