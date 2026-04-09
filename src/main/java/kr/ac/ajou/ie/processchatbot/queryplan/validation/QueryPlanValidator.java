package kr.ac.ajou.ie.processchatbot.queryplan.validation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import kr.ac.ajou.ie.processchatbot.config.ChatbotProperties;
import kr.ac.ajou.ie.processchatbot.queryplan.dto.ClarificationPayload;
import kr.ac.ajou.ie.processchatbot.queryplan.dto.DateRangeFilterPayload;
import kr.ac.ajou.ie.processchatbot.queryplan.dto.MetricPayload;
import kr.ac.ajou.ie.processchatbot.queryplan.dto.QueryFiltersPayload;
import kr.ac.ajou.ie.processchatbot.queryplan.dto.QueryPlanPayload;
import kr.ac.ajou.ie.processchatbot.queryplan.dto.SingleDateFilterPayload;
import kr.ac.ajou.ie.processchatbot.queryplan.dto.SortPayload;
import kr.ac.ajou.ie.processchatbot.queryplan.dto.TextFilterPayload;
import kr.ac.ajou.ie.processchatbot.queryplan.model.ClarificationInfo;
import kr.ac.ajou.ie.processchatbot.queryplan.model.DateFilter;
import kr.ac.ajou.ie.processchatbot.queryplan.model.DateRange;
import kr.ac.ajou.ie.processchatbot.queryplan.model.DateValueType;
import kr.ac.ajou.ie.processchatbot.queryplan.model.GroupByField;
import kr.ac.ajou.ie.processchatbot.queryplan.model.MatchType;
import kr.ac.ajou.ie.processchatbot.queryplan.model.MetricField;
import kr.ac.ajou.ie.processchatbot.queryplan.model.MetricSpec;
import kr.ac.ajou.ie.processchatbot.queryplan.model.MetricType;
import kr.ac.ajou.ie.processchatbot.queryplan.model.PlanOperation;
import kr.ac.ajou.ie.processchatbot.queryplan.model.PlanQueryType;
import kr.ac.ajou.ie.processchatbot.queryplan.model.PlanStatus;
import kr.ac.ajou.ie.processchatbot.queryplan.model.ResponseStyle;
import kr.ac.ajou.ie.processchatbot.queryplan.model.SortDirection;
import kr.ac.ajou.ie.processchatbot.queryplan.model.SortField;
import kr.ac.ajou.ie.processchatbot.queryplan.model.SortSpec;
import kr.ac.ajou.ie.processchatbot.queryplan.model.TextFilter;
import kr.ac.ajou.ie.processchatbot.queryplan.model.ValidatedFilters;
import kr.ac.ajou.ie.processchatbot.queryplan.model.ValidatedQueryPlan;
import org.springframework.stereotype.Component;

@Component
public class QueryPlanValidator {

	private static final Set<String> RELATIVE_DATES = Set.of("TODAY", "YESTERDAY");

	private final ChatbotProperties properties;

	public QueryPlanValidator(ChatbotProperties properties) {
		this.properties = properties;
	}

	public ValidatedQueryPlan validate(QueryPlanPayload payload) {
		if (payload == null) {
			throw invalid("QueryPlan payload is null.");
		}

		PlanStatus status = parseEnum(payload.status(), PlanStatus.class, "status");
		String version = defaultIfBlank(payload.version(), "1.0");

		return switch (status) {
			case READY -> validateReady(version, payload);
			case NEEDS_CLARIFICATION -> validateClarification(version, payload.clarification());
			case UNSUPPORTED -> validateUnsupported(version, payload.unsupportedReason());
		};
	}

	private ValidatedQueryPlan validateReady(String version, QueryPlanPayload payload) {
		PlanQueryType queryType = parseEnum(payload.queryType(), PlanQueryType.class, "queryType");
		PlanOperation operation = parseEnum(payload.operation(), PlanOperation.class, "operation");
		ValidatedFilters filters = validateFilters(payload.filters());
		List<GroupByField> groupBy = validateGroupBy(payload.groupBy());
		List<MetricSpec> metrics = validateMetrics(payload.metrics());
		List<SortSpec> sort = validateSort(payload.sort());
		ResponseStyle responseStyle = parseOptionalEnum(payload.responseStyle(), ResponseStyle.class, ResponseStyle.NATURAL);
		Integer limit = validateLimit(payload.limit(), operation);

		validateOperationRules(queryType, operation, filters, groupBy, metrics, sort, limit);

		return new ValidatedQueryPlan(
			version,
			PlanStatus.READY,
			queryType,
			operation,
			filters,
			groupBy,
			metrics,
			sort,
			limit,
			responseStyle,
			null,
			null
		);
	}

