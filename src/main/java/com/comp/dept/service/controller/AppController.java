package com.comp.dept.service.controller;



import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RestController
@Api("Spring boot Cache")
@Slf4j
public class AppController {

	private final AppService appService;


	public AppController(AppService appService, HttpClient httpClient) {
		this.appService = appService;
		this.httpClient = httpClient;
	}

	@ApiOperation(value = "app execution")
	@PostMapping(path = "/execution", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AppResponse> execution(@RequestBody List<RequestDto> requestList,
	                                   @ApiParam(hidden = true) @RequestAttribute(value = HttpHeaders.USER_AGENT, required = false) String sourceSystemId) {
		return processRequest(requestList, sourceSystemId, EXECUTION, null, null, null);

	}

	@ApiOperation(value = "app validation")
	@PostMapping(path = "/validation", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AppResponse> validation(@RequestBody List<RequestDto> requestList,
																	 @ApiParam(hidden = true) @RequestAttribute(value = HttpHeaders.USER_AGENT, required = false) String sourceSystemId) {
		return processRequest(requestList, sourceSystemId, VALIDATION, null, null, null);
	}

	private ResponseEntity<AppResponse> processRequest(List<RequestDto> requestList,
																		  String sourceSystemId,
																		  RequestType requestType,
																		  Integer refId,
																		  String refName,
																		  Integer refChunks) {
		long start = System.currentTimeMillis();


		log.info("request for {} ", requestType, kv(LOG_REQUEST.value(),requestList),
				kv(LOG_RESPONSE.value(), responseEntity),
				kv(PROCESS_TIME.value(), System.currentTimeMillis()-start));
		return responseEntity;
	}

	@ApiOperation(value = "App request status check")
	@GetMapping(path = "/{requestId}/status")
	public ResponseEntity status(@PathVariable("requestId") String requestId) {
		log.info("call for ads request status ");
		return appService.getStatusForRequest(requestId);
	}

	

	@ApiOperation(value = "Publish to GCP topic")
	@PostMapping(path = "/execution/tocommons", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity publishToCommons(@RequestBody List<RequestEntity> requestEntity) {
		appService.publishToCommons(requestEntity);
		return ResponseEntity.ok("");
	}

}
