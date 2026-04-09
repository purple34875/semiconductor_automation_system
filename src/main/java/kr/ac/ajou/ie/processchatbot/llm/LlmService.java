package kr.ac.ajou.ie.processchatbot.llm;

import java.net.SocketTimeoutException;
import kr.ac.ajou.ie.processchatbot.exception.LlmServiceException;
import kr.ac.ajou.ie.processchatbot.exception.LlmTimeoutException;
import kr.ac.ajou.ie.processchatbot.formatter.ResponseFormatter;
import kr.ac.ajou.ie.processchatbot.queryplan.model.ValidatedQueryPlan;
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
		return generateAnswer(question, decision.queryType().name(), decision.selectedAction().name(), result);
	}

	public String generateAnswer(String question, ValidatedQueryPlan plan, QueryExecutionResult result) {
		return generateAnswer(question, plan.queryType().name(), plan.operation().name(), result);
	}

	private String generateAnswer(String question, String queryType, String executionLabel, QueryExecutionResult result) {
		try {
			ChatClient chatClient = ChatClient.create(this.chatModel);
			String content = chatClient.prompt()
				.system(buildSystemPrompt())
				.user(buildUserPrompt(question, queryType, executionLabel, result))
				.call()
				.content();

			if (content == null || content.isBlank()) {
				return this.responseFormatter.buildFallbackAnswer(result);
			}

			return content.trim();
		}
		catch (ResourceAccessException ex) {
			if (ex.getCause() instanceof SocketTimeoutException) {
				throw new LlmTimeoutException("Timed out while generating the LLM response.", ex);
			}
			throw new LlmServiceException("Failed to generate the LLM response.", ex);
		}
		catch (Exception ex) {
			throw new LlmServiceException("Failed to generate the LLM response.", ex);
		}
	}

	private String buildSystemPrompt() {
		return """
			You are an internal QA chatbot for semiconductor process logs.
			Follow these rules:
			- Do not invent tables or columns that do not exist.
			- Use only the provided schema and database results.
			- If no data was found, say so clearly and do not guess.
			- Do not claim that you executed SQL yourself.
			- Answer clearly and concisely in Korean.

			[Schema Description]
			%s
			""".formatted(this.schemaProvider.buildSchemaDescription());
	}

	private String buildUserPrompt(String question, String queryType, String executionLabel, QueryExecutionResult result) {
		StringBuilder builder = new StringBuilder();
		builder.append("[Capability Summary]\n")
			.append(this.schemaProvider.buildCapabilitySummary())
			.append("\n\n[User Question]\n")
			.append(question)
			.append("\n\n[Query Type]\n")
			.append(queryType)
			.append("\n\n[Execution Plan]\n")
			.append(executionLabel);

		if (!result.isEmpty()) {
			builder.append("\n\n[Database Result]\n")
				.append(this.responseFormatter.formatForLlm(executionLabel, result));
		}

		builder.append("""

			[Answer Instructions]
			- Answer the user's question directly in Korean.
			- When there are multiple rows, summarize by date, operator, process, and cassette_id first.
			- Do not expose referenceSummary directly to the user.
			""");

		return builder.toString();
	}
}
