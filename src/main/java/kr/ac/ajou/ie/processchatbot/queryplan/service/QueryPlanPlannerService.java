package kr.ac.ajou.ie.processchatbot.queryplan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import kr.ac.ajou.ie.processchatbot.config.ChatbotProperties;
import kr.ac.ajou.ie.processchatbot.queryplan.dto.QueryPlanPayload;
import kr.ac.ajou.ie.processchatbot.schema.SchemaProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
public class QueryPlanPlannerService {

	private final ChatModel chatModel;
	private final ObjectMapper objectMapper;
	private final SchemaProvider schemaProvider;
	private final ChatbotProperties properties;

	public QueryPlanPlannerService(
		ChatModel chatModel,
		ObjectMapper objectMapper,
		SchemaProvider schemaProvider,
		ChatbotProperties properties
	) {
		this.chatModel = chatModel;
		this.objectMapper = objectMapper;
		this.schemaProvider = schemaProvider;
		this.properties = properties;
	}

	public QueryPlanPayload plan(String question) {
		try {
			ChatClient chatClient = ChatClient.create(this.chatModel);
			String content = chatClient.prompt()
				.system(buildPlannerSystemPrompt())
				.user(buildPlannerUserPrompt(question))
				.call()
				.content();

			if (content == null || content.isBlank()) {
				throw new QueryPlanPlanningException("Planner returned an empty response.");
			}

			return this.objectMapper.readValue(extractJsonObject(content), QueryPlanPayload.class);
		}
		catch (QueryPlanPlanningException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new QueryPlanPlanningException("Failed to generate or parse QueryPlan.", ex);
		}
	}

	private String buildPlannerSystemPrompt() {
		return """
			You are a QueryPlan planner for a semiconductor process-log chatbot.
			Convert the user's question into exactly one QueryPlan JSON object.
			Output JSON only. Do not add explanations, markdown, comments, or code fences.

			Rules:
			- Never generate SQL.
			- Use only the allowed fields and enum values.
			- For general non-database questions, return queryType=GENERAL, operation=NONE, status=READY.
			- If the question is ambiguous, return status=NEEDS_CLARIFICATION.
			- If the current data or allowed operations cannot answer the question, return status=UNSUPPORTED.
			- Fill queryType, operation, filters, groupBy, metrics, sort, and limit only when status=READY.

			Allowed queryType:
			- GENERAL
			- LOOKUP
			- COUNT
			- AGGREGATE
			- SUMMARY

			Allowed operation:
			- NONE
			- SELECT_ROWS
			- COUNT_ROWS
			- GROUP_COUNT
			- SUMMARIZE_ROWS

			Allowed filters:
			- operatorName
			- processName
			- cassetteId
			- workDate
			- dateRange

			Allowed groupBy:
			- OPERATOR_NAME
			- PROCESS_NAME
			- CASSETTE_ID
			- WORK_DATE

			Allowed sort.field:
			- WORK_DATE
			- START_TIME
			- PROCESS_NAME
			- OPERATOR_NAME
			- CASSETTE_ID
			- LOG_COUNT

			Allowed metrics:
			- COUNT with field "*"

			workDate.type:
			- ABSOLUTE
			- RELATIVE

			RELATIVE value:
			- TODAY
			- YESTERDAY

			match:
			- EXACT

			responseStyle:
			- NATURAL
			- BRIEF
			- BULLET

			Current date: %s

			Prefer GROUP_COUNT for questions about "most", "top", "ranking", or "most frequent".
			Prefer COUNT_ROWS or GROUP_COUNT for questions about counts.
			Prefer SUMMARIZE_ROWS for summary requests.

			[Schema Description]
			%s
			""".formatted(
			LocalDate.now(ZoneId.of(this.properties.timezone())),
			this.schemaProvider.buildSchemaDescription()
		);
	}

	private String buildPlannerUserPrompt(String question) {
		return """
			Convert the following user question into one QueryPlan JSON object.

			Question:
			%s

			Example:
			{
			  "version": "1.0",
			  "status": "READY",
			  "queryType": "AGGREGATE",
			  "operation": "GROUP_COUNT",
			  "filters": {
			    "operatorName": { "value": "Heo Euntaek", "match": "EXACT" }
			  },
			  "groupBy": ["PROCESS_NAME"],
			  "metrics": [
			    { "type": "COUNT", "field": "*", "alias": "logCount" }
			  ],
			  "sort": [
			    { "field": "LOG_COUNT", "direction": "DESC" }
			  ],
			  "limit": 1,
			  "responseStyle": "NATURAL",
			  "clarification": null,
			  "unsupportedReason": null
			}
			""".formatted(question);
	}

	private String extractJsonObject(String raw) {
		int start = raw.indexOf('{');
		int end = raw.lastIndexOf('}');
		if (start >= 0 && end > start) {
			return raw.substring(start, end + 1).trim();
		}
		return raw.trim();
	}
}
