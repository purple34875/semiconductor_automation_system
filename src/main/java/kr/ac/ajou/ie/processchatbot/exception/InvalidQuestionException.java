package kr.ac.ajou.ie.processchatbot.exception;

import org.springframework.http.HttpStatus;

public class InvalidQuestionException extends ChatbotException {

	public InvalidQuestionException(String message) {
		super(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_QUESTION", message);
	}
}
