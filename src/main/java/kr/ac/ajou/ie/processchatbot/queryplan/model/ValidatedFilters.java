package kr.ac.ajou.ie.processchatbot.queryplan.model;

public record ValidatedFilters(
	TextFilter operatorName,
	TextFilter processName,
	TextFilter cassetteId,
	DateFilter workDate,
	DateRange dateRange
) {

	public boolean hasAny() {
		return this.operatorName != null
			|| this.processName != null
			|| this.cassetteId != null
			|| this.workDate != null
			|| this.dateRange != null;
	}
}
