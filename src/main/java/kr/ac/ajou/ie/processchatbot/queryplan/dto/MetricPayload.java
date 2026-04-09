package kr.ac.ajou.ie.processchatbot.queryplan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MetricPayload(
	String type,
	String field,
	String alias
) {
}
