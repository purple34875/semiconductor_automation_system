package kr.ac.ajou.ie.processchatbot.queryplan.model;

public enum PlanOperation {
	NONE(false),
	SELECT_ROWS(true),
	COUNT_ROWS(true),
	GROUP_COUNT(true),
	SUMMARIZE_ROWS(true);

	private final boolean usesDatabase;

	PlanOperation(boolean usesDatabase) {
		this.usesDatabase = usesDatabase;
	}

	public boolean usesDatabase() {
		return this.usesDatabase;
	}
}
