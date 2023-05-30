package com.homedepot.assortment.service.client;

import assortment.utils.components.hmac.HmacJwtGenerator;
import assortment.utils.components.log.MonitoringLog;
import assortment.utils.constant.RequestType;
import assortment.utils.dto.AssortmentAggregatorRequestEntity;
import assortment.utils.dto.AssortmentTruthChangeRequestDTO;
import assortment.utils.exception.AdsException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.homedepot.assortment.service.config.AssortmentTruthServiceConfiguration;
import com.homedepot.assortment.service.dto.ReasonCode;
import com.homedepot.assortment.service.dto.SkuResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static assortment.utils.constant.AdsConstants.MCAFEE_HEADER_TOKEN;
import static assortment.utils.constant.LogbackKeyValue.HTTP_REQUEST_SIZE;
import static assortment.utils.constant.LogbackKeyValue.HTTP_RESPONSE;
import static assortment.utils.constant.LogbackKeyValue.HTTP_RESPONSE_TIME;
import static assortment.utils.constant.LogbackKeyValue.HTTP_URL;
import static assortment.utils.constant.LogbackKeyValue.IS_HTTP_CALL;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.Entry;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
@Slf4j
public class HttpClient {

    private RestTemplate restTemplate;
    private XmlMapper xmlMapper;
    private AssortmentTruthServiceConfiguration assortmentTruthServiceConfiguration;
    private ObjectMapper objectMapper;
    private HmacJwtGenerator hmacJwtGenerator;
    private MonitoringLog monitoringLog;

    @Autowired
    public HttpClient(
            @Qualifier("restTemplate") RestTemplate restTemplate,
            XmlMapper xmlMapper,
            AssortmentTruthServiceConfiguration assortmentTruthServiceConfiguration,
            ObjectMapper objectMapper,
            HmacJwtGenerator hmacJwtGenerator,
            MonitoringLog monitoringLog) {
        this.restTemplate = restTemplate;
        this.xmlMapper = xmlMapper;
        this.assortmentTruthServiceConfiguration = assortmentTruthServiceConfiguration;
        this.objectMapper = objectMapper;
        this.hmacJwtGenerator = hmacJwtGenerator;
        this.monitoringLog = monitoringLog;
    }

    @Cacheable(value = "strategies", sync = true)
    public Map<String, Integer> getStrategies() {

        try {
            ResponseEntity<String> xmlResponse =
                    restTemplate.exchange(
                            assortmentTruthServiceConfiguration.getSkuReasonCodeUrl(), GET, null, String.class);
            xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            xmlMapper.setDefaultUseWrapper(false);

            SkuResponse skuResponse = xmlMapper.readValue(xmlResponse.getBody(), SkuResponse.class);
            List<ReasonCode> skuChangeReasonCodes = skuResponse.getResultSet().getReasonCodeList();

            Map<String, Integer> inactiveStrategies =
                    skuChangeReasonCodes.stream()
                            .collect(
                                    Collectors.toMap(
                                            ReasonCode::getShortReasonDescription,
                                            ReasonCode::getSkuChangeReasonCode,
                                            (oldVal, newVal) -> oldVal));

            inactiveStrategies.put("Inactive Promo Special Buy Event", 1);

            Map<String, Integer> genericStrategies = new HashMap<>();
            genericStrategies.put("Active", 100);
            genericStrategies.put("In Season", 200);
            genericStrategies.put("Seasonal", 250);
            genericStrategies.put("Out Season", 300);
            genericStrategies.put("Clearance", 500);
            genericStrategies.put("Delete", 600);
            genericStrategies.put("Inactive", 400);
            genericStrategies.put("vendor-maintenance", 0);

            return Stream.concat(
                    genericStrategies.entrySet().stream(), inactiveStrategies.entrySet().stream())
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        } catch (Exception e) {
            log.error("failed to create strategies cache.", kv(IS_HTTP_CALL.value(), true),
                    kv(HTTP_URL.value(), assortmentTruthServiceConfiguration.getSkuReasonCodeUrl()),
                    kv(HTTP_RESPONSE.value(), e));
            monitoringLog.sendAlert("failed to create strategies cache.", e);
            throw AdsException.builder()
                    .userMessage("failed to create strategies cache.")
                    .detailedExceptionMessage(e.getMessage()).build();

        }
    }