	private ValidatedQueryPlan validateClarification(String version, ClarificationPayload clarification) {
		if (clarification == null || isBlank(clarification.message())) {
			throw invalid("clarification.message is required when status is NEEDS_CLARIFICATION.");
		}

		return new ValidatedQueryPlan(
			version,
			PlanStatus.NEEDS_CLARIFICATION,
			PlanQueryType.LOOKUP,
			PlanOperation.NONE,
			emptyFilters(),
			List.of(),
			List.of(),
			List.of(),
			null,
			ResponseStyle.NATURAL,
			new ClarificationInfo(
				normalizeText(clarification.message()),
				clarification.missingFields() == null ? List.of() : List.copyOf(clarification.missingFields())
			),
			null
		);
	}

	private ValidatedQueryPlan validateUnsupported(String version, String unsupportedReason) {
		if (isBlank(unsupportedReason)) {
			throw invalid("unsupportedReason is required when status is UNSUPPORTED.");
		}

		return new ValidatedQueryPlan(
			version,
			PlanStatus.UNSUPPORTED,
			PlanQueryType.SUMMARY,
			PlanOperation.NONE,
			emptyFilters(),
			List.of(),
			List.of(),
			List.of(),
			null,
			ResponseStyle.NATURAL,
			null,
			normalizeText(unsupportedReason)
		);
	}

	private ValidatedFilters validateFilters(QueryFiltersPayload filters) {
		if (filters == null) {
			return emptyFilters();
		}

		TextFilter operatorName = validateTextFilter(filters.operatorName(), false);
		TextFilter processName = validateTextFilter(filters.processName(), false);
		TextFilter cassetteId = validateTextFilter(filters.cassetteId(), true);
		DateFilter workDate = validateWorkDate(filters.workDate());
		DateRange dateRange = validateDateRange(filters.dateRange());

		if (workDate != null && dateRange != null) {
			throw invalid("workDate and dateRange cannot be used together.");
		}

		return new ValidatedFilters(operatorName, processName, cassetteId, workDate, dateRange);
	}

	private TextFilter validateTextFilter(TextFilterPayload payload, boolean uppercase) {
		if (payload == null || isBlank(payload.value())) {
			return null;
		}

		MatchType match = parseOptionalEnum(payload.match(), MatchType.class, MatchType.EXACT);
		String value = normalizeText(payload.value());
		if (uppercase) {
			value = value.toUpperCase(Locale.ROOT);
		}
		return new TextFilter(value, match);
	}

	private DateFilter validateWorkDate(SingleDateFilterPayload payload) {
		if (payload == null || isBlank(payload.value())) {
			return null;
		}

		DateValueType type = parseEnum(payload.type(), DateValueType.class, "filters.workDate.type");
		if (type == DateValueType.RELATIVE) {
			String relative = normalizeEnumToken(payload.value());
			if (!RELATIVE_DATES.contains(relative)) {
				throw invalid("filters.workDate.value must be TODAY or YESTERDAY for RELATIVE dates.");
			}
			return new DateFilter(type, relative, null);
		}

		return new DateFilter(type, null, parseDate(payload.value(), "filters.workDate.value"));
	}

	private DateRange validateDateRange(DateRangeFilterPayload payload) {
		if (payload == null || (isBlank(payload.start()) && isBlank(payload.end()))) {
			return null;
		}

		LocalDate start = parseDate(payload.start(), "filters.dateRange.start");
		LocalDate end = parseDate(payload.end(), "filters.dateRange.end");
		if (start.isAfter(end)) {
			throw invalid("filters.dateRange.start must be before or equal to end.");
		}

		long days = ChronoUnit.DAYS.between(start, end) + 1;
		if (days > this.properties.maxDateRangeDays()) {
			throw invalid("dateRange exceeds the allowed maximum days.");
		}

		return new DateRange(start, end);
	}

	private List<GroupByField> validateGroupBy(List<String> rawFields) {
		if (rawFields == null || rawFields.isEmpty()) {
			return List.of();
		}

		Set<GroupByField> deduped = new LinkedHashSet<>();
		for (String raw : rawFields) {
			deduped.add(parseEnum(raw, GroupByField.class, "groupBy"));
		}

		if (deduped.size() > 2) {
			throw invalid("groupBy supports up to 2 fields.");
		}

		return List.copyOf(deduped);
	}

	private List<MetricSpec> validateMetrics(List<MetricPayload> rawMetrics) {
		if (rawMetrics == null || rawMetrics.isEmpty()) {
			return List.of();
		}

		List<MetricSpec> metrics = new ArrayList<>();
		for (MetricPayload metric : rawMetrics) {
			MetricType type = parseEnum(metric.type(), MetricType.class, "metrics.type");
			MetricField field = parseMetricField(metric.field());
			metrics.add(new MetricSpec(type, field, defaultIfBlank(metric.alias(), null)));
		}
		return List.copyOf(metrics);
	}

	private List<SortSpec> validateSort(List<SortPayload> rawSort) {
		if (rawSort == null || rawSort.isEmpty()) {
			return List.of();
		}

		List<SortSpec> sortSpecs = new ArrayList<>();
		for (SortPayload sort : rawSort) {
			SortField field = parseEnum(sort.field(), SortField.class, "sort.field");
			SortDirection direction = parseOptionalEnum(sort.direction(), SortDirection.class, SortDirection.DESC);
			sortSpecs.add(new SortSpec(field, direction));
		}
		return List.copyOf(sortSpecs);
	}

