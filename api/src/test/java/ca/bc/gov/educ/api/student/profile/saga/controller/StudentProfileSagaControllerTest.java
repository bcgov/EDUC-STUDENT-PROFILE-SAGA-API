package ca.bc.gov.educ.api.student.profile.saga.controller;

import ca.bc.gov.educ.api.student.profile.saga.BaseSagaApiTest;
import ca.bc.gov.educ.api.student.profile.saga.constants.v1.URL;
import ca.bc.gov.educ.api.student.profile.saga.filter.FilterOperation;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.SagaEvent;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaEventRepository;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import ca.bc.gov.educ.api.student.profile.saga.struct.Condition;
import ca.bc.gov.educ.api.student.profile.saga.struct.Search;
import ca.bc.gov.educ.api.student.profile.saga.struct.SearchCriteria;
import ca.bc.gov.educ.api.student.profile.saga.struct.ValueType;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

import static ca.bc.gov.educ.api.student.profile.saga.constants.EventType.INITIATED;
import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaStatusEnum.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
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
  public void test_completeStudentProfile_givenValidPayload_shouldReturnNoContent() throws Exception {
    final String payload = "{\"studentProfileRequestID\":\"0a6112e6-75f0-1cae-8176-057a3bbe0008\",\"createUser\":\"RREDDY\",\"updateUser\":\"RREDDY\",\"reviewer\":\"RREDDY\",\"email\":\"penemail@mailsac.com\",\"identityType\":\"BASIC\",\"staffMemberIDIRGUID\":null,\"staffMemberName\":null,\"studentID\":\"ac334ce6-7600-1bc1-8176-004c3fdb0003\",\"digitalID\":\"0a612708-7602-1aea-8176-05774b800006\",\"pen\":\"200004562\",\"legalFirstName\":\"TESTER\",\"legalMiddleNames\":\"PEN\",\"legalLastName\":\"AUTOMATION\",\"dob\":\"1999-12-01\",\"sexCode\":\"M\",\"genderCode\":\"M\",\"usualFirstName\":null,\"usualMiddleNames\":null,\"usualLastName\":null,\"deceasedDate\":null,\"emailVerified\":\"N\",\"completeComment\":\"Your update my Pen request is approved\",\"postalCode\":null,\"gradeCode\":null,\"mincode\":null,\"localID\":null,\"historyActivityCode\":\"PEN\"}";
    this.mockMvc.perform(post(URL.BASE_URL + URL.STUDENT_PROFILE_COMPLETE_SAGA).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_COMPLETE_SAGA"))).contentType(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isOk());
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

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaPaginated_givenNoSearchCriteria_shouldReturnStatusOk() throws Exception {
    final File sagasFile = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-multiple-saga.json")).getFile()
    );
    val sagas = Arrays.asList(JsonUtil.objectMapper.readValue(sagasFile, Saga[].class));
    for (val saga : sagas) {
      saga.setSagaId(null);
      saga.setSagaCompensated(false);
      saga.setCreateDate(LocalDateTime.now());
      saga.setUpdateDate(LocalDateTime.now());
    }
    this.repository.saveAll(sagas);
    this.mockMvc.perform(get(URL.BASE_URL + URL.PAGINATED).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_READ_SAGA")))).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(10)));
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaPaginated_givenSearchCriteria_shouldReturnStatusOk() throws Exception {
    final File sagasFile = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-multiple-saga.json")).getFile()
    );
    val sagas = Arrays.asList(JsonUtil.objectMapper.readValue(sagasFile, Saga[].class));
    for (val saga : sagas) {
      saga.setSagaId(null);
      saga.setSagaCompensated(false);
      saga.setCreateDate(LocalDateTime.now());
      saga.setUpdateDate(LocalDateTime.now());
    }
    this.repository.saveAll(sagas);
    final SearchCriteria criteria = SearchCriteria.builder().key("sagaName").operation(FilterOperation.EQUAL).value("PEN_REQUEST_REJECT_SAGA").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    this.mockMvc.perform(get(URL.BASE_URL + URL.PAGINATED).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_READ_SAGA"))).param("searchCriteriaList", criteriaJSON)
      .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(10))).andExpect(jsonPath("$.totalElements", is(15)));
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaPaginated_givenSearchCriteria2_shouldReturnStatusOk() throws Exception {
    final File sagasFile = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-multiple-saga.json")).getFile()
    );
    val sagas = Arrays.asList(JsonUtil.objectMapper.readValue(sagasFile, Saga[].class));
    for (val saga : sagas) {
      saga.setSagaId(null);
      saga.setSagaCompensated(false);
      saga.setCreateDate(LocalDateTime.now());
      saga.setUpdateDate(LocalDateTime.now());
    }
    this.repository.saveAll(sagas);
    final SearchCriteria criteria = SearchCriteria.builder().key("status").operation(FilterOperation.EQUAL).value(null).valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    this.mockMvc.perform(get(URL.BASE_URL + URL.PAGINATED).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_READ_SAGA"))).param("searchCriteriaList", criteriaJSON)
      .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0))).andExpect(jsonPath("$.totalElements", is(0)));
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaPaginated_givenSearchCriteria3_shouldReturnStatusOk() throws Exception {
    final File sagasFile = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-multiple-saga.json")).getFile()
    );
    val sagas = Arrays.asList(JsonUtil.objectMapper.readValue(sagasFile, Saga[].class));
    for (val saga : sagas) {
      saga.setSagaId(null);
      saga.setSagaCompensated(false);
      saga.setCreateDate(LocalDateTime.now());
      saga.setUpdateDate(LocalDateTime.now());
    }
    this.repository.saveAll(sagas);
    final SearchCriteria criteria = SearchCriteria.builder().key("penRequestId").operation(FilterOperation.EQUAL).value("0a61140c-7644-1379-8176-4e15129a0001").valueType(ValueType.UUID).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    this.mockMvc.perform(get(URL.BASE_URL + URL.PAGINATED).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_READ_SAGA"))).param("searchCriteriaList", criteriaJSON)
      .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.totalElements", is(1)));
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaPaginated_givenSearchCriteria4_shouldReturnStatusOk() throws Exception {
    final File sagasFile = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-multiple-saga.json")).getFile()
    );
    val sagas = Arrays.asList(JsonUtil.objectMapper.readValue(sagasFile, Saga[].class));
    for (val saga : sagas) {
      saga.setSagaId(null);
      saga.setSagaCompensated(false);
      saga.setCreateDate(LocalDateTime.now());
      saga.setUpdateDate(LocalDateTime.now());
    }
    this.repository.saveAll(sagas);
    final SearchCriteria criteria = SearchCriteria.builder().key("penRequestId").operation(FilterOperation.EQUAL).value("0a61140c-7644-1379-8176-4e15129a0001").valueType(ValueType.UUID).build();
    final SearchCriteria criteria2 = SearchCriteria.builder().condition(Condition.AND).key("status").operation(FilterOperation.EQUAL).value("COMPLETED").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    this.mockMvc.perform(get(URL.BASE_URL + URL.PAGINATED).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_READ_SAGA"))).param("searchCriteriaList", criteriaJSON)
      .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.totalElements", is(1)));
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaPaginated_givenSearchCriteria5_shouldReturnStatusOk() throws Exception {
    final File sagasFile = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-multiple-saga.json")).getFile()
    );
    val sagas = Arrays.asList(JsonUtil.objectMapper.readValue(sagasFile, Saga[].class));
    for (val saga : sagas) {
      saga.setSagaId(null);
      saga.setSagaCompensated(false);
      saga.setCreateDate(LocalDateTime.now());
      saga.setUpdateDate(LocalDateTime.now());
    }
    this.repository.saveAll(sagas);
    final SearchCriteria criteria = SearchCriteria.builder().key("penRequestId").operation(FilterOperation.EQUAL).value("0a61140c-7644-1379-8176-4e15129a0001").valueType(ValueType.UUID).build();
    final SearchCriteria criteria2 = SearchCriteria.builder().condition(Condition.OR).key("status").operation(FilterOperation.EQUAL).value("COMPLETED").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    this.mockMvc.perform(get(URL.BASE_URL + URL.PAGINATED).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_READ_SAGA"))).param("searchCriteriaList", criteriaJSON)
      .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(10))).andExpect(jsonPath("$.totalElements", is(91)));
  }


  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaPaginated_givenSearchCriteria6_shouldReturnStatusOk() throws Exception {
    final File sagasFile = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-multiple-saga.json")).getFile()
    );
    val sagas = Arrays.asList(JsonUtil.objectMapper.readValue(sagasFile, Saga[].class));
    for (val saga : sagas) {
      saga.setSagaId(null);
      saga.setSagaCompensated(false);
      saga.setCreateDate(LocalDateTime.now());
      saga.setUpdateDate(LocalDateTime.now());
    }
    this.repository.saveAll(sagas);
    final SearchCriteria criteria = SearchCriteria.builder().key("createDate").operation(FilterOperation.GREATER_THAN).value("2000-01-01T00:00:00").valueType(ValueType.DATE_TIME).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    this.mockMvc.perform(get(URL.BASE_URL + URL.PAGINATED).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_READ_SAGA"))).param("searchCriteriaList", criteriaJSON)
      .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(10))).andExpect(jsonPath("$.totalElements", is(93)));
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaPaginated_givenSearchCriteria7_shouldReturnStatusOk() throws Exception {
    final File sagasFile = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-multiple-saga.json")).getFile()
    );
    val sagas = Arrays.asList(JsonUtil.objectMapper.readValue(sagasFile, Saga[].class));
    for (val saga : sagas) {
      saga.setSagaId(null);
      saga.setSagaCompensated(false);
      saga.setCreateDate(LocalDateTime.now());
      saga.setUpdateDate(LocalDateTime.now());
    }
    this.repository.saveAll(sagas);
    final SearchCriteria criteria = SearchCriteria.builder().key("penRequestId").operation(FilterOperation.EQUAL).value("0a61140c-7644-1379-8176-4e15129a0001").valueType(ValueType.UUID).build();
    final SearchCriteria criteria2 = SearchCriteria.builder().condition(Condition.OR).key("status").operation(FilterOperation.EQUAL).value("COMPLETED").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);

    final SearchCriteria criteria3 = SearchCriteria.builder().key("createDate").operation(FilterOperation.GREATER_THAN).value("2000-01-01T00:00:00").valueType(ValueType.DATE_TIME).build();
    final SearchCriteria criteria4 = SearchCriteria.builder().condition(Condition.AND).key("status").operation(FilterOperation.EQUAL).value("COMPLETED").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList2 = new ArrayList<>();
    criteriaList2.add(criteria3);
    criteriaList2.add(criteria4);

    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    searches.add(Search.builder().condition(Condition.OR).searchCriteriaList(criteriaList2).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    final String sort = "{\"createDate\":\"ASC\",\"status\":\"DESC\"}";
    this.mockMvc.perform(get(URL.BASE_URL + URL.PAGINATED).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_READ_SAGA"))).param("searchCriteriaList", criteriaJSON).param("sort", sort)
      .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(10))).andExpect(jsonPath("$.totalElements", is(91)));
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaPaginated_givenSearchCriteria8_shouldReturnStatus400() throws Exception {
    final File sagasFile = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-multiple-saga.json")).getFile()
    );
    val sagas = Arrays.asList(JsonUtil.objectMapper.readValue(sagasFile, Saga[].class));
    for (val saga : sagas) {
      saga.setSagaId(null);
      saga.setSagaCompensated(false);
      saga.setCreateDate(LocalDateTime.now());
      saga.setUpdateDate(LocalDateTime.now());
    }
    this.repository.saveAll(sagas);
    final SearchCriteria criteria = SearchCriteria.builder().key("penRequestId").operation(FilterOperation.EQUAL).value("0a61140c-7644-1379-8176-4e15129a0001").valueType(ValueType.UUID).build();
    final SearchCriteria criteria2 = SearchCriteria.builder().condition(Condition.OR).key("status").operation(FilterOperation.EQUAL).value("COMPLETED").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);

    final String invalidSort = "{\"legal\"}";

    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    searches.add(Search.builder().condition(Condition.OR).searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    this.mockMvc.perform(get(URL.BASE_URL + URL.PAGINATED).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_READ_SAGA"))).param("searchCriteriaList", criteriaJSON)
      .param("sort", invalidSort)
      .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isBadRequest());
  }


  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaPaginated_givenSearchCriteria9_shouldReturnStatus400() throws Exception {
    this.mockMvc.perform(get(URL.BASE_URL + URL.PAGINATED).with(jwt().jwt((jwt) -> jwt.claim("scope", "STUDENT_PROFILE_READ_SAGA"))).param("searchCriteriaList", "junk")
      .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isBadRequest());
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
