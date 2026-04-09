package kr.ac.ajou.ie.processchatbot.queryplan.model;

public record SortSpec(
	SortField field,
	SortDirection direction
) {
}
