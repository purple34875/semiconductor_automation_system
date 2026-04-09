package kr.ac.ajou.ie.processchatbot.queryplan.sql;

import java.util.Map;
import kr.ac.ajou.ie.processchatbot.queryplan.model.GroupByField;
import kr.ac.ajou.ie.processchatbot.queryplan.model.SortField;

public final class SqlColumnRegistry {

	private static final Map<GroupByField, String> GROUP_BY_COLUMNS = Map.of(
		GroupByField.OPERATOR_NAME, "om.operator_name",
		GroupByField.PROCESS_NAME, "pm.process_name",
		GroupByField.CASSETTE_ID, "pl.cassette_id",
		GroupByField.WORK_DATE, "pl.work_date"
	);

	private static final Map<GroupByField, String> GROUP_BY_ALIASES = Map.of(
		GroupByField.OPERATOR_NAME, "operator_name",
		GroupByField.PROCESS_NAME, "process_name",
		GroupByField.CASSETTE_ID, "cassette_id",
		GroupByField.WORK_DATE, "work_date"
	);

	private static final Map<SortField, String> SORT_COLUMNS = Map.of(
		SortField.WORK_DATE, "pl.work_date",
		SortField.START_TIME, "pl.start_time",
		SortField.PROCESS_NAME, "pm.process_name",
		SortField.OPERATOR_NAME, "om.operator_name",
		SortField.CASSETTE_ID, "pl.cassette_id",
		SortField.LOG_COUNT, "log_count"
	);

	private SqlColumnRegistry() {
	}

	public static String groupByColumn(GroupByField field) {
		return GROUP_BY_COLUMNS.get(field);
	}

	public static String groupByAlias(GroupByField field) {
		return GROUP_BY_ALIASES.get(field);
	}

	public static String sortColumn(SortField field) {
		return SORT_COLUMNS.get(field);
	}
}
