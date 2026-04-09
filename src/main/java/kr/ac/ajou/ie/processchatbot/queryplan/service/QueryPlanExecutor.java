package kr.ac.ajou.ie.processchatbot.queryplan.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.ac.ajou.ie.processchatbot.dto.GroupedCountViewDto;
import kr.ac.ajou.ie.processchatbot.dto.ProcessLogViewDto;
import kr.ac.ajou.ie.processchatbot.exception.DatabaseConnectionException;
import kr.ac.ajou.ie.processchatbot.queryplan.model.GroupByField;
import kr.ac.ajou.ie.processchatbot.queryplan.model.ValidatedQueryPlan;
import kr.ac.ajou.ie.processchatbot.queryplan.sql.QueryPlanSqlBuilder;
import kr.ac.ajou.ie.processchatbot.queryplan.sql.SqlColumnRegistry;
import kr.ac.ajou.ie.processchatbot.queryplan.sql.SqlQuerySpec;
import kr.ac.ajou.ie.processchatbot.queryplan.sql.SqlResultMode;
import kr.ac.ajou.ie.processchatbot.service.model.QueryExecutionResult;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class QueryPlanExecutor {

	private static final RowMapper<ProcessLogViewDto> PROCESS_LOG_ROW_MAPPER = new RowMapper<>() {
		@Override
		public ProcessLogViewDto mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new ProcessLogViewDto(
				rs.getDate("work_date").toLocalDate(),
				rs.getString("cassette_id"),
				rs.getString("process_name"),
				rs.getString("operator_name"),
				rs.getTime("start_time") != null ? rs.getTime("start_time").toLocalTime() : null,
				rs.getTime("end_time") != null ? rs.getTime("end_time").toLocalTime() : null,
				rs.getString("remarks")
			);
		}
	};

	private final QueryPlanSqlBuilder sqlBuilder;
	private final NamedParameterJdbcTemplate jdbcTemplate;

	public QueryPlanExecutor(QueryPlanSqlBuilder sqlBuilder, NamedParameterJdbcTemplate jdbcTemplate) {
		this.sqlBuilder = sqlBuilder;
		this.jdbcTemplate = jdbcTemplate;
	}

	public QueryExecutionResult execute(ValidatedQueryPlan plan) {
		try {
			SqlQuerySpec spec = this.sqlBuilder.build(plan);
			return switch (spec.resultMode()) {
				case LOG_ROWS -> QueryExecutionResult.forLogs(
					this.jdbcTemplate.query(spec.sql(), spec.params(), PROCESS_LOG_ROW_MAPPER),
					plan.operation().name()
				);
				case COUNT_ONLY -> QueryExecutionResult.forCount(
					queryCount(spec),
					plan.operation().name()
				);
				case GROUP_COUNT_ROWS -> QueryExecutionResult.forGroupCounts(
					queryGroupedCounts(spec, plan.groupBy()),
					plan.operation().name()
				);
			};
		}
		catch (DataAccessException ex) {
			throw new DatabaseConnectionException("DB connection failed.", ex);
		}
	}

	private long queryCount(SqlQuerySpec spec) {
		Long count = this.jdbcTemplate.queryForObject(spec.sql(), spec.params(), Long.class);
		return count == null ? 0L : count;
	}

	private List<GroupedCountViewDto> queryGroupedCounts(SqlQuerySpec spec, List<GroupByField> groupByFields) {
		return this.jdbcTemplate.query(spec.sql(), spec.params(), (rs, rowNum) -> {
			Map<String, String> groupValues = new LinkedHashMap<>();
			for (GroupByField field : groupByFields) {
				String alias = SqlColumnRegistry.groupByAlias(field);
				Object rawValue = rs.getObject(alias);
				groupValues.put(alias, normalizeGroupValue(rawValue));
			}
			return new GroupedCountViewDto(groupValues, rs.getLong("log_count"));
		});
	}

	private String normalizeGroupValue(Object rawValue) {
		if (rawValue == null) {
			return "";
		}
		if (rawValue instanceof LocalTime localTime) {
			return localTime.toString();
		}
		return String.valueOf(rawValue);
	}
}
