package com.britam.insureclaim.config

import com.britam.insureclaim.security.CorsProperties
import com.britam.insureclaim.security.JwtAuthenticationFilter
import com.britam.insureclaim.role.Role
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableMethodSecurity
class SecurityConfig(
	private val jwtAuthenticationFilter: JwtAuthenticationFilter,
	private val corsProperties: CorsProperties,
) {

	@Bean
	fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)

	@Bean
	fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
		http
			.csrf { it.disable() }
			.cors { it.configurationSource(corsConfigurationSource()) }
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
			.authorizeHttpRequests { registry ->
				registry
					.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
					.requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
					.requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password").permitAll()
					.requestMatchers("/api/v1/claims/tracking/**").permitAll()
				.requestMatchers("/api/v1/claims/{claimId}/tracking-link").permitAll()

					// Insurer-only surfaces.
					.requestMatchers("/api/v1/analytics/**").hasAnyRole(Role.AGENT.code, Role.INSURER_ADMIN.code, Role.ADMIN.code)
					.requestMatchers("/api/v1/fraud-alerts/**").hasAnyRole(Role.AGENT.code, Role.INSURER_ADMIN.code, Role.ADMIN.code)
					// Staff consoles sit under /admin but are not admin-only; each one
					// states its own roles via @PreAuthorize. Only user administration
					// is reserved for full administrators here.
					.requestMatchers("/api/v1/admin/users/**").hasRole(Role.ADMIN.code)
					.requestMatchers("/api/v1/users/*/role").hasAnyRole(Role.INSURER_ADMIN.code, Role.ADMIN.code)
					.requestMatchers(HttpMethod.GET, "/api/v1/users").hasAnyRole(Role.AGENT.code, Role.INSURER_ADMIN.code, Role.ADMIN.code)

					// Everything else needs a valid token; ownership is enforced in services.
					.anyRequest().authenticated()
			}
			.exceptionHandling { exceptions ->
				exceptions.authenticationEntryPoint { _, response, _ ->
					writeError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required")
				}
				exceptions.accessDeniedHandler { _, response, _ ->
					writeError(
						response,
						HttpStatus.FORBIDDEN,
						"FORBIDDEN",
						"You do not have access to this resource",
					)
				}
			}
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

		return http.build()
	}

	@Bean
	fun corsConfigurationSource(): CorsConfigurationSource {
		val configuration = CorsConfiguration().apply {
			allowedOrigins = corsProperties.allowedOrigins.toList()
			allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
			allowedHeaders = listOf("*")
			exposedHeaders = listOf("Location", "Content-Disposition")
			allowCredentials = true
			maxAge = 3600
		}
		return UrlBasedCorsConfigurationSource().apply {
			registerCorsConfiguration("/**", configuration)
		}
	}

	private fun writeError(
		response: HttpServletResponse,
		status: HttpStatus,
		code: String,
		message: String,
	) {
		response.status = status.value()
		response.contentType = MediaType.APPLICATION_JSON_VALUE
		response.characterEncoding = "UTF-8"
		response.writer.write(
			"""{"status":${status.value()},"error":"${status.reasonPhrase}","code":"$code","message":"$message"}""",
		)
	}
}
