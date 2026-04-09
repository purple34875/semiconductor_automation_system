package kr.ac.ajou.ie.processchatbot.queryplan.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import kr.ac.ajou.ie.processchatbot.config.ChatbotProperties;
import kr.ac.ajou.ie.processchatbot.queryplan.dto.ClarificationPayload;
import kr.ac.ajou.ie.processchatbot.queryplan.dto.MetricPayload;
import kr.ac.ajou.ie.processchatbot.queryplan.dto.QueryFiltersPayload;
import kr.ac.ajou.ie.processchatbot.queryplan.dto.QueryPlanPayload;
import kr.ac.ajou.ie.processchatbot.queryplan.dto.SortPayload;
import kr.ac.ajou.ie.processchatbot.queryplan.dto.TextFilterPayload;
import kr.ac.ajou.ie.processchatbot.queryplan.model.GroupByField;
import kr.ac.ajou.ie.processchatbot.queryplan.model.PlanOperation;
import kr.ac.ajou.ie.processchatbot.queryplan.model.PlanQueryType;
import kr.ac.ajou.ie.processchatbot.queryplan.model.PlanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryPlanValidatorTest {

	private QueryPlanValidator validator;

	@BeforeEach
	void setUp() {
		this.validator = new QueryPlanValidator(
			new ChatbotProperties("Asia/Seoul", 500, 5, 50, 31, "/api/tags")
		);
	}

	@Test
	void shouldValidateGeneralPlan() {
		QueryPlanPayload payload = new QueryPlanPayload(
			"1.0",
			"READY",
			"GENERAL",
			"NONE",
			null,
			List.of(),
			List.of(),
			List.of(),
			null,
			"NATURAL",
			null,
			null
		);

		var plan = this.validator.validate(payload);

		assertThat(plan.status()).isEqualTo(PlanStatus.READY);
		assertThat(plan.queryType()).isEqualTo(PlanQueryType.GENERAL);
		assertThat(plan.operation()).isEqualTo(PlanOperation.NONE);
		assertThat(plan.usesDatabase()).isFalse();
	}

	@Test
	void shouldValidateGroupCountPlan() {
		QueryPlanPayload payload = new QueryPlanPayload(
			"1.0",
			"READY",
			"AGGREGATE",
			"GROUP_COUNT",
			new QueryFiltersPayload(
				new TextFilterPayload("허은택", "EXACT"),
				null,
				null,
				null,
				null
			),
			List.of("PROCESS_NAME"),
			List.of(new MetricPayload("COUNT", "*", "logCount")),
			List.of(new SortPayload("LOG_COUNT", "DESC")),
			1,
			"NATURAL",
			null,
			null
		);

		var plan = this.validator.validate(payload);

		assertThat(plan.status()).isEqualTo(PlanStatus.READY);
		assertThat(plan.groupBy()).containsExactly(GroupByField.PROCESS_NAME);
		assertThat(plan.limit()).isEqualTo(1);
	}

	@Test
	void shouldValidateClarificationPlan() {
		QueryPlanPayload payload = new QueryPlanPayload(
			"1.0",
			"NEEDS_CLARIFICATION",
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			new ClarificationPayload("작업자명이나 공정명을 알려주세요.", List.of("operatorName", "processName")),
			null
		);

		var plan = this.validator.validate(payload);

		assertThat(plan.status()).isEqualTo(PlanStatus.NEEDS_CLARIFICATION);
		assertThat(plan.clarification().message()).contains("작업자명");
	}

	@Test
	void shouldRejectInvalidGroupCountWithoutMetric() {
		QueryPlanPayload payload = new QueryPlanPayload(
			"1.0",
			"READY",
			"AGGREGATE",
			"GROUP_COUNT",
			null,
			List.of("PROCESS_NAME"),
			List.of(),
			List.of(),
			5,
			"NATURAL",
			null,
			null
		);

		assertThatThrownBy(() -> this.validator.validate(payload))
			.isInstanceOf(QueryPlanValidationException.class);
	}
}
