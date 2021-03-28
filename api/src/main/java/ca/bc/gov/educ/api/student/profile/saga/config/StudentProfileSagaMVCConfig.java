package ca.bc.gov.educ.api.student.profile.saga.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
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
  public void addInterceptors(final InterceptorRegistry registry) {
    registry.addInterceptor(this.studentProfileSagaInterceptor).addPathPatterns("/**");
  }

  @Bean
  public Executor taskExecutor() {
    return new EnhancedQueueExecutor.Builder()
        .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("async-task-%d").build())
        .setCorePoolSize(2).setMaximumPoolSize(50).setKeepAliveTime(Duration.ofSeconds(60)).build();
  }
}
