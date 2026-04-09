package kr.ac.ajou.ie.processchatbot.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record HealthResponse(
	String status,
	String message,
	Map<String, Object> details,
	OffsetDateTime timestamp
) {
}
