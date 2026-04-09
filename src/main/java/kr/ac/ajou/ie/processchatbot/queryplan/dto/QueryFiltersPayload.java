package kr.ac.ajou.ie.processchatbot.queryplan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record QueryFiltersPayload(
	TextFilterPayload operatorName,
	TextFilterPayload processName,
	TextFilterPayload cassetteId,
	SingleDateFilterPayload workDate,
	DateRangeFilterPayload dateRange
) {
}
