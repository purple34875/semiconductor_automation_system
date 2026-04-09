package kr.ac.ajou.ie.processchatbot.service;

import kr.ac.ajou.ie.processchatbot.config.ChatbotProperties;
import kr.ac.ajou.ie.processchatbot.dto.ChatRequest;
import kr.ac.ajou.ie.processchatbot.dto.ChatResponse;
import kr.ac.ajou.ie.processchatbot.exception.InvalidQuestionException;
import kr.ac.ajou.ie.processchatbot.llm.LlmService;
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
	private final QueryRoutingService queryRoutingService;
	private final QueryExecutionService queryExecutionService;
	private final LlmService llmService;

	public ChatResponse chat(ChatRequest request) {
		String question = validateQuestion(request.question());
		QueryDecision decision = this.queryRoutingService.route(question);

		log.info("Received question: {}", question);
		log.info("Query type={}, selectedAction={}", decision.queryType(), decision.selectedAction());

		QueryExecutionResult executionResult = QueryExecutionResult.none();
		if (decision.usesDatabase()) {
			executionResult = this.queryExecutionService.execute(decision);
			log.info("DB query result count={}", executionResult.resultCount());
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
