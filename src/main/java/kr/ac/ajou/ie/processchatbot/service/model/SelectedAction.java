package kr.ac.ajou.ie.processchatbot.service.model;

public enum SelectedAction {
	NONE(false, false),
	GET_RECENT_LOGS(true, false),
	GET_LOGS_BY_CASSETTE_ID(true, false),
	GET_LOGS_BY_OPERATOR_NAME(true, false),
	GET_LOGS_BY_PROCESS_NAME(true, false),
	GET_LOGS_BY_WORK_DATE(true, false),
	GET_LOGS_BY_DATE_RANGE(true, false),
	COUNT_LOGS_BY_PROCESS_NAME(true, true),
	COUNT_LOGS_BY_OPERATOR_NAME(true, true),
	GET_LOGS_BY_OPERATOR_NAME_AND_DATE(true, false),
	GET_LOGS_BY_PROCESS_NAME_AND_DATE(true, false),
	COUNT_LOGS_BY_WORK_DATE(true, true),
	COUNT_LOGS_BY_DATE_RANGE(true, true);

	private final boolean usesDatabase;
	private final boolean countAction;

	SelectedAction(boolean usesDatabase, boolean countAction) {
		this.usesDatabase = usesDatabase;
		this.countAction = countAction;
	}

	public boolean usesDatabase() {
		return this.usesDatabase;
	}

	public boolean isCountAction() {
		return this.countAction;
	}
}
