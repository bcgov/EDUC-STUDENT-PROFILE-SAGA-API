package ca.bc.gov.educ.api.student.profile.saga.controller;

import ca.bc.gov.educ.api.student.profile.saga.BaseSagaApiTest;
import ca.bc.gov.educ.api.student.profile.saga.constants.v1.URL;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaEventRepository;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import lombok.val;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.INITIATED;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("java:S2699")
public class StudentProfileSagaControllerTest extends BaseSagaApiTest {
  public static final String PAYLOAD_STR = "  \"studentProfileRequestID\": \"ac335214-7252-1946-8172-589e58000004\",\n" +
    "  \"createUser\": \"om\",\n" +
    "  \"updateUser\": \"om\",\n" +
    "  \"email\": \"omprkshmishra@gmail.com\",\n" +
    "  \"identityType\": \"BASIC\",\n";
  @Autowired
  private SagaRepository repository;
  @Autowired
  private SagaEventRepository sagaEventRepository;

  @Autowired
  private MockMvc mockMvc;


  @Test
  @SuppressWarnings("java:S100")
  public void testRejectStudentProfile_whenRejectionReasonIsNull_shouldReturnStatusBadRequest() throws Exception {
    final String payload = "{\n" +
      PAYLOAD_STR +
      "}";
    this.mockMvc.perform(post(URL.BASE_URL + URL.STUDENT_PROFILE_REJECT_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_REJECT_SAGA"))).contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isBadRequest());
    final var results = this.repository.findAll();
    assertThat(results.isEmpty()).isTrue();
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testRejectStudentProfile_whenPayloadIsValid_shouldReturnStatusOk() throws Exception {
    final String payload = "{\n" +
      PAYLOAD_STR +
      "  \"rejectionReason\": \"rejected\"\n" +
      "}";
    this.mockMvc.perform(post(URL.BASE_URL + URL.STUDENT_PROFILE_REJECT_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_REJECT_SAGA"))).contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isOk());
    final var results = this.repository.findAll();
    assertThat(results.isEmpty()).isFalse();
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testReturnStudentProfile_whenCommentContentIsNull_shouldReturnStatusBadRequest() throws Exception {
    final String payload = "{\n" +
      PAYLOAD_STR +
      "}";
    this.mockMvc.perform(post(URL.BASE_URL + URL.STUDENT_PROFILE_RETURN_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_RETURN_SAGA"))).contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isBadRequest());
    final var results = this.repository.findAll();
    assertThat(results.isEmpty()).isTrue();
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testReturnStudentProfile_whenPayloadIsValid_shouldReturnStatusOk() throws Exception {
    final String payload = "{\n" +
      PAYLOAD_STR +
      "  \"staffMemberIDIRGUID\": \"AC335214725219468172589E58000004\",\n" +
      "  \"staffMemberName\": \"om\",\n" +
      "  \"commentContent\": \"please upload recent govt ID.\",\n" +
      "  \"commentTimestamp\": \"2020-06-10T09:52:00\"\n" +
      "}";
    this.mockMvc.perform(post(URL.BASE_URL + URL.STUDENT_PROFILE_RETURN_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_RETURN_SAGA"))).contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isOk());
    final var results = this.repository.findAll();
    assertThat(results.isEmpty()).isFalse();
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testReturnStudentProfile_whenPayloadIsValidButAnotherSagaIsInProgress_shouldReturnStatusConflict() throws Exception {
    final String payload = "{\n" +
      PAYLOAD_STR +
      "  \"staffMemberIDIRGUID\": \"AC335214725219468172589E58000004\",\n" +
      "  \"staffMemberName\": \"om\",\n" +
      "  \"commentContent\": \"please upload recent govt ID.\",\n" +
      "  \"commentTimestamp\": \"2020-06-10T09:52:00\"\n" +
      "}";

    this.repository.save(this.getSaga(payload, "STUDENT_PROFILE_RETURN_SAGA", "STUDENT_PROFILE_SAGA_API", UUID.fromString("ac335214-7252-1946-8172-589e58000004")));
    final var results = this.repository.findAll();
    this.mockMvc.perform(post(URL.BASE_URL + URL.STUDENT_PROFILE_RETURN_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_RETURN_SAGA"))).contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isConflict());
    assertThat(results.isEmpty()).isFalse();
  }

  @Test
  public void test_commentStudentProfile_givenInValidPayload_shouldReturnBadRequest() throws Exception {
    final String payload = "{\n" +
      "  \"staffMemberIDIRGUID\": \"AC335214725219468172589E58000004\",\n" +
      "  \"staffMemberName\": \"om\",\n" +
      "  \"commentContent\": \"please upload recent govt ID.\",\n" +
      "  \"commentTimestamp\": \"2020-06-10T09:52:00\"\n" +
      "}";
    this.mockMvc.perform(post(URL.BASE_URL + URL.STUDENT_PROFILE_COMMENT_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_COMMENT_SAGA"))).contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isBadRequest());
  }


  @Test
  public void test_commentStudentProfile_givenValidPayload_shouldReturnNoContent() throws Exception {
    final String payload = "{\n" +
      "  \"studentProfileRequestID\": \"ac335214-7252-1946-8172-589e58000004\",\n" +
      "  \"commentContent\": \"Hi\",\n" +
      "  \"commentTimestamp\": \"2020-04-18T19:57:00\",\n" +
      "  \"studentProfileRequestStatusCode\": \"SUBSREV\",\n" +
      "  \"createUser\": \"STUDENT_PROFILE\",\n" +
      "  \"updateUser\": \"STUDENT_PROFILE\"\n" +
      "}";
    this.mockMvc.perform(post(URL.BASE_URL + URL.STUDENT_PROFILE_COMMENT_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_COMMENT_SAGA"))).contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isOk());
    val result = this.repository.findAll();
    assertEquals(1, result.size());
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaBySagaID_whenSagaIDIsValid_shouldReturnStatusNoContent() throws Exception {
    final String payload = "{\n" +
      PAYLOAD_STR +
      "  \"staffMemberIDIRGUID\": \"AC335214725219468172589E58000004\",\n" +
      "  \"staffMemberName\": \"om\",\n" +
      "  \"commentContent\": \"please upload recent govt ID.\",\n" +
      "  \"commentTimestamp\": \"2020-06-10T09:52:00\"\n" +
      "}";
    final var entity = this.getSaga(payload, "STUDENT_PROFILE_RETURN_SAGA", "STUDENT_PROFILE_SAGA_API", UUID.fromString("ac335214-7252-1946-8172-589e58000004"));
    this.repository.save(entity);
    this.mockMvc.perform(get(URL.BASE_URL + URL.SAGA_ID, entity.getSagaId()).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_READ_SAGA")))).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.sagaId").value(entity.getSagaId().toString()));
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaEventsBySagaID_whenSagaIDIsValid_shouldReturnStatusOk() throws Exception {
    final File sagEventsFile = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-saga-events.json")).getFile()
    );
    final File sagFile = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-saga.json")).getFile()
    );
    val sagaEvents = Arrays.asList(JsonUtil.objectMapper.readValue(sagEventsFile, SagaEvent[].class));
    val saga = JsonUtil.objectMapper.readValue(sagFile, Saga.class);
    saga.setSagaId(null);
    saga.setSagaCompensated(false);
    saga.setCreateDate(LocalDateTime.now());
    saga.setUpdateDate(LocalDateTime.now());
    this.repository.save(saga);
    for (val sagaEvent : sagaEvents) {
      sagaEvent.setSaga(saga);
      sagaEvent.setCreateDate(LocalDateTime.now());
      sagaEvent.setUpdateDate(LocalDateTime.now());
    }
    this.sagaEventRepository.saveAll(sagaEvents);
    this.mockMvc.perform(get(URL.BASE_URL + URL.SAGA_ID + URL.SAGA_EVENTS, saga.getSagaId()).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_READ_SAGA")))).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(8)));
  }

  private Saga getSaga(final String payload, final String sagaName, final String apiName, final UUID profileRequestId) {
    return Saga
      .builder()
      .payload(payload)
      .sagaName(sagaName)
      .status(STARTED.toString())
      .sagaState(INITIATED.toString())
      .sagaCompensated(false)
      .createDate(LocalDateTime.now())
      .createUser(apiName)
      .updateUser(apiName)
      .updateDate(LocalDateTime.now())
      .profileRequestId(profileRequestId)
      .build();
  }
}
