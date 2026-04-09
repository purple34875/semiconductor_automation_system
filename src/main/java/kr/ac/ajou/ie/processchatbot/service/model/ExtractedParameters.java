package kr.ac.ajou.ie.processchatbot.service.model;

import java.time.LocalDate;

public record ExtractedParameters(
	String operatorName,
	String processName,
	String cassetteId,
	LocalDate workDate,
	LocalDate startDate,
	LocalDate endDate,
	Integer limit,
	boolean countQuery,
	boolean recentQuery
) {

	public boolean hasDateRange() {
		return this.startDate != null && this.endDate != null;
	}

	public boolean hasSingleDate() {
		return this.workDate != null;
	}

	public boolean hasLookupTarget() {
		return this.operatorName != null
			|| this.processName != null
			|| this.cassetteId != null
			|| this.workDate != null
			|| hasDateRange();
	}
}
