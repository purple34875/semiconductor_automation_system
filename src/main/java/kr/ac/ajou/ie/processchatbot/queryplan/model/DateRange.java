package kr.ac.ajou.ie.processchatbot.queryplan.model;

import java.time.LocalDate;

public record DateRange(
	LocalDate start,
	LocalDate end
) {
}
