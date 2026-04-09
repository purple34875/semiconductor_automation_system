package kr.ac.ajou.ie.processchatbot.queryplan.model;

public record MetricSpec(
	MetricType type,
	MetricField field,
	String alias
) {
}
