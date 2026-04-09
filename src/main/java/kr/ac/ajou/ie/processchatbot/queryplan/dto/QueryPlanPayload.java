package kr.ac.ajou.ie.processchatbot.queryplan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record QueryPlanPayload(
	String version,
	String status,
	String queryType,
	String operation,
	QueryFiltersPayload filters,
	List<String> groupBy,
	List<MetricPayload> metrics,
	List<SortPayload> sort,
	Integer limit,
	String responseStyle,
	ClarificationPayload clarification,
	String unsupportedReason
) {
}
