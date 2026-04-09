package kr.ac.ajou.ie.processchatbot.service;

import java.time.OffsetDateTime;
import java.util.Map;
import kr.ac.ajou.ie.processchatbot.dto.HealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DbHealthService {

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public HealthResponse check() {
		try {
			Integer value = this.jdbcTemplate.getJdbcTemplate().queryForObject("SELECT 1", Integer.class);
			return new HealthResponse(
				"UP",
				"DB connection is available.",
				Map.of("queryResult", value),
				OffsetDateTime.now()
			);
		}
		catch (Exception ex) {
			return new HealthResponse(
				"DOWN",
				"DB connection failed.",
				Map.of("error", ex.getMessage()),
				OffsetDateTime.now()
			);
		}
	}
}
