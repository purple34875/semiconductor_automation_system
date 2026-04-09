package kr.ac.ajou.ie.processchatbot.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import kr.ac.ajou.ie.processchatbot.dto.ProcessLogViewDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProcessLogRepository {

	private static final String BASE_SELECT = """
		SELECT
		  pl.work_date,
		  pl.cassette_id,
		  pm.process_name,
		  om.operator_name,
		  pl.start_time,
		  pl.end_time,
		  pl.remarks
		FROM Process_Log pl
		JOIN Process_Master pm ON pl.process_id = pm.process_id
		JOIN Operator_Master om ON pl.operator_id = om.operator_id
		""";

	private static final String ORDER_BY_RECENT = """
		ORDER BY pl.work_date DESC, pl.start_time DESC, pl.log_id DESC
		""";

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

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public List<ProcessLogViewDto> getRecentLogs(int limit) {
		String sql = BASE_SELECT + ORDER_BY_RECENT + " LIMIT :limit";
		return this.jdbcTemplate.query(sql, Map.of("limit", limit), PROCESS_LOG_ROW_MAPPER);
	}

	public List<ProcessLogViewDto> getLogsByCassetteId(String cassetteId, Integer limit) {
		String sql = BASE_SELECT + """
			WHERE UPPER(pl.cassette_id) = :cassetteId
			""" + ORDER_BY_RECENT + appendLimit(limit);
		return this.jdbcTemplate.query(sql, parameters("cassetteId", cassetteId, limit), PROCESS_LOG_ROW_MAPPER);
	}

	public List<ProcessLogViewDto> getLogsByOperatorName(String operatorName, Integer limit) {
		String sql = BASE_SELECT + """
			WHERE LOWER(om.operator_name) = LOWER(:operatorName)
			""" + ORDER_BY_RECENT + appendLimit(limit);
		return this.jdbcTemplate.query(sql, parameters("operatorName", operatorName, limit), PROCESS_LOG_ROW_MAPPER);
	}

	public List<ProcessLogViewDto> getLogsByProcessName(String processName, Integer limit) {
		String sql = BASE_SELECT + """
			WHERE LOWER(pm.process_name) = LOWER(:processName)
			""" + ORDER_BY_RECENT + appendLimit(limit);
		return this.jdbcTemplate.query(sql, parameters("processName", processName, limit), PROCESS_LOG_ROW_MAPPER);
	}

	public List<ProcessLogViewDto> getLogsByWorkDate(LocalDate workDate, Integer limit) {
		String sql = BASE_SELECT + """
			WHERE pl.work_date = :workDate
			""" + ORDER_BY_RECENT + appendLimit(limit);
		return this.jdbcTemplate.query(sql, parameters("workDate", workDate, limit), PROCESS_LOG_ROW_MAPPER);
	}

	public List<ProcessLogViewDto> getLogsByDateRange(LocalDate startDate, LocalDate endDate, Integer limit) {
		String sql = BASE_SELECT + """
			WHERE pl.work_date BETWEEN :startDate AND :endDate
			""" + ORDER_BY_RECENT + appendLimit(limit);

		MapSqlParameterSource params = new MapSqlParameterSource()
			.addValue("startDate", startDate)
			.addValue("endDate", endDate);
		if (limit != null) {
			params.addValue("limit", limit);
		}

		return this.jdbcTemplate.query(sql, params, PROCESS_LOG_ROW_MAPPER);
	}

	public List<ProcessLogViewDto> getLogsByOperatorNameAndDate(String operatorName, LocalDate workDate, Integer limit) {
		String sql = BASE_SELECT + """
			WHERE LOWER(om.operator_name) = LOWER(:operatorName)
			  AND pl.work_date = :workDate
			""" + ORDER_BY_RECENT + appendLimit(limit);

		MapSqlParameterSource params = new MapSqlParameterSource()
			.addValue("operatorName", operatorName)
			.addValue("workDate", workDate);
		if (limit != null) {
			params.addValue("limit", limit);
		}

		return this.jdbcTemplate.query(sql, params, PROCESS_LOG_ROW_MAPPER);
	}

	public List<ProcessLogViewDto> getLogsByProcessNameAndDate(String processName, LocalDate workDate, Integer limit) {
		String sql = BASE_SELECT + """
			WHERE LOWER(pm.process_name) = LOWER(:processName)
			  AND pl.work_date = :workDate
			""" + ORDER_BY_RECENT + appendLimit(limit);

		MapSqlParameterSource params = new MapSqlParameterSource()
			.addValue("processName", processName)
			.addValue("workDate", workDate);
		if (limit != null) {
			params.addValue("limit", limit);
		}

		return this.jdbcTemplate.query(sql, params, PROCESS_LOG_ROW_MAPPER);
	}

	public long countLogsByProcessName(String processName) {
		return executeCount("""
			SELECT COUNT(*)
			FROM Process_Log pl
			JOIN Process_Master pm ON pl.process_id = pm.process_id
			WHERE LOWER(pm.process_name) = LOWER(:processName)
			""", Map.of("processName", processName));
	}

	public long countLogsByOperatorName(String operatorName) {
		return executeCount("""
			SELECT COUNT(*)
			FROM Process_Log pl
			JOIN Operator_Master om ON pl.operator_id = om.operator_id
			WHERE LOWER(om.operator_name) = LOWER(:operatorName)
			""", Map.of("operatorName", operatorName));
	}

	public long countLogsByWorkDate(LocalDate workDate) {
		return executeCount("""
			SELECT COUNT(*)
			FROM Process_Log pl
			WHERE pl.work_date = :workDate
			""", Map.of("workDate", workDate));
	}

	public long countLogsByDateRange(LocalDate startDate, LocalDate endDate) {
		return executeCount("""
			SELECT COUNT(*)
			FROM Process_Log pl
			WHERE pl.work_date BETWEEN :startDate AND :endDate
			""", Map.of("startDate", startDate, "endDate", endDate));
	}

	private long executeCount(String sql, Map<String, ?> params) {
		Long count = this.jdbcTemplate.queryForObject(sql, params, Long.class);
		return count == null ? 0L : count;
	}

	private String appendLimit(Integer limit) {
		return limit == null ? "" : " LIMIT :limit";
	}

	private MapSqlParameterSource parameters(String key, Object value, Integer limit) {
		MapSqlParameterSource params = new MapSqlParameterSource().addValue(key, value);
		if (limit != null) {
			params.addValue("limit", limit);
		}
		return params;
	}
}
