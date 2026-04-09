package kr.ac.ajou.ie.processchatbot.dto;

import java.util.Map;

public record GroupedCountViewDto(
	Map<String, String> groupValues,
	long count
) {
}
