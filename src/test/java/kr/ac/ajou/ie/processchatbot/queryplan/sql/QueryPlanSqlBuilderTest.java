package kr.ac.ajou.ie.processchatbot.queryplan.sql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kr.ac.ajou.ie.processchatbot.config.ChatbotProperties;
import kr.ac.ajou.ie.processchatbot.queryplan.model.GroupByField;
import kr.ac.ajou.ie.processchatbot.queryplan.model.MatchType;
import kr.ac.ajou.ie.processchatbot.queryplan.model.MetricField;
import kr.ac.ajou.ie.processchatbot.queryplan.model.MetricSpec;
import kr.ac.ajou.ie.processchatbot.queryplan.model.MetricType;
import kr.ac.ajou.ie.processchatbot.queryplan.model.PlanOperation;
import kr.ac.ajou.ie.processchatbot.queryplan.model.PlanQueryType;
import kr.ac.ajou.ie.processchatbot.queryplan.model.PlanStatus;
import kr.ac.ajou.ie.processchatbot.queryplan.model.ResponseStyle;
import kr.ac.ajou.ie.processchatbot.queryplan.model.SortDirection;
import kr.ac.ajou.ie.processchatbot.queryplan.model.SortField;
import kr.ac.ajou.ie.processchatbot.queryplan.model.SortSpec;
import kr.ac.ajou.ie.processchatbot.queryplan.model.TextFilter;
import kr.ac.ajou.ie.processchatbot.queryplan.model.ValidatedFilters;
import kr.ac.ajou.ie.processchatbot.queryplan.model.ValidatedQueryPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryPlanSqlBuilderTest {

	private QueryPlanSqlBuilder sqlBuilder;

	@BeforeEach
	void setUp() {
		this.sqlBuilder = new QueryPlanSqlBuilder(
			new ChatbotProperties("Asia/Seoul", 500, 5, 50, 31, "/api/tags")
		);
	}

	@Test
	void shouldBuildSelectRowsSql() {
		ValidatedQueryPlan plan = new ValidatedQueryPlan(
			"1.0",
			PlanStatus.READY,
			PlanQueryType.LOOKUP,
			PlanOperation.SELECT_ROWS,
			new ValidatedFilters(new TextFilter("허은택", MatchType.EXACT), null, null, null, null),
			List.of(),
			List.of(),
			List.of(new SortSpec(SortField.WORK_DATE, SortDirection.DESC)),
			5,
			ResponseStyle.NATURAL,
			null,
			null
		);

		SqlQuerySpec spec = this.sqlBuilder.build(plan);

		assertThat(spec.sql()).contains("LOWER(om.operator_name) = LOWER(:operatorName)");
		assertThat(spec.sql()).contains("LIMIT :limit");
		assertThat(spec.params()).containsEntry("operatorName", "허은택");
		assertThat(spec.resultMode()).isEqualTo(SqlResultMode.LOG_ROWS);
	}

	@Test
	void shouldBuildGroupCountSql() {
		ValidatedQueryPlan plan = new ValidatedQueryPlan(
			"1.0",
			PlanStatus.READY,
			PlanQueryType.AGGREGATE,
			PlanOperation.GROUP_COUNT,
			new ValidatedFilters(new TextFilter("허은택", MatchType.EXACT), null, null, null, null),
			List.of(GroupByField.PROCESS_NAME),
			List.of(new MetricSpec(MetricType.COUNT, MetricField.ALL, "logCount")),
			List.of(new SortSpec(SortField.LOG_COUNT, SortDirection.DESC)),
			1,
			ResponseStyle.NATURAL,
			null,
			null
		);

		SqlQuerySpec spec = this.sqlBuilder.build(plan);

		assertThat(spec.sql()).contains("COUNT(*) AS log_count");
		assertThat(spec.sql()).contains("GROUP BY pm.process_name");
		assertThat(spec.sql()).contains("ORDER BY log_count DESC");
		assertThat(spec.resultMode()).isEqualTo(SqlResultMode.GROUP_COUNT_ROWS);
	}
}
