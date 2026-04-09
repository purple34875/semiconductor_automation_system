package kr.ac.ajou.ie.processchatbot.dto;

import java.time.OffsetDateTime;

public record ErrorResponse(
	OffsetDateTime timestamp,
	String code,
	String message,
	Object details
) {
}
