package me.pgthinker.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class MemoryConfigRequest {

	@JsonProperty("config")
	private Map<String, Object> config;

	private MemoryConfigRequest() {
	}

	public static Builder builder() {
		return new Builder();
	}

	public Map<String, Object> getConfig() {
		return this.config;
	}

	public void setConfig(Map<String, Object> config) {
		this.config = config;
	}

	public static class Builder {

		private final MemoryConfigRequest request = new MemoryConfigRequest();

		public Builder() {
		}

		public Builder config(Map<String, Object> config) {
			this.request.config = config;
			return this;
		}

		public MemoryConfigRequest build() {
			return this.request;
		}

	}

}
