package me.pgthinker.ai.advisor;

import me.pgthinker.client.Mem0Client;
import me.pgthinker.dto.request.MemoryCreateRequest;
import me.pgthinker.dto.request.MemorySearchRequest;
import me.pgthinker.dto.response.Memory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Project: me.pgthinker.ai.advisor Author: NingNing0111 GitHub:
 * https://github.com/ningning0111 Date: 2025/6/16 23:21 Description:
 */
public class Mem0Advisor implements BaseAdvisor {

	private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = SystemPromptTemplate.builder()
		.template(
				"You are a helpful AI. Answer the question based on query and memories.\\nUser Memories:\\n{memories_str}")
		.build();

	private static final int DEFAULT_ORDER = 0;

	private final Mem0Client mem0Client;

	private final PromptTemplate promptTemplate;

	private final int order;

	private final MemorySearchRequest memorySearchRequest;

	private Mem0Advisor(Mem0Client mem0Client, MemorySearchRequest memorySearchRequest) {
		Assert.notNull(mem0Client, "mem0Client must not be null");
		Assert.notNull(memorySearchRequest, "memorySearchRequest must not be null");
		String userId = memorySearchRequest.getUserId();
		String runId = memorySearchRequest.getRunId();
		String agentId = memorySearchRequest.getAgentId();
		if (userId == null && runId == null && agentId == null) {
			throw new IllegalArgumentException("userId, runId, and agentId cannot all be null");
		}

		this.mem0Client = mem0Client;
		this.promptTemplate = DEFAULT_PROMPT_TEMPLATE;
		this.order = DEFAULT_ORDER;
		this.memorySearchRequest = memorySearchRequest;
	}

	private Mem0Advisor(Mem0Client mem0Client, @Nullable PromptTemplate promptTemplate, int order,
			MemorySearchRequest memorySearchRequest) {
		Assert.notNull(mem0Client, "mem0Client must not be null");
		Assert.notNull(memorySearchRequest, "memorySearchRequest must not be null");
		String userId = memorySearchRequest.getUserId();
		String runId = memorySearchRequest.getRunId();
		String agentId = memorySearchRequest.getAgentId();
		if (userId == null && runId == null && agentId == null) {
			throw new IllegalArgumentException("userId, runId, and agentId cannot all be null");
		}
		this.mem0Client = mem0Client;
		this.memorySearchRequest = memorySearchRequest;
		this.promptTemplate = promptTemplate != null ? promptTemplate : DEFAULT_PROMPT_TEMPLATE;
		this.order = order;
	}

	@Override
	public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
		List<Memory> memories = this.mem0Client.searchMemory(this.memorySearchRequest);
		UserMessage userMessage = chatClientRequest.prompt().getUserMessage();
		String memoriesStr = String.join("\n", memories.stream().map(Memory::getMemory).toList());

		Map<String, Object> context = new HashMap<>(chatClientRequest.context());
		context.put("memories", memoriesStr);
		context.put("input", userMessage.getText());

		SystemMessage systemMessage = chatClientRequest.prompt().getSystemMessage();
		String originSystemMessage = systemMessage.getText();
		String memoriesMessage = this.promptTemplate.createMessage(Map.of("memories_str", memoriesStr)).getText();
		String augmentedSystemText = String.join("\n\n", originSystemMessage, memoriesMessage);
		return chatClientRequest.mutate()
			.prompt(chatClientRequest.prompt().augmentSystemMessage(augmentedSystemText))
			.context(context)
			.build();

	}

	@Override
	public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
		ChatResponse.Builder chatResponseBuilder;
		if (chatClientResponse.chatResponse() == null) {
			chatResponseBuilder = ChatResponse.builder();
		}
		else {
			chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
		}
		assert chatClientResponse.chatResponse() != null;
		String input = (String) chatClientResponse.context().get("input");
		String response = chatClientResponse.chatResponse().getResult().getOutput().getText();
		chatResponseBuilder.metadata("memories", chatClientResponse.context().get("memories"));
		MemoryCreateRequest memoryCreateRequest = getMemoryCreateRequest(input, response);
		this.mem0Client.createMemory(memoryCreateRequest);
		return ChatClientResponse.builder()
			.chatResponse(chatResponseBuilder.build())
			.context(chatClientResponse.context())
			.build();
	}

	private MemoryCreateRequest getMemoryCreateRequest(String input, String response) {
		MemoryCreateRequest memoryCreateRequest = new MemoryCreateRequest();
		if (this.memorySearchRequest.getUserId() != null) {
			memoryCreateRequest.setUserId(this.memorySearchRequest.getUserId());
		}
		if (this.memorySearchRequest.getAgentId() != null) {
			memoryCreateRequest.setAgentId(this.memorySearchRequest.getAgentId());
		}
		if (this.memorySearchRequest.getRunId() != null) {
			memoryCreateRequest.setRunId(this.memorySearchRequest.getRunId());
		}
		memoryCreateRequest.setMessages(List.of(new MemoryCreateRequest.Message(input, "user"),
				new MemoryCreateRequest.Message(response, "assistant")));
		return memoryCreateRequest;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public static Mem0Advisor.Builder builder(Mem0Client mem0Client, MemorySearchRequest memorySearchRequest) {
		return new Mem0Advisor.Builder(mem0Client, memorySearchRequest);
	}

	public static final class Builder {

		private final Mem0Client mem0Client;

		private PromptTemplate promptTemplate;

		private int order = DEFAULT_ORDER;

		private final MemorySearchRequest memorySearchRequest;

		private Builder(Mem0Client mem0Client, MemorySearchRequest memorySearchRequest) {
			Assert.notNull(mem0Client, "mem0Client must not be null");
			Assert.notNull(memorySearchRequest, "memorySearchRequest must not be null");
			String userId = memorySearchRequest.getUserId();
			String runId = memorySearchRequest.getRunId();
			String agentId = memorySearchRequest.getAgentId();
			if (userId == null && runId == null && agentId == null) {
				throw new IllegalArgumentException("userId, runId, and agentId cannot all be null");
			}
			this.mem0Client = mem0Client;
			this.memorySearchRequest = memorySearchRequest;
		}

		public Mem0Advisor.Builder promptTemplate(PromptTemplate promptTemplate) {
			Assert.notNull(promptTemplate, "promptTemplate cannot be null");
			this.promptTemplate = promptTemplate;
			return this;
		}

		public Mem0Advisor.Builder order(int order) {
			this.order = order;
			return this;
		}

		public Mem0Advisor build() {
			return new Mem0Advisor(this.mem0Client, this.promptTemplate, this.order, this.memorySearchRequest);
		}

	}

}
