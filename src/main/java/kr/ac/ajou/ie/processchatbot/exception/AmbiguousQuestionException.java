package kr.ac.ajou.ie.processchatbot.exception;

import org.springframework.http.HttpStatus;

public class AmbiguousQuestionException extends ChatbotException {

	public AmbiguousQuestionException(String message) {
		super(HttpStatus.UNPROCESSABLE_ENTITY, "QUESTION_AMBIGUOUS", message);
	}
}
