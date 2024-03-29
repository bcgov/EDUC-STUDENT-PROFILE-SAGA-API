package ca.bc.gov.educ.api.student.profile.saga.controller;

import ca.bc.gov.educ.api.student.profile.saga.BaseSagaApiTest;
import ca.bc.gov.educ.api.student.profile.saga.constants.v1.URL;
import ca.bc.gov.educ.api.student.profile.saga.controller.v1.PenRequestSagaController;
import ca.bc.gov.educ.api.student.profile.saga.exception.SagaRuntimeException;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import ca.bc.gov.educ.api.student.profile.saga.service.SagaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.INITIATED;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.STARTED;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PenRequestSagaControllerTest extends BaseSagaApiTest {

  @Autowired
  SagaRepository repository;
  @Autowired
  SagaService service;
  @Autowired
  PenRequestSagaController controller;
  @Autowired
  private MockMvc mockMvc;

  public static String asJsonString(final Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (final Exception e) {
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  @Test
  public void test_completePENRequest_givenInValidPayload_shouldReturnBadRequest() throws Exception {
    final String payload = "{\n" +
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
    this.mockMvc.perform(
        post(URL.BASE_URL + URL.PEN_REQUEST_COMPLETE_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_COMPLETE_SAGA"))).contentType(MediaType.APPLICATION_JSON)
            .content(payload)).andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  public void test_completePENRequest_givenWrongScope_shouldReturnForbidden() throws Exception {
    final String payload = "{\n" +
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
        "  \"statusUpdateDate\": \"2020-04-17T22:29:00\",\n" +
        "  \"historyActivityCode\": \"PEN\",\n" +
        "  \"identityType\": \"BASIC\"\n" +
        "}";
    this.mockMvc
        .perform(post(URL.BASE_URL + URL.PEN_REQUEST_COMPLETE_SAGA)
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRONG_SCOPE")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andDo(print()).andExpect(status().isForbidden());
  }

  @Test
  public void test_completePENRequest_givenValidPayload_shouldReturnStatusNoContent() throws Exception {
    final String payload = "{\n" +
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
        "  \"statusUpdateDate\": \"2020-04-17T22:29:00\",\n" +
        "  \"historyActivityCode\": \"PEN\",\n" +
        "  \"identityType\": \"BASIC\"\n" +
        "}";
    this.mockMvc.perform(
        post(URL.BASE_URL + URL.PEN_REQUEST_COMPLETE_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_COMPLETE_SAGA"))).contentType(MediaType.APPLICATION_JSON)
            .content(payload)).andDo(print()).andExpect(status().isOk());
    val result = this.repository.findAll();
    assertEquals(1, result.size());
  }

  @Test
  public void test_rejectPENRequest_givenValidPayload_shouldReturnStatusNoContent() throws Exception {
    final String payload = "{\n" +
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
    this.mockMvc.perform(
        post(URL.BASE_URL + URL.PEN_REQUEST_REJECT_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_REJECT_SAGA"))).contentType(MediaType.APPLICATION_JSON)
            .content(payload)).andDo(print()).andExpect(status().isOk());
    val result = this.repository.findAll();
    assertEquals(1, result.size());
  }

  @Test
  public void test_returnPENRequest_givenValidPayload_shouldReturnStatusOK() throws Exception {
    final String payload = "{\"penRetrievalReqCommentID\":null,\"penRetrievalRequestID\":\"0a611585-75f0-1cee-8176-06d5ee9a0053\"," + "  \"createUser\": \"marco\",\n" +
        "  \"updateUser\": \"marco\",\n"
        + "\"staffMemberIDIRGUID\":\"27D2034327EB442FA1073B0C6F2F3F7D\",\"staffMemberName\":\"PENREG1\",\"penRequestStatusCode\":\"RETURNED\",\"sagaCompensated\":\"PENREG1\",\"updateUser\":\"PENREG1\",\"email\":\"penemail@mailsac.com\",\"identityType\":\"BASIC\",\"reviewer\":\"PENREG1\",\"commentContent\":\"Please provide an Identification Document\",\"commentTimestamp\":\"2020-11-26T15:16:20\"}";
    this.mockMvc.perform(
        post(URL.BASE_URL + URL.PEN_REQUEST_RETURN_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_RETURN_SAGA"))).contentType(MediaType.APPLICATION_JSON)
            .content(payload)).andDo(print()).andExpect(status().isOk());
    val result = this.repository.findAll();
    assertEquals(1, result.size());
  }

  @Test
  public void test_rejectPENRequest_givenValidPayloadButOtherSagaInFlight_shouldReturnStatusConflict() throws Exception {
    final String payload = "{\n" +
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
    this.repository.save(this.getSaga(payload, "PEN_REQUEST_REJECT_SAGA", UUID.fromString("ac334a38-715f-1340-8171-607a59d0000a")));
    this.mockMvc.perform(
        post(URL.BASE_URL + URL.PEN_REQUEST_REJECT_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_REJECT_SAGA"))).contentType(MediaType.APPLICATION_JSON)
            .content(payload)).andDo(print()).andExpect(status().isConflict());
    val result = this.repository.findAll();
    assertEquals(1, result.size());
  }

  @Test
  public void test_commentPENRequest_givenInValidPayload_shouldReturnBadRequest() throws Exception {
    final String payload = "{\n" +
        "  \"staffMemberIDIRGUID\": \"2827808f-dfde-4b9b-835c-10cf0130261c\",\n" +
        "  \"staffMemberName\": \"SHFOORD\",\n" +
        "  \"commentContent\": \"Hi\",\n" +
        "  \"commentTimestamp\": \"2020-04-18T19:57:00\",\n" +
        "  \"penRequestStatusCode\": \"SUBSREV\",\n" +
        "  \"createUser\": \"STAFF_ADMIN\",\n" +
        "  \"updateUser\": \"STAFF_ADMIN\"\n" +
        "}";
    this.mockMvc.perform(
        post(URL.BASE_URL + URL.PEN_REQUEST_COMMENT_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_COMMENT_SAGA"))).contentType(MediaType.APPLICATION_JSON)
            .content(payload)).andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  public void test_commentPENRequest_givenValidPayload_shouldReturnNoContent() throws Exception {
    final String payload = "{\n" +
        "  \"penRetrievalRequestID\": \"ac334a38-715f-1340-8171-607a59d0000a\",\n" +
        "  \"staffMemberIDIRGUID\": \"2827808f-dfde-4b9b-835c-10cf0130261c\",\n" +
        "  \"staffMemberName\": \"SHFOORD\",\n" +
        "  \"commentContent\": \"Hi\",\n" +
        "  \"commentTimestamp\": \"2020-04-18T19:57:00\",\n" +
        "  \"penRequestStatusCode\": \"SUBSREV\",\n" +
        "  \"createUser\": \"STAFF_ADMIN\",\n" +
        "  \"updateUser\": \"STAFF_ADMIN\"\n" +
        "}";
    this.mockMvc.perform(
        post(URL.BASE_URL + URL.PEN_REQUEST_COMMENT_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_COMMENT_SAGA"))).contentType(MediaType.APPLICATION_JSON)
            .content(payload)).andDo(print()).andExpect(status().isOk());
    val result = this.repository.findAll();
    assertEquals(1, result.size());
  }

  @Test
  public void test_unlinkPENRequest_givenInValidPayload_shouldReturnBadRequest() throws Exception {
    final var payload = "{\n" +

        "}";
    this.mockMvc.perform(
            post(URL.BASE_URL + URL.PEN_REQUEST_UNLINK_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_UNLINK_SAGA"))).contentType(MediaType.APPLICATION_JSON)
                .content(payload)).andDo(print())
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.subErrors", hasSize(6)));
  }

  @Test
  public void test_unlinkPENRequest_givenValidPayload_shouldReturnOK() throws Exception {
    final var payload = "{\n" +
        "  \"penRetrievalRequestID\": \"ac334a38-715f-1340-8171-607a59d0000a\",\n" +
        "  \"digitalID\": \"2827808f-dfde-4b9b-835c-10cf0130261c\",\n" +
        "  \"reviewer\": \"SHFOORD\",\n" +
        "  \"penRequestStatusCode\": \"SUBSREV\",\n" +
        "  \"createUser\": \"STAFF_ADMIN\",\n" +
        "  \"updateUser\": \"STAFF_ADMIN\"\n" +
        "}";
    this.mockMvc.perform(
        post(URL.BASE_URL + URL.PEN_REQUEST_UNLINK_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_UNLINK_SAGA"))).contentType(MediaType.APPLICATION_JSON)
            .content(payload)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$", notNullValue()));
    val result = this.repository.findAll();
    assertEquals(1, result.size());
  }

  @Test
  public void test_unlinkPENRequest_givenValidPayloadAndCompleteSagaInProgress_shouldForceStopCompleteSagaAndReturnOK() throws Exception {
    final var payload = "{\n" +
        "  \"penRetrievalRequestID\": \"ac334a38-715f-1340-8171-607a59d0000a\",\n" +
        "  \"digitalID\": \"2827808f-dfde-4b9b-835c-10cf0130261c\",\n" +
        "  \"reviewer\": \"SHFOORD\",\n" +
        "  \"penRequestStatusCode\": \"SUBSREV\",\n" +
        "  \"createUser\": \"STAFF_ADMIN\",\n" +
        "  \"updateUser\": \"STAFF_ADMIN\"\n" +
        "}";
    final var saga = this.getSaga(payload, "PEN_REQUEST_COMPLETE_SAGA", UUID.fromString("ac334a38-715f-1340-8171-607a59d0000a"));
    this.repository.save(saga);
    this.mockMvc.perform(
        post(URL.BASE_URL + URL.PEN_REQUEST_UNLINK_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "PEN_REQUEST_UNLINK_SAGA"))).contentType(MediaType.APPLICATION_JSON)
            .content(payload)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$", notNullValue()));
    val result = this.repository.findAll();
    assertEquals(2, result.size());
    final var updatedSaga = this.repository.findById(saga.getSagaId());
    assertTrue(updatedSaga.isPresent());
    assertEquals("FORCE_STOPPED", updatedSaga.get().getStatus());
  }

  private Saga getSaga(final String payload, final String sagaName, final UUID penRequestId) {
    return Saga
        .builder()
        .payload(payload)
        .sagaName(sagaName)
        .status(STARTED.toString())
        .sagaState(INITIATED.toString())
        .sagaCompensated(false)
        .createDate(LocalDateTime.now())
        .createUser("STUDENT_PROFILE_SAGA_API")
        .updateUser("STUDENT_PROFILE_SAGA_API")
        .updateDate(LocalDateTime.now())
        .penRequestId(penRequestId)
        .build();
  }
}
