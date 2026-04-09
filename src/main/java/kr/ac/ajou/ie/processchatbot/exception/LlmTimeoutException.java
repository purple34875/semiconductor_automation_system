package kr.ac.ajou.ie.processchatbot.exception;

import org.springframework.http.HttpStatus;

public class LlmTimeoutException extends ChatbotException {

	public LlmTimeoutException(String message, Throwable cause) {
		super(HttpStatus.GATEWAY_TIMEOUT, "LLM_TIMEOUT", message);
		initCause(cause);
	}
}
