package kr.ac.ajou.ie.processchatbot.service.model;

import java.util.List;
import kr.ac.ajou.ie.processchatbot.dto.ProcessLogViewDto;

public final class QueryExecutionResult {

	private final List<ProcessLogViewDto> logs;
	private final Long count;
	private final String referenceSummary;

	private QueryExecutionResult(List<ProcessLogViewDto> logs, Long count, String referenceSummary) {
		this.logs = logs;
		this.count = count;
		this.referenceSummary = referenceSummary;
	}

	public static QueryExecutionResult none() {
		return new QueryExecutionResult(List.of(), null, null);
	}

	public static QueryExecutionResult forLogs(List<ProcessLogViewDto> logs, String referenceSummary) {
		return new QueryExecutionResult(List.copyOf(logs), null, referenceSummary);
	}

	public static QueryExecutionResult forCount(long count, String referenceSummary) {
		return new QueryExecutionResult(List.of(), count, referenceSummary);
	}

	public boolean hasLogs() {
		return !this.logs.isEmpty();
	}

	public boolean hasCount() {
		return this.count != null;
	}

	public boolean isEmpty() {
		return !hasCount() && this.logs.isEmpty();
	}

	public int resultCount() {
		if (this.count != null) {
			return Math.toIntExact(this.count);
		}
		return this.logs.size();
	}

	public List<ProcessLogViewDto> logs() {
		return this.logs;
	}

	public Long count() {
		return this.count;
	}

	public String referenceSummary() {
		return this.referenceSummary;
	}
}
