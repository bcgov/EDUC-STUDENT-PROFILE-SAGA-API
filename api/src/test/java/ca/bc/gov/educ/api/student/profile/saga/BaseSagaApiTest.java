package ca.bc.gov.educ.api.student.profile.saga;

import ca.bc.gov.educ.api.student.profile.saga.support.SagApiTestUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = {StudentProfileSagaApiResourceApplication.class})
public abstract class BaseSagaApiTest {
  @Autowired
  SagApiTestUtils sagApiTestUtils;

  @Before
  public void before() {
    this.sagApiTestUtils.cleanDB();
  }
}
