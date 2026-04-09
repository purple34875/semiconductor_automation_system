package kr.ac.ajou.ie.processchatbot.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kr.ac.ajou.ie.processchatbot.config.ChatbotProperties;
import kr.ac.ajou.ie.processchatbot.exception.AmbiguousQuestionException;
import kr.ac.ajou.ie.processchatbot.exception.InvalidQuestionException;
import kr.ac.ajou.ie.processchatbot.service.model.ExtractedParameters;
import kr.ac.ajou.ie.processchatbot.service.model.QueryDecision;
import kr.ac.ajou.ie.processchatbot.service.model.QueryType;
import kr.ac.ajou.ie.processchatbot.service.model.SelectedAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueryRoutingService {

	private static final Pattern CASSETTE_PATTERN = Pattern.compile("(?i)\\bcassette\\s*([A-Za-z0-9_-]+)");
	private static final Pattern OPERATOR_PATTERN = Pattern.compile("([가-힣A-Za-z][가-힣A-Za-z0-9_-]{0,49})\\s*(?:작업자|담당자)");
	private static final Pattern PROCESS_PATTERN = Pattern.compile("([가-힣A-Za-z][가-힣A-Za-z0-9_-]{0,49})\\s*공정");
	private static final Pattern LIMIT_PATTERN = Pattern.compile("(\\d+)\\s*건");
	private static final Pattern RANGE_PATTERN = Pattern.compile(
		"(\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2})\\s*(?:부터|~)\\s*(\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2})(?:\\s*까지)?"
	);
	private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}");
	private static final Set<String> ENTITY_STOPWORDS = Set.of("오늘", "어제", "최근", "이번", "현재", "전체", "작업", "로그");

	private final ChatbotProperties properties;

	public QueryDecision route(String question) {
		String normalizedQuestion = normalize(question);
		boolean summaryQuery = containsAny(normalizedQuestion, "요약", "정리");
		ExtractedParameters parameters = extractParameters(normalizedQuestion);
		boolean dbHint = isDbHint(normalizedQuestion, parameters);
		QueryType queryType = determineQueryType(summaryQuery, dbHint);
		SelectedAction selectedAction = determineAction(queryType, normalizedQuestion, parameters);
		return new QueryDecision(queryType, selectedAction, parameters, normalizedQuestion);
	}

	private String normalize(String question) {
		if (question == null || question.isBlank()) {
			throw new InvalidQuestionException("질문을 해석하지 못했습니다.");
		}
		return question.trim().replaceAll("\\s+", " ");
	}

	private ExtractedParameters extractParameters(String question) {
		boolean countQuery = containsAny(question, "건수", "몇 건", "몇건", "count");
		boolean recentQuery = question.contains("최근");
		Integer limit = extractLimit(question);
		if (recentQuery && limit == null) {
			limit = this.properties.defaultLimit();
		}

		LocalDate workDate = extractRelativeDate(question);
		LocalDate startDate = null;
		LocalDate endDate = null;

		Matcher rangeMatcher = RANGE_PATTERN.matcher(question);
		if (rangeMatcher.find()) {
			startDate = parseDate(rangeMatcher.group(1));
			endDate = parseDate(rangeMatcher.group(2));
			workDate = null;
		}
		else {
			List<String> explicitDates = DATE_PATTERN.matcher(question)
				.results()
				.map(MatchResult::group)
				.toList();
			if (!explicitDates.isEmpty()) {
				workDate = parseDate(explicitDates.get(0));
			}
		}

		return new ExtractedParameters(
			extractEntity(OPERATOR_PATTERN, question),
			extractEntity(PROCESS_PATTERN, question),
			extractCassetteId(question),
			workDate,
			startDate,
			endDate,
			limit,
			countQuery,
			recentQuery
		);
	}

	private QueryType determineQueryType(boolean summaryQuery, boolean dbHint) {
		if (summaryQuery) {
			return QueryType.SUMMARY;
		}
		if (dbHint) {
			return QueryType.DB_LOOKUP;
		}
		return QueryType.GENERAL;
	}

	private SelectedAction determineAction(QueryType queryType, String question, ExtractedParameters parameters) {
		if (queryType == QueryType.GENERAL) {
			return SelectedAction.NONE;
		}

		if (parameters.countQuery()) {
			if (parameters.processName() != null) {
				return SelectedAction.COUNT_LOGS_BY_PROCESS_NAME;
			}
			if (parameters.operatorName() != null) {
				return SelectedAction.COUNT_LOGS_BY_OPERATOR_NAME;
			}
			if (parameters.hasDateRange()) {
				return SelectedAction.COUNT_LOGS_BY_DATE_RANGE;
			}
			if (parameters.workDate() != null) {
				return SelectedAction.COUNT_LOGS_BY_WORK_DATE;
			}
			throw ambiguousQuestion();
		}

		if (parameters.operatorName() != null && parameters.workDate() != null) {
			return SelectedAction.GET_LOGS_BY_OPERATOR_NAME_AND_DATE;
		}
		if (parameters.processName() != null && parameters.workDate() != null) {
			return SelectedAction.GET_LOGS_BY_PROCESS_NAME_AND_DATE;
		}
		if (parameters.cassetteId() != null) {
			return SelectedAction.GET_LOGS_BY_CASSETTE_ID;
		}
		if (parameters.hasDateRange()) {
			return SelectedAction.GET_LOGS_BY_DATE_RANGE;
		}
		if (parameters.operatorName() != null) {
			return SelectedAction.GET_LOGS_BY_OPERATOR_NAME;
		}
		if (parameters.processName() != null) {
			return SelectedAction.GET_LOGS_BY_PROCESS_NAME;
		}
		if (parameters.workDate() != null) {
			return SelectedAction.GET_LOGS_BY_WORK_DATE;
		}
		if (parameters.recentQuery() || parameters.limit() != null) {
			return SelectedAction.GET_RECENT_LOGS;
		}

		throw ambiguousQuestion();
	}

	private boolean isDbHint(String question, ExtractedParameters parameters) {
		return parameters.hasLookupTarget()
			|| parameters.countQuery()
			|| parameters.recentQuery()
			|| containsAny(question, "작업", "기록", "로그", "이력", "내역", "공정", "작업자", "cassette", "날짜", "오늘", "어제");
	}

	private String extractCassetteId(String question) {
		Matcher matcher = CASSETTE_PATTERN.matcher(question);
		if (matcher.find()) {
			return matcher.group(1).trim().toUpperCase(Locale.ROOT);
		}
		return null;
	}

	private String extractEntity(Pattern pattern, String question) {
		Matcher matcher = pattern.matcher(question);
		while (matcher.find()) {
			String candidate = matcher.group(1).trim();
			if (!ENTITY_STOPWORDS.contains(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private Integer extractLimit(String question) {
		Matcher matcher = LIMIT_PATTERN.matcher(question);
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		}
		return null;
	}

	private LocalDate extractRelativeDate(String question) {
		LocalDate today = LocalDate.now(ZoneId.of(this.properties.timezone()));
		if (question.contains("오늘")) {
			return today;
		}
		if (question.contains("어제")) {
			return today.minusDays(1);
		}
		return null;
	}

	private LocalDate parseDate(String rawDate) {
		String normalized = rawDate.replace('.', '-').replace('/', '-');
		String[] tokens = normalized.split("-");
		if (tokens.length != 3) {
			throw new InvalidQuestionException("날짜를 해석하지 못했습니다.");
		}
		return LocalDate.of(
			Integer.parseInt(tokens[0]),
			Integer.parseInt(tokens[1]),
			Integer.parseInt(tokens[2])
		);
	}

	private AmbiguousQuestionException ambiguousQuestion() {
		return new AmbiguousQuestionException("질문을 처리하려면 작업자명, 공정명, cassette ID, 날짜 조건 중 하나 이상이 필요합니다.");
	}

	private boolean containsAny(String source, String... keywords) {
		for (String keyword : keywords) {
			if (source.contains(keyword)) {
				return true;
			}
		}
		return false;
	}
}
