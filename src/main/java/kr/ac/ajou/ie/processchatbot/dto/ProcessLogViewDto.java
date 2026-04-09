package kr.ac.ajou.ie.processchatbot.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ProcessLogViewDto(
	LocalDate workDate,
	String cassetteId,
	String processName,
	String operatorName,
	LocalTime startTime,
	LocalTime endTime,
	String remarks
) {
}
