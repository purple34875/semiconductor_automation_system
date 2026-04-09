package kr.ac.ajou.ie.processchatbot.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record ChatbotProperties(
	@NotBlank String timezone,
	@Min(1) int maxQuestionLength,
	@Min(1) int defaultLimit,
	@Min(1) int maxLimit,
	@Min(1) int maxDateRangeDays,
	@NotBlank String ollamaHealthPath
) {
}
