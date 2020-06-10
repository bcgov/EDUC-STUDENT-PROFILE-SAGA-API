package ca.bc.gov.educ.api.student.profile.saga.controller;

import ca.bc.gov.educ.api.student.profile.saga.exception.RestExceptionHandler;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaEventRepository;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import ca.bc.gov.educ.api.student.profile.saga.support.WithMockOAuth2Scope;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
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
  public void testRejectStudentProfile_whenPayloadIsValid_shouldReturnStatusNoContent() throws Exception {
    String payload = "{\n" +
        PAYLOAD_STR +
        "  \"rejectionReason\": \"rejected\"\n" +
        "}";
    this.mockMvc.perform(post("/student-profile-reject-saga").contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isNoContent());
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
  public void testReturnStudentProfile_whenPayloadIsValid_shouldReturnStatusNoContent() throws Exception {
    String payload = "{\n" +
        PAYLOAD_STR +
        "  \"staffMemberIDIRGUID\": \"AC335214725219468172589E58000004\",\n" +
        "  \"staffMemberName\": \"om\",\n" +
        "  \"commentContent\": \"please upload recent govt ID.\",\n" +
        "  \"commentTimestamp\": \"2020-06-10T09:52:00\"\n" +
        "}";
    this.mockMvc.perform(post("/student-profile-return-saga").contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isNoContent());
    var results = repository.findAll();
    assertThat (results.isEmpty()).isFalse();
  }
}