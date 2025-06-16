package me.pgthinker.client;

import me.pgthinker.dto.request.MemoryConfigRequest;
import me.pgthinker.dto.request.MemoryCreateRequest;
import me.pgthinker.dto.request.MemoryDeleteRequest;
import me.pgthinker.dto.request.MemorySearchRequest;
import me.pgthinker.dto.response.Memory;
import me.pgthinker.dto.response.ResponseWrapper;
import me.pgthinker.exception.Mem0ClientException;
import me.pgthinker.interceptor.Mem0AuthInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

/**
 * Project: me.pgthinker.starter Author: NingNing0111 GitHub:
 * https://github.com/ningning0111 Date: 2025/6/14 20:44 Description:
 */
public class Mem0Client {

	private static final Logger log = LoggerFactory.getLogger(Mem0AuthInterceptor.class);

	private final String baseUrl;

	private final RestTemplate restTemplate;

	public Mem0Client(String baseUrl, String token, RestTemplate restTemplate) {
		this.baseUrl = baseUrl;
		this.restTemplate = restTemplate;
		this.restTemplate.getInterceptors().add(new Mem0AuthInterceptor(token));
	}

	public Boolean configure(MemoryConfigRequest request) {
		URI uri = UriComponentsBuilder.fromUriString(this.baseUrl + "/configure").build().encode().toUri();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<MemoryConfigRequest> entity = new HttpEntity(request, headers);
		ResponseEntity<ResponseWrapper> response = this.restTemplate.exchange(uri, HttpMethod.POST, entity,
				ResponseWrapper.class);
		if (response.getBody() == null) {
			throw new Mem0ClientException("Set memory configuration failed!", uri.toString());
		}
		else {
			boolean operationSuccess = ((ResponseWrapper) response.getBody()).getMessage() != null;
			if (operationSuccess) {
				log.info("Set memory configuration successful: {} -> {}", uri,
						((ResponseWrapper) response.getBody()).getMessage());
			}

			return operationSuccess;
		}
	}

	public List<Memory> createMemory(MemoryCreateRequest request) {
		validateIdentifiers(request.getUserId(), request.getAgentId(), request.getRunId());
		URI uri = UriComponentsBuilder.fromUriString(this.baseUrl + "/memories").build().encode().toUri();
		ResponseWrapper responseWrapper = restTemplate.postForObject(uri, request, ResponseWrapper.class);
		if (responseWrapper == null) {
			throw new Mem0ClientException("Create Memory fail!", "/memories");
		}
		return responseWrapper.getResults();

	}

	public Boolean deleteAllMemory(MemoryDeleteRequest request) {
		validateIdentifiers(request.getUserId(), request.getAgentId(), request.getRunId());
		URI uri = UriComponentsBuilder.fromUriString(this.baseUrl + "/memories")
			.queryParamIfPresent("user_id", Optional.ofNullable(request.getUserId()))
			.queryParamIfPresent("run_id", Optional.ofNullable(request.getRunId()))
			.queryParamIfPresent("agent_id", Optional.ofNullable(request.getAgentId()))
			.build()
			.encode()
			.toUri();
		ResponseWrapper responseWrapper = restTemplate.exchange(uri, HttpMethod.DELETE, null, ResponseWrapper.class)
			.getBody();
		if (responseWrapper == null) {
			throw new Mem0ClientException("Delete Memory fail!", uri.toString());
		}
		boolean operationSuccess = responseWrapper.getMessage() != null;
		if (operationSuccess) {
			log.info("Delete All Memory {} -> {}", uri, responseWrapper.getMessage());
		}
		return operationSuccess;
	}

	public Boolean deleteMemoryById(String memoryId) {
		URI uri = UriComponentsBuilder.fromUriString(this.baseUrl + "/memories/" + memoryId).build().encode().toUri();
		ResponseWrapper responseWrapper = restTemplate.exchange(uri, HttpMethod.DELETE, null, ResponseWrapper.class)
			.getBody();
		boolean operationSuccess = responseWrapper.getMessage() != null;
		if (operationSuccess) {
			log.info("Delete Memory by Id:  {} -> {}", uri, responseWrapper.getMessage());
		}
		return operationSuccess;
	}

	public Boolean updateMemoryById(String memoryId, String data) {
		URI uri = UriComponentsBuilder.fromUriString(this.baseUrl + "/memories/" + memoryId).build().encode().toUri();
		HashMap<String, Object> params = new HashMap<>();
		params.put("message", data);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);
		// 执行 PUT 请求
		ResponseEntity<ResponseWrapper> response = restTemplate.exchange(uri, HttpMethod.PUT, entity,
				ResponseWrapper.class);

		boolean operationSuccess = response.getBody() != null && response.getBody().getMessage() != null;
		if (operationSuccess) {
			log.info("Update Memory by Id:  {} -> {}", uri, response.getBody().getMessage());
		}
		return operationSuccess;
	}

	public List<Memory> searchMemory(MemorySearchRequest request) {
		validateIdentifiers(request.getUserId(), request.getAgentId(), request.getRunId());
		String url = this.baseUrl + "/memories/search/";
		ResponseWrapper responseWrapper = restTemplate.postForObject(url, request, ResponseWrapper.class);
		if (responseWrapper == null) {
			throw new Mem0ClientException("Search Memory fail!", "/memories/search/");
		}
		return responseWrapper.getResults();
	}

	public List<Memory> getAllMemory(MemorySearchRequest request) {
		validateIdentifiers(request.getUserId(), request.getAgentId(), request.getRunId());
		URI uri = UriComponentsBuilder.fromUriString(this.baseUrl + "/memories")
			.queryParamIfPresent("user_id", Optional.ofNullable(request.getUserId()))
			.queryParamIfPresent("run_id", Optional.ofNullable(request.getRunId()))
			.queryParamIfPresent("agent_id", Optional.ofNullable(request.getAgentId()))
			.queryParam("page_size", request.getPageSize())
			.build()
			.encode()
			.toUri();
		if (request.getFilters() != null) {
			log.warn("filter data:{} will be ignore!", request.getFilters());
		}
		ResponseWrapper responseWrapper = restTemplate.exchange(uri, HttpMethod.GET, null, ResponseWrapper.class)
			.getBody();
		if (responseWrapper == null) {
			throw new Mem0ClientException("Get All Memory fail!", uri.toString());
		}
		return responseWrapper.getResults();
	}

	public Memory getMemoryById(String memoryId) {
		URI uri = UriComponentsBuilder.fromUriString(this.baseUrl + "/memories/" + memoryId).build().encode().toUri();

		return restTemplate.exchange(uri, HttpMethod.GET, null, Memory.class).getBody();
	}

	private void validateIdentifiers(String userId, String runId, String agentId) {
		if (userId == null && runId == null && agentId == null) {
			throw new IllegalArgumentException("At least one identifier (userId, runId, agentId) is required");
		}
	}

}
