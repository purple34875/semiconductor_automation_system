package kr.ac.ajou.ie.processchatbot.queryplan.model;

import java.util.List;

public record ValidatedQueryPlan(
	String version,
	PlanStatus status,
	PlanQueryType queryType,
	PlanOperation operation,
	ValidatedFilters filters,
	List<GroupByField> groupBy,
	List<MetricSpec> metrics,
	List<SortSpec> sort,
	Integer limit,
	ResponseStyle responseStyle,
	ClarificationInfo clarification,
	String unsupportedReason
) {

	public boolean isReady() {
		return this.status == PlanStatus.READY;
	}

	public boolean usesDatabase() {
		return this.operation.usesDatabase();
	}
}
