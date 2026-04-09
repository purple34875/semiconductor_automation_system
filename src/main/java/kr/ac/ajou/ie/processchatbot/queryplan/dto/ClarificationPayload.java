package kr.ac.ajou.ie.processchatbot.queryplan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClarificationPayload(
	String message,
	List<String> missingFields
) {
}
