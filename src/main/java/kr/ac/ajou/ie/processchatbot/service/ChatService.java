package kr.ac.ajou.ie.processchatbot.service;

import kr.ac.ajou.ie.processchatbot.config.ChatbotProperties;
import kr.ac.ajou.ie.processchatbot.dto.ChatRequest;
import kr.ac.ajou.ie.processchatbot.dto.ChatResponse;
import kr.ac.ajou.ie.processchatbot.exception.InvalidQuestionException;
import kr.ac.ajou.ie.processchatbot.llm.LlmService;
import kr.ac.ajou.ie.processchatbot.queryplan.dto.QueryPlanPayload;
import kr.ac.ajou.ie.processchatbot.queryplan.model.PlanStatus;
import kr.ac.ajou.ie.processchatbot.queryplan.model.ValidatedQueryPlan;
import kr.ac.ajou.ie.processchatbot.queryplan.service.QueryPlanExecutor;
import kr.ac.ajou.ie.processchatbot.queryplan.service.QueryPlanPlanningException;
import kr.ac.ajou.ie.processchatbot.queryplan.service.QueryPlanPlannerService;
import kr.ac.ajou.ie.processchatbot.queryplan.validation.QueryPlanValidationException;
import kr.ac.ajou.ie.processchatbot.queryplan.validation.QueryPlanValidator;
import kr.ac.ajou.ie.processchatbot.service.model.QueryDecision;
import kr.ac.ajou.ie.processchatbot.service.model.QueryExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

	private final ChatbotProperties properties;
	private final QueryPlanPlannerService queryPlanPlannerService;
	private final QueryPlanValidator queryPlanValidator;
	private final QueryPlanExecutor queryPlanExecutor;
	private final QueryRoutingService queryRoutingService;
	private final QueryExecutionService queryExecutionService;
	private final LlmService llmService;

	public ChatResponse chat(ChatRequest request) {
		String question = validateQuestion(request.question());
		log.info("Received question: {}", question);

		try {
			return chatWithQueryPlan(question);
		}
		catch (QueryPlanPlanningException | QueryPlanValidationException ex) {
			log.warn("Falling back to legacy routing: {}", ex.getMessage());
			return chatWithLegacyRouting(question);
		}
	}

	private ChatResponse chatWithQueryPlan(String question) {
		QueryPlanPayload payload = this.queryPlanPlannerService.plan(question);
		ValidatedQueryPlan plan = this.queryPlanValidator.validate(payload);
		log.info("Planned queryType={}, operation={}, status={}", plan.queryType(), plan.operation(), plan.status());

		if (plan.status() == PlanStatus.NEEDS_CLARIFICATION) {
			return new ChatResponse(
				plan.clarification().message(),
				false,
				plan.queryType().name(),
				plan.status().name(),
				0,
				null
			);
		}

		if (plan.status() == PlanStatus.UNSUPPORTED) {
			return new ChatResponse(
				plan.unsupportedReason(),
				false,
				plan.queryType().name(),
				plan.status().name(),
				0,
				null
			);
		}

		QueryExecutionResult executionResult = QueryExecutionResult.none();
		if (plan.usesDatabase()) {
			executionResult = this.queryPlanExecutor.execute(plan);
			log.info("QueryPlan DB result count={}", executionResult.resultCount());
		}

		if (plan.usesDatabase() && executionResult.isEmpty()) {
			return new ChatResponse(
				"조건에 맞는 데이터가 없습니다.",
				true,
				plan.queryType().name(),
				plan.operation().name(),
				0,
				executionResult.referenceSummary()
			);
		}

		String answer = this.llmService.generateAnswer(question, plan, executionResult);
		return new ChatResponse(
			answer,
			plan.usesDatabase(),
			plan.queryType().name(),
			plan.operation().name(),
			executionResult.resultCount(),
			executionResult.referenceSummary()
		);
	}

	private ChatResponse chatWithLegacyRouting(String question) {
		QueryDecision decision = this.queryRoutingService.route(question);
		log.info("Legacy query type={}, selectedAction={}", decision.queryType(), decision.selectedAction());

		QueryExecutionResult executionResult = QueryExecutionResult.none();
		if (decision.usesDatabase()) {
			executionResult = this.queryExecutionService.execute(decision);
			log.info("Legacy DB query result count={}", executionResult.resultCount());
		}

		if (decision.usesDatabase() && executionResult.isEmpty()) {
			return new ChatResponse(
				"조건에 맞는 데이터가 없습니다.",
				true,
				decision.queryType().name(),
				decision.selectedAction().name(),
				0,
				executionResult.referenceSummary()
			);
		}

		String answer = this.llmService.generateAnswer(question, decision, executionResult);
		return new ChatResponse(
			answer,
			decision.usesDatabase(),
			decision.queryType().name(),
			decision.selectedAction().name(),
			executionResult.resultCount(),
			executionResult.referenceSummary()
		);
	}

	private String validateQuestion(String question) {
		if (question == null || question.isBlank()) {
			throw new InvalidQuestionException("질문을 해석하지 못했습니다.");
		}
		if (question.length() > this.properties.maxQuestionLength()) {
			throw new InvalidQuestionException("질문 길이는 최대 %d자까지 허용됩니다.".formatted(this.properties.maxQuestionLength()));
		}
		return question.trim();
	}
}
