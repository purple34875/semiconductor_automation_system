package kr.ac.ajou.ie.processchatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kr.ac.ajou.ie.processchatbot.config.ChatbotProperties;
import kr.ac.ajou.ie.processchatbot.exception.AmbiguousQuestionException;
import kr.ac.ajou.ie.processchatbot.service.model.QueryType;
import kr.ac.ajou.ie.processchatbot.service.model.SelectedAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryRoutingServiceTest {

	private QueryRoutingService queryRoutingService;

	@BeforeEach
	void setUp() {
		this.queryRoutingService = new QueryRoutingService(
			new ChatbotProperties("Asia/Seoul", 500, 5, 50, 31, "/api/tags")
		);
	}

	@Test
	void shouldRouteRecentLogQuestion() {
		var decision = this.queryRoutingService.route("최근 작업 기록 5건 보여줘");

		assertThat(decision.queryType()).isEqualTo(QueryType.DB_LOOKUP);
		assertThat(decision.selectedAction()).isEqualTo(SelectedAction.GET_RECENT_LOGS);
		assertThat(decision.parameters().limit()).isEqualTo(5);
	}

	@Test
	void shouldRouteCountByDateQuestion() {
		var decision = this.queryRoutingService.route("오늘 작업 로그 건수 알려줘");

		assertThat(decision.selectedAction()).isEqualTo(SelectedAction.COUNT_LOGS_BY_WORK_DATE);
		assertThat(decision.parameters().workDate()).isNotNull();
	}

	@Test
	void shouldRouteGeneralQuestion() {
		var decision = this.queryRoutingService.route("이 시스템은 어떤 기능을 제공해?");

		assertThat(decision.queryType()).isEqualTo(QueryType.GENERAL);
		assertThat(decision.selectedAction()).isEqualTo(SelectedAction.NONE);
	}

	@Test
	void shouldRejectAmbiguousLookupQuestion() {
		assertThatThrownBy(() -> this.queryRoutingService.route("작업 기록 알려줘"))
			.isInstanceOf(AmbiguousQuestionException.class);
	}
}
