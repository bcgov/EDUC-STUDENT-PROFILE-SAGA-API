package ca.bc.gov.educ.api.student.profile.saga.config;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StudentProfileSagaMVCConfig implements WebMvcConfigurer {

    @Getter(AccessLevel.PRIVATE)
    private final StudentProfileSagaInterceptor studentProfileSagaInterceptor;

    @Autowired
    public StudentProfileSagaMVCConfig(final StudentProfileSagaInterceptor studentProfileSagaInterceptor){
        this.studentProfileSagaInterceptor = studentProfileSagaInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(studentProfileSagaInterceptor).addPathPatterns("/**");
    }
}
