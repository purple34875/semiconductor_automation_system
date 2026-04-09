package kr.ac.ajou.ie.processchatbot.service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import kr.ac.ajou.ie.processchatbot.config.ChatbotProperties;
import kr.ac.ajou.ie.processchatbot.dto.ProcessLogViewDto;
import kr.ac.ajou.ie.processchatbot.exception.DatabaseConnectionException;
import kr.ac.ajou.ie.processchatbot.exception.InvalidQuestionException;
import kr.ac.ajou.ie.processchatbot.repository.ProcessLogRepository;
import kr.ac.ajou.ie.processchatbot.service.model.ExtractedParameters;
import kr.ac.ajou.ie.processchatbot.service.model.QueryDecision;
import kr.ac.ajou.ie.processchatbot.service.model.QueryExecutionResult;
import kr.ac.ajou.ie.processchatbot.service.model.SelectedAction;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueryExecutionService {

	private final ProcessLogRepository repository;
	private final ChatbotProperties properties;

	public QueryExecutionResult execute(QueryDecision decision) {
		ExtractedParameters parameters = normalize(decision.parameters(), decision.selectedAction());

		try {
			return switch (decision.selectedAction()) {
				case GET_RECENT_LOGS -> logResult(
					this.repository.getRecentLogs(parameters.limit()),
					"Process_Log 최근 %d건 조회".formatted(parameters.limit())
				);
				case GET_LOGS_BY_CASSETTE_ID -> logResult(
					this.repository.getLogsByCassetteId(parameters.cassetteId(), parameters.limit()),
					"cassette_id=%s 조건 조회".formatted(parameters.cassetteId())
				);
				case GET_LOGS_BY_OPERATOR_NAME -> logResult(
					this.repository.getLogsByOperatorName(parameters.operatorName(), parameters.limit()),
					"operator_name=%s 조건 조회".formatted(parameters.operatorName())
				);
				case GET_LOGS_BY_PROCESS_NAME -> logResult(
					this.repository.getLogsByProcessName(parameters.processName(), parameters.limit()),
					"process_name=%s 조건 조회".formatted(parameters.processName())
				);
				case GET_LOGS_BY_WORK_DATE -> logResult(
					this.repository.getLogsByWorkDate(parameters.workDate(), parameters.limit()),
					"work_date=%s 조건 조회".formatted(parameters.workDate())
				);
				case GET_LOGS_BY_DATE_RANGE -> logResult(
					this.repository.getLogsByDateRange(parameters.startDate(), parameters.endDate(), parameters.limit()),
					"work_date BETWEEN %s AND %s 조회".formatted(parameters.startDate(), parameters.endDate())
				);
				case COUNT_LOGS_BY_PROCESS_NAME -> QueryExecutionResult.forCount(
					this.repository.countLogsByProcessName(parameters.processName()),
					"process_name=%s 건수 조회".formatted(parameters.processName())
				);
				case COUNT_LOGS_BY_OPERATOR_NAME -> QueryExecutionResult.forCount(
					this.repository.countLogsByOperatorName(parameters.operatorName()),
					"operator_name=%s 건수 조회".formatted(parameters.operatorName())
				);
				case GET_LOGS_BY_OPERATOR_NAME_AND_DATE -> logResult(
					this.repository.getLogsByOperatorNameAndDate(parameters.operatorName(), parameters.workDate(), parameters.limit()),
					"operator_name=%s AND work_date=%s 조회".formatted(parameters.operatorName(), parameters.workDate())
				);
				case GET_LOGS_BY_PROCESS_NAME_AND_DATE -> logResult(
					this.repository.getLogsByProcessNameAndDate(parameters.processName(), parameters.workDate(), parameters.limit()),
					"process_name=%s AND work_date=%s 조회".formatted(parameters.processName(), parameters.workDate())
				);
				case COUNT_LOGS_BY_WORK_DATE -> QueryExecutionResult.forCount(
					this.repository.countLogsByWorkDate(parameters.workDate()),
					"work_date=%s 건수 조회".formatted(parameters.workDate())
				);
				case COUNT_LOGS_BY_DATE_RANGE -> QueryExecutionResult.forCount(
					this.repository.countLogsByDateRange(parameters.startDate(), parameters.endDate()),
					"work_date BETWEEN %s AND %s 건수 조회".formatted(parameters.startDate(), parameters.endDate())
				);
				case NONE -> QueryExecutionResult.none();
			};
		}
		catch (DataAccessException ex) {
			throw new DatabaseConnectionException("DB 연결에 실패했습니다.", ex);
		}
	}

	private QueryExecutionResult logResult(List<ProcessLogViewDto> logs, String referenceSummary) {
		return QueryExecutionResult.forLogs(logs, referenceSummary);
	}

	private ExtractedParameters normalize(ExtractedParameters parameters, SelectedAction selectedAction) {
		Integer limit = parameters.limit();
		if (selectedAction == SelectedAction.GET_RECENT_LOGS && limit == null) {
			limit = this.properties.defaultLimit();
		}

		if (limit != null && (limit <= 0 || limit > this.properties.maxLimit())) {
			throw new InvalidQuestionException("조회 건수는 1 이상 %d 이하로 요청해야 합니다.".formatted(this.properties.maxLimit()));
		}

		if (parameters.hasDateRange()) {
			if (parameters.startDate().isAfter(parameters.endDate())) {
				throw new InvalidQuestionException("날짜 범위를 해석하지 못했습니다.");
			}

			long days = ChronoUnit.DAYS.between(parameters.startDate(), parameters.endDate()) + 1;
			if (days > this.properties.maxDateRangeDays()) {
				throw new InvalidQuestionException("날짜 범위는 최대 %d일까지 조회할 수 있습니다.".formatted(this.properties.maxDateRangeDays()));
			}
		}

		return new ExtractedParameters(
			parameters.operatorName(),
			parameters.processName(),
			parameters.cassetteId(),
			parameters.workDate(),
			parameters.startDate(),
			parameters.endDate(),
			limit,
			parameters.countQuery(),
			parameters.recentQuery()
		);
	}
}
