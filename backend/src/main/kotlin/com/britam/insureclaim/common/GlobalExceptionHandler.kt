package com.britam.insureclaim.common

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.time.Instant

@Schema(name = "ApiError", description = "Uniform error envelope returned by every endpoint")
data class ApiErrorResponse(
	val timestamp: Instant = Instant.now(),
	val status: Int,
	val error: String,
	val code: String,
	val message: String,
	val path: String,
	val fieldErrors: List<FieldErrorDto>? = null,
)

@Schema(name = "FieldError", description = "Validation failure for a single request field")
data class FieldErrorDto(
	val field: String,
	val rejectedValue: Any? = null,
	val message: String,
)

/**
 * Translates exceptions into the same envelope everywhere so the frontend can
 * rely on a single error shape. Unexpected exception types are logged at ERROR
 * and reported as a generic 500 so internals never leak to clients.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

	companion object {
		private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
	}

	@ExceptionHandler(ApiException::class)
	fun handleApiException(ex: ApiException, request: HttpServletRequest): ResponseEntity<ApiErrorResponse> =
		build(ex.status, ex.code, ex.message, request)

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidation(
		ex: MethodArgumentNotValidException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> {
		val fieldErrors = ex.bindingResult.fieldErrors.map {
			FieldErrorDto(
				field = it.field,
				rejectedValue = it.rejectedValue?.toString()?.take(120),
				message = it.defaultMessage ?: "is invalid",
			)
		}
		return build(
			HttpStatus.BAD_REQUEST,
			"VALIDATION_FAILED",
			"Request validation failed",
			request,
			fieldErrors,
		)
	}

	@ExceptionHandler(AuthenticationException::class)
	fun handleAuthentication(ex: AuthenticationException, request: HttpServletRequest): ResponseEntity<ApiErrorResponse> {
		log.debug("Authentication failure on {}: {}", request.requestURI, ex.message)
		return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required", request)
	}

	@ExceptionHandler(AccessDeniedException::class)
	fun handleAccessDenied(ex: AccessDeniedException, request: HttpServletRequest): ResponseEntity<ApiErrorResponse> =
		build(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this resource", request)

	@ExceptionHandler(MissingServletRequestParameterException::class)
	fun handleMissingParam(
		ex: MissingServletRequestParameterException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> =
		build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", ex.message, request)

	@ExceptionHandler(HttpRequestMethodNotSupportedException::class)
	fun handleMethodNotAllowed(
		ex: HttpRequestMethodNotSupportedException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> =
		build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not supported for this endpoint", request)

	@ExceptionHandler(NoResourceFoundException::class)
	fun handleNoResource(ex: NoResourceFoundException, request: HttpServletRequest): ResponseEntity<ApiErrorResponse> =
		build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Not Found", request)

	@ExceptionHandler(MethodArgumentTypeMismatchException::class)
	fun handleTypeMismatch(
		ex: MethodArgumentTypeMismatchException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> =
		build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "Parameter '${ex.name}' has an invalid value", request)

	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleUnreadableBody(
		ex: HttpMessageNotReadableException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> {
		log.debug("Malformed request body on {}: {}", request.requestURI, ex.mostSpecificCause.message)
		return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body could not be read", request)
	}

	@ExceptionHandler(DataIntegrityViolationException::class)
	fun handleDataIntegrity(
		ex: DataIntegrityViolationException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> {
		// Unique constraints are enforced in the schema; surface them as conflicts
		// rather than leaking driver detail through the generic 500 path.
		log.warn("Constraint violation on {} {}: {}", request.method, request.requestURI, ex.mostSpecificCause.message)
		return build(HttpStatus.CONFLICT, "CONSTRAINT_VIOLATION", "That record already exists", request)
	}

	@ExceptionHandler(Exception::class)
	fun handleUnexpected(ex: Exception, request: HttpServletRequest): ResponseEntity<ApiErrorResponse> {
		log.error("Unhandled exception on {} {}", request.method, request.requestURI, ex)
		return build(
			HttpStatus.INTERNAL_SERVER_ERROR,
			"INTERNAL_ERROR",
			"An unexpected error occurred. Please try again later.",
			request,
		)
	}

	private fun build(
		status: HttpStatus,
		code: String,
		message: String,
		request: HttpServletRequest,
		fieldErrors: List<FieldErrorDto>? = null,
	): ResponseEntity<ApiErrorResponse> {
		val body = ApiErrorResponse(
			status = status.value(),
			error = status.reasonPhrase,
			code = code,
			message = message,
			path = request.requestURI,
			fieldErrors = fieldErrors,
		)
		return ResponseEntity.status(status).body(body)
	}
}