    public void assortmentAggregation(
            String requestId,
            RequestType requestType,
            List<AssortmentTruthChangeRequestDTO> assortmentTruthChangeRequestDTO,
            Integer refId,
            String refName,
            Integer refChunks) {
        String jsonString = null;

        String url =
                assortmentTruthServiceConfiguration.getAssortmentAggregationUrl()
                        + "/"
                        + requestId
                        + "?requestType="
                        + requestType;
        //Regional Assortment requests with reference details.
         if (refId!=null){
             url = url + "&refId="+refId+"&refName="+refName+"&refChunks="+refChunks;
        }

        long start = System.currentTimeMillis();

        try {
            jsonString = objectMapper.writeValueAsString(assortmentTruthChangeRequestDTO);
            MultiValueMap<String, String> body = getBody();
            body.add(CONTENT_TYPE, APPLICATION_JSON_VALUE);
            body.add(MCAFEE_HEADER_TOKEN, assortmentTruthServiceConfiguration.getMcafeeToken());
            HttpEntity<?> httpEntity = new HttpEntity<>(jsonString, body);

            ResponseEntity<String> responseEntity =
                    restTemplate.exchange(url, POST, httpEntity, String.class);


            if (responseEntity.getStatusCode() != HttpStatus.OK) {

                log.error("Failed to create assortment request ", kv(IS_HTTP_CALL.value(), true),
                        kv(HTTP_URL.value(), url),
                        kv(HTTP_REQUEST_SIZE.value(), assortmentTruthChangeRequestDTO.size()),
                        kv(HTTP_RESPONSE.value(), responseEntity),
                        kv(HTTP_RESPONSE_TIME.value(), System.currentTimeMillis() - start));

                throw AdsException.builder()
                        .userMessage("failed to create assortment request")
                        .detailedExceptionMessage(responseEntity.getStatusCode().getReasonPhrase())
                        .build();
            } else {
                log.info("assortment request is created ", kv(IS_HTTP_CALL.value(), true), kv(HTTP_URL.value(), url), kv(HTTP_RESPONSE.value(), responseEntity));
            }
        } catch (RestClientException | JsonProcessingException e) {

            log.error("Failed to create assortment request ", kv(IS_HTTP_CALL.value(), true),
                    kv(HTTP_URL.value(), url),
                    kv(HTTP_REQUEST_SIZE.value(), assortmentTruthChangeRequestDTO.size()),
                    kv(HTTP_RESPONSE.value(), e),
                    kv(HTTP_RESPONSE_TIME.value(), System.currentTimeMillis() - start));

            throw AdsException.builder()
                    .userMessage("failed to create assortment request")
                    .detailedExceptionMessage(e.getMessage())
                    .build();
        }
    }

    public ResponseEntity getStatusForRequest(String requestId) {
        try {
            HttpEntity<?> httpEntity = new HttpEntity<>(getBody());

            ResponseEntity<String> responseEntity =
                    restTemplate.exchange(
                            assortmentTruthServiceConfiguration.getAssortmentAggregationUrl() + "/" + requestId,
                            GET,
                            httpEntity,
                            String.class);

            if (responseEntity.getStatusCode() != HttpStatus.OK) {
                throw AdsException.builder()
                        .userMessage("failed to get assortment request status")
                        .detailedExceptionMessage(responseEntity.getStatusCode().getReasonPhrase())
                        .build();
            }

            return responseEntity;

        } catch (RestClientException re) {
            throw AdsException.builder()
                    .userMessage("failed to get assortment request status")
                    .detailedExceptionMessage(re.getMessage())
                    .build();
        }
    }


    private LinkedMultiValueMap<String, String> getBody() {
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add(CONTENT_TYPE, APPLICATION_JSON_VALUE);
        body.add(MCAFEE_HEADER_TOKEN, assortmentTruthServiceConfiguration.getMcafeeToken());
        body.add(ACCEPT, APPLICATION_JSON_VALUE);
        return body;
    }

    public ResponseEntity executeVendorMaintenanceForOnline(List<AssortmentTruthChangeRequestDTO> assortmentTruthChangeRequestDTO) {

        String url = assortmentTruthServiceConfiguration.getAssortmentExecutionUrl()
                + "/execution/vendor/status";
        long start = System.currentTimeMillis();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.add(AUTHORIZATION, "Bearer " + hmacJwtGenerator.getAeHmacJwtToken());
            String body = objectMapper.writeValueAsString(assortmentTruthChangeRequestDTO);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity responseEntity = restTemplate.exchange(
                    url,
                    POST,
                    entity,
                    String.class);


            log.info("vendor maintenance http call to assortment execution ", kv(IS_HTTP_CALL.value(), true),
                    kv(HTTP_URL.value(), url),
                    kv(HTTP_REQUEST_SIZE.value(), assortmentTruthChangeRequestDTO.size()),
                    kv(HTTP_RESPONSE_TIME.value(), System.currentTimeMillis() - start),
                    kv(HTTP_RESPONSE.value(), responseEntity));

            return responseEntity;
        } catch (RestClientException | JsonProcessingException e) {
            log.error("failed to vendor maintenance http call to assortment execution ", kv(IS_HTTP_CALL.value(), true),
                    kv(HTTP_URL.value(), url),
                    kv(HTTP_REQUEST_SIZE.value(), assortmentTruthChangeRequestDTO.size()),
                    kv(HTTP_RESPONSE_TIME.value(), System.currentTimeMillis() - start),
                    kv(HTTP_RESPONSE.value(), e));

            monitoringLog.sendAlert("failed to vendor maintenance http call to assortment execution", e);
            throw AdsException.builder()
                    .userMessage("failed to call vendor-maintenance api. please retry again.")
                    .detailedExceptionMessage(e.getMessage())
                    .build();
        }
    }
}

