package kr.ac.ajou.ie.processchatbot.formatter;

import java.util.Map;
import java.util.stream.Collectors;
import kr.ac.ajou.ie.processchatbot.dto.GroupedCountViewDto;
import kr.ac.ajou.ie.processchatbot.dto.ProcessLogViewDto;
import kr.ac.ajou.ie.processchatbot.service.model.QueryDecision;
import kr.ac.ajou.ie.processchatbot.service.model.QueryExecutionResult;
import org.springframework.stereotype.Component;

@Component
public class ResponseFormatter {

	public String formatForLlm(QueryDecision decision, QueryExecutionResult result) {
		return formatForLlm(decision.selectedAction().name(), result);
	}

	public String formatForLlm(String executionLabel, QueryExecutionResult result) {
		if (result.hasCount()) {
			return """
				{
				  "type": "count_result",
				  "executionLabel": "%s",
				  "count": %d,
				  "referenceSummary": "%s"
				}
				""".formatted(
				executionLabel,
				result.count(),
				nullSafe(result.referenceSummary())
			);
		}

		if (result.hasGroupedCounts()) {
			String items = result.groupedCounts()
				.stream()
				.map(this::toJsonLikeLine)
				.collect(Collectors.joining(",\n"));

			return """
				{
				  "type": "group_count_result",
				  "executionLabel": "%s",
				  "resultCount": %d,
				  "referenceSummary": "%s",
				  "items": [
				%s
				  ]
				}
				""".formatted(
				executionLabel,
				result.resultCount(),
				nullSafe(result.referenceSummary()),
				items
			);
		}

		String items = result.logs()
			.stream()
			.map(this::toJsonLikeLine)
			.collect(Collectors.joining(",\n"));

		return """
			{
			  "type": "log_result",
			  "executionLabel": "%s",
			  "resultCount": %d,
			  "referenceSummary": "%s",
			  "items": [
			%s
			  ]
			}
			""".formatted(
			executionLabel,
			result.resultCount(),
			nullSafe(result.referenceSummary()),
			items
		);
	}

	public String buildFallbackAnswer(QueryExecutionResult result) {
		if (!result.hasCount() && !result.hasGroupedCounts() && result.logs().isEmpty()) {
			return "현재는 작업 기록 조회, 집계 결과 확인, 건수 조회, 요약 응답을 지원합니다.";
		}

		if (result.hasCount()) {
			return "조건에 맞는 로그 건수는 %d건입니다.".formatted(result.count());
		}

		if (result.hasGroupedCounts()) {
			GroupedCountViewDto top = result.groupedCounts().get(0);
			String groups = top.groupValues()
				.entrySet()
				.stream()
				.map(entry -> entry.getKey() + "=" + entry.getValue())
				.collect(Collectors.joining(", "));
			return "가장 높은 집계 결과는 %s (%d건)입니다.".formatted(groups, top.count());
		}

		String summary = result.logs()
			.stream()
			.limit(5)
			.map(log -> "%s %s %s %s".formatted(
				log.workDate(),
				log.cassetteId(),
				log.processName(),
				log.operatorName()
			))
			.collect(Collectors.joining("; "));

		return "조회 결과는 다음과 같습니다: %s".formatted(summary);
	}

	private String toJsonLikeLine(GroupedCountViewDto dto) {
		String groupValues = dto.groupValues()
			.entrySet()
			.stream()
			.map(this::toJsonLikeEntry)
			.collect(Collectors.joining(",\n"));

		return """
			    {
			      "groupValues": {
			%s
			      },
			      "count": %d
			    }""".formatted(groupValues, dto.count());
	}

	private String toJsonLikeEntry(Map.Entry<String, String> entry) {
		return "        \"%s\": \"%s\"".formatted(
			entry.getKey(),
			nullSafe(entry.getValue())
		);
	}

	private String toJsonLikeLine(ProcessLogViewDto dto) {
		return """
			    {
			      "workDate": "%s",
			      "cassetteId": "%s",
			      "processName": "%s",
			      "operatorName": "%s",
			      "startTime": "%s",
			      "endTime": "%s",
			      "remarks": "%s"
			    }""".formatted(
			dto.workDate(),
			nullSafe(dto.cassetteId()),
			nullSafe(dto.processName()),
			nullSafe(dto.operatorName()),
			nullSafe(dto.startTime()),
			nullSafe(dto.endTime()),
			nullSafe(dto.remarks())
		);
	}

	private String nullSafe(Object value) {
		return value == null ? "" : String.valueOf(value);
	}
}
