package ca.bc.gov.educ.api.student.profile.saga.controller;

import ca.bc.gov.educ.api.student.profile.saga.exception.RestExceptionHandler;
import ca.bc.gov.educ.api.student.profile.saga.exception.SagaRuntimeException;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import ca.bc.gov.educ.api.student.profile.saga.support.WithMockOAuth2Scope;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings("java:S100")
public class PenRequestSagaControllerTest {

  private MockMvc mockMvc;

  @Autowired
  SagaRepository repository;

  @Autowired
  SagaService service;

  @Autowired
  PenRequestSagaController controller;

  @Before
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new RestExceptionHandler()).build();
  }

  @After
  public void after() {
    repository.deleteAll();
  }

  @Test
  @WithMockOAuth2Scope(scope = "PEN_REQUEST_COMPLETE_SAGA")
  public void test_completePENRequest_givenInValidPayload_shouldReturnBadRequest() throws Exception {
    String payload = "{\n" +
            "  \"digitalID\": \"ac330603-715f-12b6-8171-6079a6270005\",\n" +
            "  \"penRequestID\": \"ac334a38-715f-1340-8171-607a59d0000a\",\n" +
            "  \"legalFirstName\": \"om\",\n" +
            "  \"legalMiddleNames\": \"mishra\",\n" +
            "  \"legalLastName\": \"mishra\",\n" +
            "  \"dob\": \"2000-01-01\",\n" +
            "  \"sexCode\": \"M\",\n" +
            "  \"genderCode\": \"M\",\n" +
            "  \"email\": \"om@gmail.com\",\n" +
            "  \"createUser\": \"om\",\n" +
            "  \"updateUser\": \"om\",\n" +
            "  \"bcscAutoMatchOutcome\": \"ONEMATCH\",\n" +
            "  \"penRequestStatusCode\": \"MANUAL\",\n" +
            "  \"statusUpdateDate\": \"2020-04-17T22:29:00\"\n" +
            "}";
    this.mockMvc.perform(post("/pen-request-complete-saga").contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockOAuth2Scope(scope = "PEN_REQUEST_COMPLETE_SAGA")
  public void test_completePENRequest_givenValidPayload_shouldReturnStatusNoContent() throws Exception {
    String payload = "{\n" +
            "  \"digitalID\": \"ac330603-715f-12b6-8171-6079a6270005\",\n" +
            "  \"penRequestID\": \"ac334a38-715f-1340-8171-607a59d0000a\",\n" +
            "  \"pen\": \"123456789\",\n" +
            "  \"legalFirstName\": \"om\",\n" +
            "  \"legalMiddleNames\": \"mishra\",\n" +
            "  \"legalLastName\": \"mishra\",\n" +
            "  \"dob\": \"2000-01-01\",\n" +
            "  \"sexCode\": \"M\",\n" +
            "  \"genderCode\": \"M\",\n" +
            "  \"email\": \"om@gmail.com\",\n" +
            "  \"createUser\": \"om\",\n" +
            "  \"updateUser\": \"om\",\n" +
            "  \"bcscAutoMatchOutcome\": \"ONEMATCH\",\n" +
            "  \"penRequestStatusCode\": \"MANUAL\",\n" +
            "  \"statusUpdateDate\": \"2020-04-17T22:29:00\"\n" +
            "}";
    this.mockMvc.perform(post("/pen-request-complete-saga").contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isOk());
    val result = repository.findAll();
    assertEquals(1, result.size());
  }

  @Test
  @WithMockOAuth2Scope(scope = "PEN_REQUEST_REJECT_SAGA")
  public void test_rejectPENRequest_givenValidPayload_shouldReturnStatusNoContent() throws Exception {
    String payload = "{\n" +
            "  \"digitalID\": \"ac330603-715f-12b6-8171-6079a6270005\",\n" +
            "  \"penRetrievalRequestID\": \"ac334a38-715f-1340-8171-607a59d0000a\",\n" +
            "  \"pen\": \"123456789\",\n" +
            "  \"legalFirstName\": \"eric\",\n" +
            "  \"legalMiddleNames\": \"mishra\",\n" +
            "  \"legalLastName\": \"sermon\",\n" +
            "  \"dob\": \"2000-01-01\",\n" +
            "  \"sexCode\": \"M\",\n" +
            "  \"genderCode\": \"M\",\n" +
            "  \"email\": \"someplace@gmail.com\",\n" +
            "  \"createUser\": \"marco\",\n" +
            "  \"updateUser\": \"marco\",\n" +
            "  \"penRequestStatusCode\": \"REJECTED\",\n" +
            "  \"identityType\": \"BASIC\",\n" +
            "  \"rejectionReason\": \"Can't find you\",\n" +
            "  \"statusUpdateDate\": \"2020-04-17T22:29:00\"\n" +
            "}";
    this.mockMvc.perform(post("/pen-request-reject-saga").contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isOk());
    val result = repository.findAll();
    assertEquals(1, result.size());
  }


  @Test
  @WithMockOAuth2Scope(scope = "PEN_REQUEST_COMMENT_SAGA")
  public void test_commentPENRequest_givenInValidPayload_shouldReturnBadRequest() throws Exception {
    String payload = "{\n" +
            "  \"staffMemberIDIRGUID\": \"2827808f-dfde-4b9b-835c-10cf0130261c\",\n" +
            "  \"staffMemberName\": \"SHFOORD\",\n" +
            "  \"commentContent\": \"Hi\",\n" +
            "  \"commentTimestamp\": \"2020-04-18T19:57:00\",\n" +
            "  \"penRequestStatusCode\": \"SUBSREV\",\n" +
            "  \"createUser\": \"STAFF_ADMIN\",\n" +
            "  \"updateUser\": \"STAFF_ADMIN\"\n" +
            "}";
    this.mockMvc.perform(post("/pen-request-comment-saga").contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockOAuth2Scope(scope = "PEN_REQUEST_COMMENT_SAGA")
  public void test_commentPENRequest_givenValidPayload_shouldReturnNoContent() throws Exception {
    String payload = "{\n" +
            "  \"penRetrievalRequestID\": \"ac334a38-715f-1340-8171-607a59d0000a\",\n" +
            "  \"staffMemberIDIRGUID\": \"2827808f-dfde-4b9b-835c-10cf0130261c\",\n" +
            "  \"staffMemberName\": \"SHFOORD\",\n" +
            "  \"commentContent\": \"Hi\",\n" +
            "  \"commentTimestamp\": \"2020-04-18T19:57:00\",\n" +
            "  \"penRequestStatusCode\": \"SUBSREV\",\n" +
            "  \"createUser\": \"STAFF_ADMIN\",\n" +
            "  \"updateUser\": \"STAFF_ADMIN\"\n" +
            "}";
    this.mockMvc.perform(post("/pen-request-comment-saga").contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isOk());
    val result = repository.findAll();
    assertEquals(1, result.size());
  }
  @Test
  @WithMockOAuth2Scope(scope = "PEN_REQUEST_UNLINK_SAGA")
  public void test_unlinkPENRequest_givenInValidPayload_shouldReturnBadRequest() throws Exception {
    var payload = "{\n" +

        "}";
    this.mockMvc.perform(post("/pen-request-unlink-saga").contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print())
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.subErrors", hasSize(6)));
  }

  @Test
  @WithMockOAuth2Scope(scope = "PEN_REQUEST_UNLINK_SAGA")
  public void test_commentPENRequest_givenValidPayload_shouldReturnOK() throws Exception {
    var payload = "{\n" +
        "  \"penRetrievalRequestID\": \"ac334a38-715f-1340-8171-607a59d0000a\",\n" +
        "  \"digitalID\": \"2827808f-dfde-4b9b-835c-10cf0130261c\",\n" +
        "  \"reviewer\": \"SHFOORD\",\n" +
        "  \"penRequestStatusCode\": \"SUBSREV\",\n" +
        "  \"createUser\": \"STAFF_ADMIN\",\n" +
        "  \"updateUser\": \"STAFF_ADMIN\"\n" +
        "}";
    this.mockMvc.perform(post("/pen-request-unlink-saga").contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$", notNullValue()));
    val result = repository.findAll();
    assertEquals(1, result.size());
  }
  public static String asJsonString(final Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }
}
