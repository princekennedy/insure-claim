package com.britam.insureclaim.common

import org.springframework.http.HttpStatus

/**
 * Base type for every failure the API reports deliberately. Anything else
 * escaping a controller is treated as a bug and masked as a 500.
 */
sealed class ApiException(
	val status: HttpStatus,
	override val message: String,
	val code: String,
) : RuntimeException(message)

class NotFoundException(resource: String, identifier: Any) :
	ApiException(HttpStatus.NOT_FOUND, "$resource not found: $identifier", "RESOURCE_NOT_FOUND")

class ConflictException(message: String, code: String = "CONFLICT") :
	ApiException(HttpStatus.CONFLICT, message, code)

class ValidationException(message: String, code: String = "VALIDATION_FAILED") :
	ApiException(HttpStatus.UNPROCESSABLE_ENTITY, message, code)

class UnauthorizedException(message: String = "Authentication required", code: String = "UNAUTHORIZED") :
	ApiException(HttpStatus.UNAUTHORIZED, message, code)

class ForbiddenException(message: String = "You do not have access to this resource") :
	ApiException(HttpStatus.FORBIDDEN, message, "FORBIDDEN")

class BusinessRuleException(message: String, code: String = "BUSINESS_RULE_VIOLATION") :
	ApiException(HttpStatus.UNPROCESSABLE_ENTITY, message, code)

class BadRequestException(message: String, code: String = "BAD_REQUEST") :
	ApiException(HttpStatus.BAD_REQUEST, message, code)

class RateLimitedException(message: String = "Too many requests, please retry shortly") :
	ApiException(HttpStatus.TOO_MANY_REQUESTS, message, "RATE_LIMITED")
