package kr.ac.ajou.ie.processchatbot.exception;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import kr.ac.ajou.ie.processchatbot.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ChatbotException.class)
	public ResponseEntity<ErrorResponse> handleChatbotException(ChatbotException ex) {
		log.warn("Handled chatbot exception: code={}, message={}", ex.getCode(), ex.getMessage());
		return ResponseEntity.status(ex.getStatus())
			.body(new ErrorResponse(OffsetDateTime.now(), ex.getCode(), ex.getMessage(), ex.getDetails()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
		Map<String, String> details = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.collect(Collectors.toMap(
				FieldError::getField,
				FieldError::getDefaultMessage,
				(first, second) -> second,
				LinkedHashMap::new
			));

		return ResponseEntity.badRequest()
			.body(new ErrorResponse(OffsetDateTime.now(), "INVALID_REQUEST", "잘못된 요청입니다.", details));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
		return ResponseEntity.badRequest()
			.body(new ErrorResponse(OffsetDateTime.now(), "INVALID_REQUEST", "요청 본문을 읽을 수 없습니다.", null));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
		log.error("Unhandled exception", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(new ErrorResponse(OffsetDateTime.now(), "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.", null));
	}
}
