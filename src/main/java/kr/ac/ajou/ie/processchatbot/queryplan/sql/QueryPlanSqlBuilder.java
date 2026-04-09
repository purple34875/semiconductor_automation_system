package kr.ac.ajou.ie.processchatbot.queryplan.sql;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.ac.ajou.ie.processchatbot.config.ChatbotProperties;
import kr.ac.ajou.ie.processchatbot.queryplan.model.DateFilter;
import kr.ac.ajou.ie.processchatbot.queryplan.model.DateRange;
import kr.ac.ajou.ie.processchatbot.queryplan.model.GroupByField;
import kr.ac.ajou.ie.processchatbot.queryplan.model.PlanOperation;
import kr.ac.ajou.ie.processchatbot.queryplan.model.SortDirection;
import kr.ac.ajou.ie.processchatbot.queryplan.model.SortSpec;
import kr.ac.ajou.ie.processchatbot.queryplan.model.TextFilter;
import kr.ac.ajou.ie.processchatbot.queryplan.model.ValidatedFilters;
import kr.ac.ajou.ie.processchatbot.queryplan.model.ValidatedQueryPlan;
import org.springframework.stereotype.Component;

@Component
public class QueryPlanSqlBuilder {

	private static final String BASE_FROM = """
		FROM Process_Log pl
		JOIN Process_Master pm ON pl.process_id = pm.process_id
		JOIN Operator_Master om ON pl.operator_id = om.operator_id
		""";

	private final ChatbotProperties properties;

	public QueryPlanSqlBuilder(ChatbotProperties properties) {
		this.properties = properties;
	}

	public SqlQuerySpec build(ValidatedQueryPlan plan) {
		if (!plan.isReady()) {
			throw new IllegalArgumentException("Only READY plans can be converted to SQL.");
		}

		return switch (plan.operation()) {
			case NONE -> throw new IllegalArgumentException("Operation NONE does not produce SQL.");
			case SELECT_ROWS, SUMMARIZE_ROWS -> buildSelectRows(plan);
			case COUNT_ROWS -> buildCountRows(plan);
			case GROUP_COUNT -> buildGroupCount(plan);
		};
	}

	private SqlQuerySpec buildSelectRows(ValidatedQueryPlan plan) {
		StringBuilder sql = new StringBuilder("""
			SELECT
			  pl.work_date,
			  pl.cassette_id,
			  pm.process_name,
			  om.operator_name,
			  pl.start_time,
			  pl.end_time,
			  pl.remarks
			""");

		Map<String, Object> params = new LinkedHashMap<>();
		List<String> whereClauses = buildWhereClauses(plan.filters(), params);

		sql.append(BASE_FROM);
		appendWhere(sql, whereClauses);
		appendOrderBy(sql, plan.sort(), List.of("pl.work_date DESC", "pl.start_time DESC", "pl.log_id DESC"));
		appendLimit(sql, params, plan.limit());

		return new SqlQuerySpec(sql.toString(), Map.copyOf(params), SqlResultMode.LOG_ROWS);
	}

	private SqlQuerySpec buildCountRows(ValidatedQueryPlan plan) {
		StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS log_count\n");
		Map<String, Object> params = new LinkedHashMap<>();
		List<String> whereClauses = buildWhereClauses(plan.filters(), params);

		sql.append(BASE_FROM);
		appendWhere(sql, whereClauses);

		return new SqlQuerySpec(sql.toString(), Map.copyOf(params), SqlResultMode.COUNT_ONLY);
	}

	private SqlQuerySpec buildGroupCount(ValidatedQueryPlan plan) {
		StringBuilder sql = new StringBuilder("SELECT\n");
		List<String> selectFields = new ArrayList<>();
		for (GroupByField field : plan.groupBy()) {
			selectFields.add("  " + SqlColumnRegistry.groupByColumn(field) + " AS " + SqlColumnRegistry.groupByAlias(field));
		}
		selectFields.add("  COUNT(*) AS log_count");
		sql.append(String.join(",\n", selectFields)).append('\n');

		Map<String, Object> params = new LinkedHashMap<>();
		List<String> whereClauses = buildWhereClauses(plan.filters(), params);

		sql.append(BASE_FROM);
		appendWhere(sql, whereClauses);
		appendGroupBy(sql, plan.groupBy());
		appendOrderBy(sql, plan.sort(), List.of("log_count DESC"));
		appendLimit(sql, params, plan.limit());

		return new SqlQuerySpec(sql.toString(), Map.copyOf(params), SqlResultMode.GROUP_COUNT_ROWS);
	}

	private List<String> buildWhereClauses(ValidatedFilters filters, Map<String, Object> params) {
		List<String> clauses = new ArrayList<>();

		TextFilter operator = filters.operatorName();
		if (operator != null) {
			clauses.add("LOWER(om.operator_name) = LOWER(:operatorName)");
			params.put("operatorName", operator.value());
		}

		TextFilter process = filters.processName();
		if (process != null) {
			clauses.add("LOWER(pm.process_name) = LOWER(:processName)");
			params.put("processName", process.value());
		}

		TextFilter cassette = filters.cassetteId();
		if (cassette != null) {
			clauses.add("UPPER(pl.cassette_id) = :cassetteId");
			params.put("cassetteId", cassette.value());
		}

		DateFilter workDate = filters.workDate();
		if (workDate != null) {
			clauses.add("pl.work_date = :workDate");
			params.put("workDate", resolveDate(workDate));
		}

		DateRange dateRange = filters.dateRange();
		if (dateRange != null) {
			clauses.add("pl.work_date BETWEEN :startDate AND :endDate");
			params.put("startDate", dateRange.start());
			params.put("endDate", dateRange.end());
		}

		return clauses;
	}

	private LocalDate resolveDate(DateFilter filter) {
		if (filter.absoluteValue() != null) {
			return filter.absoluteValue();
		}

		LocalDate today = LocalDate.now(ZoneId.of(this.properties.timezone()));
		return switch (filter.relativeValue()) {
			case "TODAY" -> today;
			case "YESTERDAY" -> today.minusDays(1);
			default -> throw new IllegalArgumentException("Unsupported relative date: " + filter.relativeValue());
		};
	}

	private void appendWhere(StringBuilder sql, List<String> whereClauses) {
		if (!whereClauses.isEmpty()) {
			sql.append("WHERE ").append(String.join("\n  AND ", whereClauses)).append('\n');
		}
	}

	private void appendGroupBy(StringBuilder sql, List<GroupByField> groupBy) {
		if (!groupBy.isEmpty()) {
			List<String> columns = groupBy.stream().map(SqlColumnRegistry::groupByColumn).toList();
			sql.append("GROUP BY ").append(String.join(", ", columns)).append('\n');
		}
	}

	private void appendOrderBy(StringBuilder sql, List<SortSpec> sortSpecs, List<String> fallbackOrder) {
		List<String> orderParts = new ArrayList<>();

		if (sortSpecs != null && !sortSpecs.isEmpty()) {
			for (SortSpec spec : sortSpecs) {
				String column = SqlColumnRegistry.sortColumn(spec.field());
				String direction = spec.direction() == SortDirection.ASC ? "ASC" : "DESC";
				orderParts.add(column + " " + direction);
			}
		}
		else {
			orderParts.addAll(fallbackOrder);
		}

		if (!orderParts.isEmpty()) {
			sql.append("ORDER BY ").append(String.join(", ", orderParts)).append('\n');
		}
	}

	private void appendLimit(StringBuilder sql, Map<String, Object> params, Integer limit) {
		if (limit != null) {
			sql.append("LIMIT :limit");
			params.put("limit", limit);
		}
	}
}
