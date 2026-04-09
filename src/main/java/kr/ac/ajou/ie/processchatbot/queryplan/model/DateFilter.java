package kr.ac.ajou.ie.processchatbot.queryplan.model;

import java.time.LocalDate;

public record DateFilter(
	DateValueType type,
	String relativeValue,
	LocalDate absoluteValue
) {
}
