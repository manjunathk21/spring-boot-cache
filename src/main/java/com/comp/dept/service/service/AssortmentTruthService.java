package com.comp.dept.service.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@Slf4j
public class AppService {

    private HttpClient httpClient;

    private Validator validator;

    private MessagePublisher messagePublisher;

    private ObjectMapper objectMapper;

    @Value("")
    private String adsRequestTopicName;

    @Value("")
    private String ingestionTopicName;

    @Value("")
    private String gcpProjectName;

    @Autowired
    public AppService(HttpClient httpClient, Validator validator, MessagePublisher messagePublisher, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.validator = validator;
        this.messagePublisher = messagePublisher;
        this.objectMapper = objectMapper;
    }

    public AppResponse validations(List<AppChangeRequestDTO> appChangeRequestDTOList) {

        AppResponse appResponse = new AppResponse();

        appChangeRequestDTOList.forEach(req -> {

            Set<ConstraintViolation<AppChangeRequestDTO>> constraintViolations = validator.validate(req);

            if (!constraintViolations.isEmpty()) {
                if (req.getErrors() == null || req.getErrors().isEmpty()) {
                    req.setErrors(new ArrayList<>());
                }
                for (ConstraintViolation<AppChangeRequestDTO> cv : constraintViolations) {
                    req.getErrors().add(cv.getMessage());
                }
            }
        });

        List<AppChangeRequestDTO> errorRequests = appChangeRequestDTOList.stream()
                .filter(a -> null != a.getErrors() && !a.getErrors().isEmpty()).collect(Collectors.toList());

        appResponse.setFailedRequests(errorRequests);
        return appResponse;
    }


    public ResponseEntity getStatusForRequest(String requestId) {
        return httpClient.getStatusForRequest(requestId);
    }


    public void publishTogcp(List<RequestEntity> requestEntity) {
        try {
            messagePublisher.publish(ingestionTopicName, objectMapper.writeValueAsString(requestEntity));
        } catch (IOException e) {
            log.error("publish to  gcp failed - " + e.getMessage(), e);
            throw AdsException.builder().userMessage("publish to  gcp failed ")
                    .detailedExceptionMessage("publish to  gcp failed "+ e.getMessage())
                    .build();
        }
    }

   
}
