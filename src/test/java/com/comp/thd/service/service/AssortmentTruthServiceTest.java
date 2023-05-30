package com.comp.dept.service.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppServiceTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validatorlocal;

    @Mock
    private HttpClient httpClient;

    @Mock
    private Validator validator;

    @InjectMocks
    private AppService appService;

    @BeforeEach
    public void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validatorlocal = validatorFactory.getValidator();

    }

    @Test
    void validationsTest() {
        List<AppRequestDTO> request = Arrays.asList(AppChangeRequestDTO.builder()
                .strategy("Active")
                .sourceSystemId("Mac")
                .generatedOn(new Date(System.currentTimeMillis()))
                .userId("")
                .build());

        Set<ConstraintViolation<AppChangeRequestDTO>> constraintViolations =
                validatorlocal.validate(request.get(0));

        when(validator.validate(request.get(0))).thenReturn(constraintViolations);

        AppResponse response = appService.validations(request);
        assertTrue(response.getFailedRequests().size() == 0);
    }



}
