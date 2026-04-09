package kr.ac.ajou.ie.processchatbot.controller;

import java.time.OffsetDateTime;
import java.util.Map;
import kr.ac.ajou.ie.processchatbot.dto.HealthResponse;
import kr.ac.ajou.ie.processchatbot.service.DbHealthService;
import kr.ac.ajou.ie.processchatbot.service.LlmHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

	private final DbHealthService dbHealthService;
	private final LlmHealthService llmHealthService;

	@GetMapping("/health")
	public HealthResponse health() {
		return new HealthResponse(
			"UP",
			"Application is running.",
			Map.of("service", "process-log-chatbot"),
			OffsetDateTime.now()
		);
	}

	@GetMapping("/db/health")
	public HealthResponse dbHealth() {
		return this.dbHealthService.check();
	}

	@GetMapping("/llm/health")
	public HealthResponse llmHealth() {
		return this.llmHealthService.check();
	}
}
