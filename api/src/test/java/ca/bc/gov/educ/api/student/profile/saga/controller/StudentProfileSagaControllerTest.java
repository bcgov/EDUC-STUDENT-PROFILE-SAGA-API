package ca.bc.gov.educ.api.student.profile.saga.controller;

import ca.bc.gov.educ.api.student.profile.saga.exception.RestExceptionHandler;
import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaEventRepository;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import ca.bc.gov.educ.api.student.profile.saga.support.WithMockOAuth2Scope;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.INITIATED;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class StudentProfileSagaControllerTest {
  public static final String PAYLOAD_STR = "  \"studentProfileRequestID\": \"ac335214-7252-1946-8172-589e58000004\",\n" +
      "  \"createUser\": \"om\",\n" +
      "  \"updateUser\": \"om\",\n" +
      "  \"email\": \"omprkshmishra@gmail.com\",\n" +
      "  \"identityType\": \"BASIC\",\n";
  @Autowired
  private StudentProfileSagaController controller;
  @Autowired
  private SagaRepository repository;
  @Autowired
  private SagaEventRepository sagaEventRepository;
  private MockMvc mockMvc;


  @Before
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new RestExceptionHandler()).build();
  }

  @After
  public void tearDown() {
    sagaEventRepository.deleteAll();
    repository.deleteAll();
  }


  @Test
  @WithMockOAuth2Scope(scope = "STUDENT_PROFILE_REJECT_SAGA")
  @SuppressWarnings("java:S100")
  public void testRejectStudentProfile_whenRejectionReasonIsNull_shouldReturnStatusBadRequest() throws Exception {
    String payload = "{\n" +
        PAYLOAD_STR +
        "}";
    this.mockMvc.perform(post("/student-profile-reject-saga").contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isBadRequest());
    var results = repository.findAll();
    assertThat (results.isEmpty()).isTrue();
  }

  @Test
  @WithMockOAuth2Scope(scope = "STUDENT_PROFILE_REJECT_SAGA")
  @SuppressWarnings("java:S100")
  public void testRejectStudentProfile_whenPayloadIsValid_shouldReturnStatusOk() throws Exception {
    String payload = "{\n" +
        PAYLOAD_STR +
        "  \"rejectionReason\": \"rejected\"\n" +
        "}";
    this.mockMvc.perform(post("/student-profile-reject-saga").contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isOk());
    var results = repository.findAll();
    assertThat (results.isEmpty()).isFalse();
  }

  @Test
  @WithMockOAuth2Scope(scope = "STUDENT_PROFILE_RETURN_SAGA")
  @SuppressWarnings("java:S100")
  public void testReturnStudentProfile_whenCommentContentIsNull_shouldReturnStatusBadRequest() throws Exception {
    String payload = "{\n" +
        PAYLOAD_STR +
        "}";
    this.mockMvc.perform(post("/student-profile-return-saga").contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isBadRequest());
    var results = repository.findAll();
    assertThat (results.isEmpty()).isTrue();
  }

  @Test
  @WithMockOAuth2Scope(scope = "STUDENT_PROFILE_RETURN_SAGA")
  @SuppressWarnings("java:S100")
  public void testReturnStudentProfile_whenPayloadIsValid_shouldReturnStatusOk() throws Exception {
    String payload = "{\n" +
        PAYLOAD_STR +
        "  \"staffMemberIDIRGUID\": \"AC335214725219468172589E58000004\",\n" +
        "  \"staffMemberName\": \"om\",\n" +
        "  \"commentContent\": \"please upload recent govt ID.\",\n" +
        "  \"commentTimestamp\": \"2020-06-10T09:52:00\"\n" +
        "}";
    this.mockMvc.perform(post("/student-profile-return-saga").contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isOk());
    var results = repository.findAll();
    assertThat (results.isEmpty()).isFalse();
  }
  @Test
  @WithMockOAuth2Scope(scope = "STUDENT_PROFILE_RETURN_SAGA")
  @SuppressWarnings("java:S100")
  public void testReturnStudentProfile_whenPayloadIsValidButAnotherSagaIsInProgress_shouldReturnStatusConflict() throws Exception {
    String payload = "{\n" +
        PAYLOAD_STR +
        "  \"staffMemberIDIRGUID\": \"AC335214725219468172589E58000004\",\n" +
        "  \"staffMemberName\": \"om\",\n" +
        "  \"commentContent\": \"please upload recent govt ID.\",\n" +
        "  \"commentTimestamp\": \"2020-06-10T09:52:00\"\n" +
        "}";

    repository.save(getSaga(payload,"STUDENT_PROFILE_RETURN_SAGA","STUDENT_PROFILE_SAGA_API", UUID.fromString("ac335214-7252-1946-8172-589e58000004")));
    var results = repository.findAll();
    this.mockMvc.perform(post("/student-profile-return-saga").contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isConflict());
    assertThat (results.isEmpty()).isFalse();
  }
  @Test
  @WithMockOAuth2Scope(scope = "STUDENT_PROFILE_COMMENT_SAGA")
  public void test_commentStudentProfile_givenInValidPayload_shouldReturnBadRequest() throws Exception {
    String payload = "{\n" +
            "  \"staffMemberIDIRGUID\": \"AC335214725219468172589E58000004\",\n" +
            "  \"staffMemberName\": \"om\",\n" +
            "  \"commentContent\": \"please upload recent govt ID.\",\n" +
            "  \"commentTimestamp\": \"2020-06-10T09:52:00\"\n" +
            "}";
    this.mockMvc.perform(post("/student-profile-comment-saga").contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isBadRequest());
  }



  @Test
  @WithMockOAuth2Scope(scope = "STUDENT_PROFILE_COMMENT_SAGA")
  public void test_commentStudentProfile_givenValidPayload_shouldReturnNoContent() throws Exception {
    String payload = "{\n" +
            "  \"studentProfileRequestID\": \"ac335214-7252-1946-8172-589e58000004\",\n" +
            "  \"commentContent\": \"Hi\",\n" +
            "  \"commentTimestamp\": \"2020-04-18T19:57:00\",\n" +
            "  \"studentProfileRequestStatusCode\": \"SUBSREV\",\n" +
            "  \"createUser\": \"STUDENT_PROFILE\",\n" +
            "  \"updateUser\": \"STUDENT_PROFILE\"\n" +
            "}";
    this.mockMvc.perform(post("/student-profile-comment-saga").contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isOk());
    val result = repository.findAll();
    assertEquals(1, result.size());
  }

  @Test
  @WithMockOAuth2Scope(scope = "READ_SAGA")
  @SuppressWarnings("java:S100")
  public void testGetSagaBySagaID_whenSagaIDIsValid_shouldReturnStatusNoContent() throws Exception {
    String payload = "{\n" +
        PAYLOAD_STR +
        "  \"staffMemberIDIRGUID\": \"AC335214725219468172589E58000004\",\n" +
        "  \"staffMemberName\": \"om\",\n" +
        "  \"commentContent\": \"please upload recent govt ID.\",\n" +
        "  \"commentTimestamp\": \"2020-06-10T09:52:00\"\n" +
        "}";
    var entity = getSaga(payload,"STUDENT_PROFILE_RETURN_SAGA","STUDENT_PROFILE_SAGA_API", UUID.fromString("ac335214-7252-1946-8172-589e58000004"));
    repository.save(entity);
    this.mockMvc.perform(get("/" + entity.getSagaId())).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.sagaId").value(entity.getSagaId().toString()));
  }


  private Saga getSaga(String payload, String sagaName, String apiName, UUID profileRequestId) {
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
