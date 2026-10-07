package com.britam.insureclaim.user

import com.britam.insureclaim.security.CurrentUserResolver
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Authentication", description = "Registration, sign-in, session refresh and password management")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
	private val authService: AuthService,
	private val currentUser: CurrentUserResolver,
) {

	@Operation(summary = "Register a new policyholder account")
	@ApiResponses(
		SwaggerResponse(responseCode = "201", description = "Account created and signed in"),
		SwaggerResponse(responseCode = "400", description = "Validation failed"),
		SwaggerResponse(responseCode = "409", description = "Email already registered"),
	)
	@PostMapping("/register")
	@SecurityRequirements
	fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<TokenResponse> =
		ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request))

	@Operation(summary = "Sign in and receive an access + refresh token pair")
	@SecurityRequirements
	@PostMapping("/login")
	fun login(
		@Valid @RequestBody request: LoginRequest,
		httpRequest: HttpServletRequest,
	): TokenResponse = authService.login(request, httpRequest.getHeader(USER_AGENT), clientIp(httpRequest))

	@Operation(
		summary = "Exchange a refresh token for a new pair",
		description = "The presented refresh token is revoked and replaced. Presenting an already-revoked token revokes every session for that user.",
	)
	@SecurityRequirements
	@PostMapping("/refresh")
	fun refresh(
		@Valid @RequestBody request: RefreshRequest,
		httpRequest: HttpServletRequest,
	): TokenResponse = authService.refresh(request.refreshToken, httpRequest.getHeader(USER_AGENT), clientIp(httpRequest))

	@Operation(summary = "Sign out of the current device")
	@PostMapping("/logout")
	fun logout(@RequestBody(required = false) request: RefreshRequest?): ResponseEntity<Void> {
		authService.logout(currentUser.requireId(), request?.refreshToken)
		return ResponseEntity.noContent().build()
	}

	@Operation(summary = "Sign out of every device")
	@PostMapping("/logout-all")
	fun logoutAll(): ResponseEntity<Void> {
		authService.logoutAll(currentUser.requireId())
		return ResponseEntity.noContent().build()
	}

	@Operation(summary = "Return the currently authenticated account")
	@GetMapping("/me")
	fun me(): UserResponse = UserResponse.from(authService.findById(currentUser.requireId()))

	@Operation(summary = "Change the signed-in user's password")
	@PostMapping("/change-password")
	fun changePassword(@Valid @RequestBody request: ChangePasswordRequest): ResponseEntity<Void> {
		authService.changePassword(currentUser.requireId(), request)
		return ResponseEntity.noContent().build()
	}

	@Operation(summary = "Request a password reset link")
	@PostMapping("/forgot-password")
	fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<Void> {
		authService.forgotPassword(request)
		return ResponseEntity.noContent().build()
	}

	@Operation(summary = "Reset password using a token")
	@PostMapping("/reset-password")
	fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<Void> {
		authService.resetPassword(request)
		return ResponseEntity.noContent().build()
	}

	private fun clientIp(request: HttpServletRequest): String =
		request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
			?: request.remoteAddr

	private companion object {
		const val USER_AGENT = "User-Agent"
	}
}
