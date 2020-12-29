package ca.bc.gov.educ.api.student.profile.saga.config;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Profile("!test")
public class StudentProfileSagaMVCConfig implements WebMvcConfigurer {

  @Getter(AccessLevel.PRIVATE)
  private final StudentProfileSagaInterceptor studentProfileSagaInterceptor;

  @Autowired
  public StudentProfileSagaMVCConfig(final StudentProfileSagaInterceptor studentProfileSagaInterceptor) {
    this.studentProfileSagaInterceptor = studentProfileSagaInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(studentProfileSagaInterceptor).addPathPatterns("/**");
  }

  @Bean
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("async-task-");
    executor.initialize();
    return executor;
  }
}
