package kr.ac.ajou.ie.processchatbot.exception;

import org.springframework.http.HttpStatus;

public class LlmServiceException extends ChatbotException {

	public LlmServiceException(String message, Throwable cause) {
		super(HttpStatus.SERVICE_UNAVAILABLE, "LLM_UNAVAILABLE", message);
		initCause(cause);
	}
}
