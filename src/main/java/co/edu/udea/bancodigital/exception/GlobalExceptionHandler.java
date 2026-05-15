package co.edu.udea.bancodigital.exception;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.dao.DataIntegrityViolationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Manejador global de excepciones para la API REST.
 * Intercepta todas las excepciones y proporciona respuestas consistentes.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	/**
	 * Maneja la excepción EntityNotFoundException.
	 * Responde con HTTP 404 Not Found.
	 */
	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ApiError> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
		String traceId = UUID.randomUUID().toString();
		log.warn("EntityNotFoundException [traceId: {}] - {}", traceId, ex.getMessage(), ex);

		ApiError apiError = ApiError.builder()
				.errorCode("NOT_FOUND")
				.message(ex.getMessage())
				.details("La entidad solicitada no existe")
				.traceId(traceId)
				.timestamp(LocalDateTime.now())
				.build();

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
	}

	/**
	 * Maneja la excepción DuplicateResourceException.
	 * Responde con HTTP 409 Conflict.
	 */
	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ApiError> handleDuplicateResourceException(DuplicateResourceException ex, WebRequest request) {
		String traceId = UUID.randomUUID().toString();
		log.warn("DuplicateResourceException [traceId: {}] - {}", traceId, ex.getMessage(), ex);

		ApiError apiError = ApiError.builder()
				.errorCode("DUPLICATE_RESOURCE")
				.message(ex.getMessage())
				.details("El recurso que intenta registrar o actualizar ya existe")
				.traceId(traceId)
				.timestamp(LocalDateTime.now())
				.build();

		return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
	}

	/**
	 * Maneja la excepción IllegalArgumentException.
	 * Responde con HTTP 400 Bad Request.
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
		String traceId = UUID.randomUUID().toString();
		log.warn("IllegalArgumentException [traceId: {}] - {}", traceId, ex.getMessage(), ex);

		ApiError apiError = ApiError.builder()
				.errorCode("INVALID_ARGUMENT")
				.message(ex.getMessage())
				.details("El argumento proporcionado es inválido")
				.traceId(traceId)
				.timestamp(LocalDateTime.now())
				.build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
	}

	/**
	 * Maneja la excepción AccessDeniedException.
	 * Responde con HTTP 403 Forbidden.
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
		String traceId = UUID.randomUUID().toString();
		log.warn("AccessDeniedException [traceId: {}] - {}", traceId, ex.getMessage(), ex);

		ApiError apiError = ApiError.builder()
				.errorCode("ACCESS_DENIED")
				.message("Acceso denegado")
				.details("No tiene permisos para acceder a este recurso")
				.traceId(traceId)
				.timestamp(LocalDateTime.now())
				.build();

		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiError);
	}

	/**
	 * Maneja la excepción AuthenticationException.
	 * Responde con HTTP 401 Unauthorized.
	 */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
		String traceId = UUID.randomUUID().toString();
		log.warn("AuthenticationException [traceId: {}] - {}", traceId, ex.getMessage(), ex);

		ApiError apiError = ApiError.builder()
				.errorCode("UNAUTHORIZED")
				.message("No autenticado")
				.details("Debe iniciar sesion para acceder a este recurso")
				.traceId(traceId)
				.timestamp(LocalDateTime.now())
				.build();

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiError);
	}

	/**
	 * Maneja la excepción MethodArgumentNotValidException.
	 * Responde con HTTP 400 Bad Request y extrae errores de validación.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
			WebRequest request) {
		String traceId = UUID.randomUUID().toString();
		log.warn("MethodArgumentNotValidException [traceId: {}] - Validation failed", traceId, ex);

		String details = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));

		ApiError apiError = ApiError.builder()
				.errorCode("VALIDATION_ERROR")
				.message("Error de validación en la solicitud")
				.details(details)
				.traceId(traceId)
				.timestamp(LocalDateTime.now())
				.build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
	}

	/**
	 * Maneja la excepción DataIntegrityViolationException.
	 * Responde con HTTP 409 Conflict (para registros duplicados).
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> handleDataIntegrityViolationException(DataIntegrityViolationException ex,
			WebRequest request) {
		String traceId = UUID.randomUUID().toString();
		log.warn("DataIntegrityViolationException [traceId: {}] - {}", traceId, ex.getMessage(), ex);

		String details = "Posiblemente existe un registro duplicado o violación de restricción de base de datos";

		ApiError apiError = ApiError.builder()
				.errorCode("DATA_INTEGRITY_VIOLATION")
				.message("Conflicto de integridad de datos")
				.details(details)
				.traceId(traceId)
				.timestamp(LocalDateTime.now())
				.build();

		return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
	}

	/**
	 * Maneja errores de parsing JSON (payload malformado).
	 * Responde con HTTP 400 Bad Request.
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex,
			WebRequest request) {
		String traceId = UUID.randomUUID().toString();
		log.warn("HttpMessageNotReadableException [traceId: {}] - {}", traceId, ex.getMessage(), ex);

		ApiError apiError = ApiError.builder()
				.errorCode("MALFORMED_JSON")
				.message("El cuerpo de la solicitud no es un JSON válido")
				.details("Verifique comas finales, comillas, llaves y tipos de datos en el payload")
				.traceId(traceId)
				.timestamp(LocalDateTime.now())
				.build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
	}

	/**
	 * Maneja cualquier otra excepción no capturada.
	 * Responde con HTTP 500 Internal Server Error.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleGenericException(Exception ex, WebRequest request) {
		String traceId = UUID.randomUUID().toString();
		log.error("Unexpected exception [traceId: {}] - {}", traceId, ex.getMessage(), ex);

		ApiError apiError = ApiError.builder()
				.errorCode("INTERNAL_SERVER_ERROR")
				.message("Ha ocurrido un error interno en el servidor")
				.details("Contacte al administrador con el traceId para más información")
				.traceId(traceId)
				.timestamp(LocalDateTime.now())
				.build();

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
	}
}
