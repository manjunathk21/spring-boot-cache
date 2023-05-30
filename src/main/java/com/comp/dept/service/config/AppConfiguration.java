package com.comp.dept.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.concurrent.TimeUnit;

@Configuration
@Getter
@Setter
@Slf4j
public class AppConfiguration {

 


    @Bean
    public RestTemplate restTemplate() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        ConnectionPool okHttpConnectionPool = new ConnectionPool(20, 30, TimeUnit.SECONDS);
        builder.connectionPool(okHttpConnectionPool);
        builder.connectTimeout(2, TimeUnit.MINUTES);
        builder.readTimeout(2, TimeUnit.MINUTES);
        builder.writeTimeout(2, TimeUnit.MINUTES);
        builder.retryOnConnectionFailure(false);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new RestTemplateResponseErrorHandler());
        restTemplate.setRequestFactory(new OkHttp3ClientHttpRequestFactory(builder.build()));
        return restTemplate;
    }

    @Bean
    public XmlMapper xmlMapper() {
        return new XmlMapper();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("strategies", "hmac");
    }

    @Bean
    public Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(databaseDriver);
        dataSource.setUrl(databaseJdbcUrl);
        dataSource.setUsername(dbUsername);
        dataSource.setPassword(dbPwd);
        return dataSource;
    }
}
