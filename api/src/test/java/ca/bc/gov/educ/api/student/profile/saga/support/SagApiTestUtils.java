package ca.bc.gov.educ.api.student.profile.saga.support;

import ca.bc.gov.educ.api.student.profile.saga.repository.SagaEventRepository;
import ca.bc.gov.educ.api.student.profile.saga.repository.SagaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SagApiTestUtils {

  @Autowired
  SagaRepository sagaRepository;
  @Autowired
  SagaEventRepository sagaEventRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void cleanDB() {
    this.sagaEventRepository.deleteAll();
    this.sagaRepository.deleteAll();
  }
}
