package kr.ac.ajou.ie.processchatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import kr.ac.ajou.ie.processchatbot.config.ChatbotProperties;
import kr.ac.ajou.ie.processchatbot.dto.HealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class LlmHealthService {

	private final RestClient ollamaHealthRestClient;
	private final ChatbotProperties properties;
	private final ObjectMapper objectMapper;

	@Value("${spring.ai.ollama.chat.model:exaone3.5:latest}")
	private String configuredModel;

	public HealthResponse check() {
		try {
			String payload = this.ollamaHealthRestClient.get()
				.uri(this.properties.ollamaHealthPath())
				.retrieve()
				.body(String.class);

			JsonNode root = this.objectMapper.readTree(payload);
			boolean modelAvailable = false;
			if (root.has("models")) {
				for (JsonNode modelNode : root.get("models")) {
					if (this.configuredModel.equals(modelNode.path("name").asText())) {
						modelAvailable = true;
						break;
					}
				}
			}

			Map<String, Object> details = new HashMap<>();
			details.put("configuredModel", this.configuredModel);
			details.put("modelAvailable", modelAvailable);

			return new HealthResponse("UP", "Ollama API is reachable.", details, OffsetDateTime.now());
		}
		catch (Exception ex) {
			return new HealthResponse(
				"DOWN",
				"Ollama API is not reachable.",
				Map.of("error", ex.getMessage(), "configuredModel", this.configuredModel),
				OffsetDateTime.now()
			);
		}
	}
}