	private Integer validateLimit(Integer limit, PlanOperation operation) {
		if (operation == PlanOperation.NONE || operation == PlanOperation.COUNT_ROWS) {
			return null;
		}

		int normalized = limit == null ? this.properties.defaultLimit() : limit;
		if (normalized < 1 || normalized > this.properties.maxLimit()) {
			throw invalid("limit must be between 1 and " + this.properties.maxLimit() + ".");
		}
		return normalized;
	}

	private void validateOperationRules(
		PlanQueryType queryType,
		PlanOperation operation,
		ValidatedFilters filters,
		List<GroupByField> groupBy,
		List<MetricSpec> metrics,
		List<SortSpec> sort,
		Integer limit
	) {
		switch (operation) {
			case NONE -> {
				if (queryType != PlanQueryType.GENERAL) {
					throw invalid("operation NONE can only be used with queryType GENERAL.");
				}
				if (filters.hasAny() || !groupBy.isEmpty() || !metrics.isEmpty() || !sort.isEmpty() || limit != null) {
					throw invalid("GENERAL/NONE plans cannot include filters, groupBy, metrics, sort, or limit.");
				}
			}
			case SELECT_ROWS, SUMMARIZE_ROWS -> {
				if (!groupBy.isEmpty() || !metrics.isEmpty()) {
					throw invalid(operation + " cannot include groupBy or metrics.");
				}
			}
			case COUNT_ROWS -> {
				if (!groupBy.isEmpty()) {
					throw invalid("COUNT_ROWS cannot include groupBy.");
				}
				if (!metrics.isEmpty() && !isCountAllOnly(metrics)) {
					throw invalid("COUNT_ROWS only supports COUNT(ALL).");
				}
			}
			case GROUP_COUNT -> {
				if (groupBy.isEmpty()) {
					throw invalid("GROUP_COUNT requires at least one groupBy field.");
				}
				if (!isCountAllOnly(metrics)) {
					throw invalid("GROUP_COUNT requires COUNT(ALL).");
				}
			}
		}

		if (queryType == PlanQueryType.COUNT
			&& operation != PlanOperation.COUNT_ROWS
			&& operation != PlanOperation.GROUP_COUNT) {
			throw invalid("COUNT queryType must use COUNT_ROWS or GROUP_COUNT.");
		}

		if (queryType == PlanQueryType.AGGREGATE && operation != PlanOperation.GROUP_COUNT) {
			throw invalid("AGGREGATE queryType must use GROUP_COUNT.");
		}

		if (queryType == PlanQueryType.GENERAL && operation != PlanOperation.NONE) {
			throw invalid("GENERAL queryType must use operation NONE.");
		}
	}

	private boolean isCountAllOnly(List<MetricSpec> metrics) {
		return metrics.size() == 1
			&& metrics.get(0).type() == MetricType.COUNT
			&& metrics.get(0).field() == MetricField.ALL;
	}

	private MetricField parseMetricField(String raw) {
		if (isBlank(raw) || "*".equals(raw.trim()) || "ALL".equalsIgnoreCase(raw.trim())) {
			return MetricField.ALL;
		}
		throw invalid("Only '*' or 'ALL' is supported for metrics.field in v1.");
	}

	private <E extends Enum<E>> E parseEnum(String raw, Class<E> enumType, String fieldName) {
		if (isBlank(raw)) {
			throw invalid(fieldName + " is required.");
		}

		try {
			return Enum.valueOf(enumType, normalizeEnumToken(raw));
		}
		catch (IllegalArgumentException ex) {
			throw invalid(fieldName + " has unsupported value: " + raw);
		}
	}

	private <E extends Enum<E>> E parseOptionalEnum(String raw, Class<E> enumType, E fallback) {
		return isBlank(raw) ? fallback : parseEnum(raw, enumType, enumType.getSimpleName());
	}

	private String normalizeEnumToken(String raw) {
		return raw.trim()
			.replaceAll("([a-z])([A-Z])", "$1_$2")
			.replace('-', '_')
			.replace(' ', '_')
			.toUpperCase(Locale.ROOT);
	}

	private LocalDate parseDate(String raw, String fieldName) {
		if (isBlank(raw)) {
			throw invalid(fieldName + " is required.");
		}

		try {
			return LocalDate.parse(raw.trim().replace('.', '-').replace('/', '-'));
		}
		catch (Exception ex) {
			throw invalid(fieldName + " must be in yyyy-MM-dd format.");
		}
	}

	private String normalizeText(String value) {
		return value == null ? null : value.trim().replaceAll("\\s+", " ");
	}

	private String defaultIfBlank(String value, String fallback) {
		return isBlank(value) ? fallback : normalizeText(value);
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private ValidatedFilters emptyFilters() {
		return new ValidatedFilters(null, null, null, null, null);
	}

	private QueryPlanValidationException invalid(String message) {
		return new QueryPlanValidationException(message);
	}
}
