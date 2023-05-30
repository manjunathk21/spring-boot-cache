package com.comp.assortment.service.config;

import com.comp.dept.service.interceptor.JwtHmacInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("!test")
public class InterceptorConfig implements WebMvcConfigurer {

  @Autowired private JwtHmacInterceptor jwtHmacInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {

    registry
        .addInterceptor(jwtHmacInterceptor)
        .addPathPatterns("/execution/**")
        .addPathPatterns("/*/status")
        .addPathPatterns("/validation/**")
        .addPathPatterns("/vendor/maintenance/**")
        .addPathPatterns("/publish")
        .excludePathPatterns(
            "/",
            "/actuator/health",
            "/swagger*/**",
            "/v2/api-docs",
            "/config/ui",
            "/swagger-resources",
            "/config/security",
            "/swagger-ui.html",
            "/webjars/**");
  }
}
