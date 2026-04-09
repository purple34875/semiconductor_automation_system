package kr.ac.ajou.ie.processchatbot.exception;

import org.springframework.http.HttpStatus;

public class DatabaseConnectionException extends ChatbotException {

	public DatabaseConnectionException(String message, Throwable cause) {
		super(HttpStatus.SERVICE_UNAVAILABLE, "DB_UNAVAILABLE", message);
		initCause(cause);
	}
}
