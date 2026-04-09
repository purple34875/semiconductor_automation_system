package kr.ac.ajou.ie.processchatbot.queryplan.model;

import java.util.List;

public record ClarificationInfo(
	String message,
	List<String> missingFields
) {
}
