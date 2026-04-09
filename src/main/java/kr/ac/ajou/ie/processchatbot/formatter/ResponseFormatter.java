package kr.ac.ajou.ie.processchatbot.formatter;

import java.util.stream.Collectors;
import kr.ac.ajou.ie.processchatbot.dto.ProcessLogViewDto;
import kr.ac.ajou.ie.processchatbot.service.model.QueryDecision;
import kr.ac.ajou.ie.processchatbot.service.model.QueryExecutionResult;
import org.springframework.stereotype.Component;

@Component
public class ResponseFormatter {

	public String formatForLlm(QueryDecision decision, QueryExecutionResult result) {
		if (result.hasCount()) {
			return """
				{
				  "type": "count_result",
				  "selectedAction": "%s",
				  "count": %d,
				  "referenceSummary": "%s"
				}
				""".formatted(
				decision.selectedAction().name(),
				result.count(),
				nullSafe(result.referenceSummary())
			);
		}

		String items = result.logs()
			.stream()
			.map(this::toJsonLikeLine)
			.collect(Collectors.joining(",\n"));

		return """
			{
			  "type": "log_result",
			  "selectedAction": "%s",
			  "resultCount": %d,
			  "referenceSummary": "%s",
			  "items": [
			%s
			  ]
			}
			""".formatted(
			decision.selectedAction().name(),
			result.resultCount(),
			nullSafe(result.referenceSummary()),
			items
		);
	}

	public String buildFallbackAnswer(QueryExecutionResult result) {
		if (!result.hasCount() && result.logs().isEmpty()) {
			return "이 시스템은 작업 기록 조회, 조건별 검색, 건수 조회, 요약 응답을 지원합니다.";
		}

		if (result.hasCount()) {
			return "조건에 맞는 로그 건수는 %d건입니다.".formatted(result.count());
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
