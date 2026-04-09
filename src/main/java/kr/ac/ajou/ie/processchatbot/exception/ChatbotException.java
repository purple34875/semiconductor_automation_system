package kr.ac.ajou.ie.processchatbot.exception;

import org.springframework.http.HttpStatus;

public class ChatbotException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final Object details;

	public ChatbotException(HttpStatus status, String code, String message) {
		this(status, code, message, null);
	}

	public ChatbotException(HttpStatus status, String code, String message, Object details) {
		super(message);
		this.status = status;
		this.code = code;
		this.details = details;
	}

	public HttpStatus getStatus() {
		return this.status;
	}

	public String getCode() {
		return this.code;
	}

	public Object getDetails() {
		return this.details;
	}
}
