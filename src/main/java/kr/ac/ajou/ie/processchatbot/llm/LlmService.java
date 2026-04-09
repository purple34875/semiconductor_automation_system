package kr.ac.ajou.ie.processchatbot.llm;

import java.net.SocketTimeoutException;
import kr.ac.ajou.ie.processchatbot.exception.LlmServiceException;
import kr.ac.ajou.ie.processchatbot.exception.LlmTimeoutException;
import kr.ac.ajou.ie.processchatbot.formatter.ResponseFormatter;
import kr.ac.ajou.ie.processchatbot.schema.SchemaProvider;
import kr.ac.ajou.ie.processchatbot.service.model.QueryDecision;
import kr.ac.ajou.ie.processchatbot.service.model.QueryExecutionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

@Service
@RequiredArgsConstructor
public class LlmService {

	private final ChatModel chatModel;
	private final SchemaProvider schemaProvider;
	private final ResponseFormatter responseFormatter;

	public String generateAnswer(String question, QueryDecision decision, QueryExecutionResult result) {
		try {
			ChatClient chatClient = ChatClient.create(this.chatModel);
			String content = chatClient.prompt()
				.system(buildSystemPrompt())
				.user(buildUserPrompt(question, decision, result))
				.call()
				.content();

			if (content == null || content.isBlank()) {
				return this.responseFormatter.buildFallbackAnswer(result);
			}

			return content.trim();
		}
		catch (ResourceAccessException ex) {
			if (ex.getCause() instanceof SocketTimeoutException) {
				throw new LlmTimeoutException("현재 LLM 응답 생성에 문제가 발생했습니다.", ex);
			}
			throw new LlmServiceException("현재 LLM 응답 생성에 문제가 발생했습니다.", ex);
		}
		catch (Exception ex) {
			throw new LlmServiceException("현재 LLM 응답 생성에 문제가 발생했습니다.", ex);
		}
	}

	private String buildSystemPrompt() {
		return """
			당신은 반도체 공정 기록을 조회하고 설명하는 사내 QA 챗봇이다.
			아래 규칙을 반드시 지켜라.
			- 존재하지 않는 테이블이나 컬럼을 만들지 마라.
			- 제공된 스키마와 조회 결과만 사용하라.
			- 조회 결과가 없으면 추측하지 말고 없다고 분명히 말하라.
			- DB를 직접 수정하거나 SQL을 임의 생성한다고 말하지 마라.
			- 답변은 한국어로 짧고 명확하게 작성하라.

			[스키마 설명]
			%s
			""".formatted(this.schemaProvider.buildSchemaDescription());
	}

	private String buildUserPrompt(String question, QueryDecision decision, QueryExecutionResult result) {
		StringBuilder builder = new StringBuilder();
		builder.append("[지원 기능 요약]\n")
			.append(this.schemaProvider.buildCapabilitySummary())
			.append("\n\n[사용자 질문]\n")
			.append(question)
			.append("\n\n[질문 유형]\n")
			.append(decision.queryType().name())
			.append("\n\n[선택된 조회 함수]\n")
			.append(decision.selectedAction().name());

		if (decision.usesDatabase()) {
			builder.append("\n\n[DB 조회 결과]\n")
				.append(this.responseFormatter.formatForLlm(decision, result));
		}

		builder.append("""

			[답변 지침]
			- 사용자 질문에 직접 답하라.
			- 여러 건이면 날짜, 작업자명, 공정명, cassette_id를 우선 정리하라.
			- referenceSummary는 사용자에게 직접 노출하지 않아도 된다.
			""");

		return builder.toString();
	}
}
