package kr.ac.ajou.ie.processchatbot.queryplan.validation;

import java.util.Map;
import kr.ac.ajou.ie.processchatbot.exception.ChatbotException;
import org.springframework.http.HttpStatus;

public class QueryPlanValidationException extends ChatbotException {

	public QueryPlanValidationException(String message) {
		super(HttpStatus.BAD_REQUEST, "INVALID_QUERY_PLAN", message, Map.of());
	}
}
