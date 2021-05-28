package ca.bc.gov.educ.api.student.profile.saga.service;

import ca.bc.gov.educ.api.student.profile.saga.exception.SagaRuntimeException;
import ca.bc.gov.educ.api.student.profile.saga.filter.FilterOperation;
import ca.bc.gov.educ.api.student.profile.saga.filter.SagaFilterSpecs;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import ca.bc.gov.educ.api.student.profile.saga.struct.Condition;
import ca.bc.gov.educ.api.student.profile.saga.struct.Search;
import ca.bc.gov.educ.api.student.profile.saga.struct.SearchCriteria;
import ca.bc.gov.educ.api.student.profile.saga.struct.ValueType;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SagaSearchService {
  private final SagaFilterSpecs sagaFilterSpecs;

  public SagaSearchService(final SagaFilterSpecs sagaFilterSpecs) {
    this.sagaFilterSpecs = sagaFilterSpecs;
  }

  /**
   * Gets specifications.
   *
   * @param studentSpecs the pen reg batch specs
   * @param i            the
   * @param search       the search
   * @return the specifications
   */
  public Specification<Saga> getSpecifications(Specification<Saga> studentSpecs, final int i, final Search search) {
    if (i == 0) {
      studentSpecs = this.getStudentEntitySpecification(search.getSearchCriteriaList());
    } else {
      if (search.getCondition() == Condition.AND) {
        studentSpecs = studentSpecs.and(this.getStudentEntitySpecification(search.getSearchCriteriaList()));
      } else {
        studentSpecs = studentSpecs.or(this.getStudentEntitySpecification(search.getSearchCriteriaList()));
      }
    }
    return studentSpecs;
  }

  private Specification<Saga> getStudentEntitySpecification(final List<SearchCriteria> criteriaList) {
    Specification<Saga> studentSpecs = null;
    if (!criteriaList.isEmpty()) {
      var i = 0;
      for (final SearchCriteria criteria : criteriaList) {
        if (criteria.getKey() != null && criteria.getOperation() != null && criteria.getValueType() != null) {
          final var criteriaValue = criteria.getValue();
          final Specification<Saga> typeSpecification = this.getTypeSpecification(criteria.getKey(), criteria.getOperation(), criteriaValue, criteria.getValueType());
          studentSpecs = this.getSpecificationPerGroup(studentSpecs, i, criteria, typeSpecification);
          i++;
        } else {
          throw new SagaRuntimeException("Search Criteria can not contain null values for key, value and operation type");
        }
      }
    }
    return studentSpecs;
  }

  /**
   * Gets specification per group.
   *
   * @param studentEntitySpecification the pen request batch entity specification
   * @param i                          the
   * @param criteria                   the criteria
   * @param typeSpecification          the type specification
   * @return the specification per group
   */
  private Specification<Saga> getSpecificationPerGroup(Specification<Saga> studentEntitySpecification, final int i, final SearchCriteria criteria, final Specification<Saga> typeSpecification) {
    if (i == 0) {
      studentEntitySpecification = Specification.where(typeSpecification);
    } else {
      if (criteria.getCondition() == Condition.AND) {
        studentEntitySpecification = studentEntitySpecification.and(typeSpecification);
      } else {
        studentEntitySpecification = studentEntitySpecification.or(typeSpecification);
      }
    }
    return studentEntitySpecification;
  }

  private Specification<Saga> getTypeSpecification(final String key, final FilterOperation filterOperation, final String value, final ValueType valueType) {
    Specification<Saga> studentEntitySpecification = null;
    switch (valueType) {
      case STRING:
        studentEntitySpecification = this.sagaFilterSpecs.getStringTypeSpecification(key, value, filterOperation);
        break;
      case DATE_TIME:
        studentEntitySpecification = this.sagaFilterSpecs.getDateTimeTypeSpecification(key, value, filterOperation);
        break;
      case UUID:
        studentEntitySpecification = this.sagaFilterSpecs.getUUIDTypeSpecification(key, value, filterOperation);
        break;
      default:
        break;
    }
    return studentEntitySpecification;
  }

  /**
   * Sets specification and sort criteria.
   *
   * @param sortCriteriaJson       the sort criteria json
   * @param searchCriteriaListJson the search criteria list json
   * @param sorts                  the sorts
   * @return the specification and sort criteria
   */
  public Specification<Saga> setSpecificationAndSortCriteria(final String sortCriteriaJson, final String searchCriteriaListJson, final List<Sort.Order> sorts) {
    Specification<Saga> studentSpecs = null;
    try {
      if (StringUtils.isNotBlank(sortCriteriaJson)) {
        final Map<String, String> sortMap = JsonUtil.objectMapper.readValue(sortCriteriaJson, new TypeReference<>() {
        });
        sortMap.forEach((k, v) -> {
          if ("ASC".equalsIgnoreCase(v)) {
            sorts.add(new Sort.Order(Sort.Direction.ASC, k));
          } else {
            sorts.add(new Sort.Order(Sort.Direction.DESC, k));
          }
        });
      }
      if (StringUtils.isNotBlank(searchCriteriaListJson)) {
        final List<Search> searches = JsonUtil.objectMapper.readValue(searchCriteriaListJson, new TypeReference<>() {
        });
        var i = 0;
        for (final var search : searches) {
          studentSpecs = this.getSpecifications(studentSpecs, i, search);
          i++;
        }
      }
    } catch (final JsonProcessingException e) {
      throw new SagaRuntimeException(e.getMessage());
    }
    return studentSpecs;
  }
}
