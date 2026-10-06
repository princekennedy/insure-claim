package com.britam.insureclaim.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

	@Bean
	fun inssureClaimOpenApi(): OpenAPI = OpenAPI()
		.info(
			Info()
				.title("InsureClaim Portal API")
				.description(
					"""
					Motor insurance claims platform.

					**Customers** file claims, complete digital KYC, track progress and rate garage work.
					**Insurer staff** work the claim queue, review fraud alerts and monitor garage quality.
					**Assistants** answer 24/7 through the chat endpoint and escalate concerns to operations.

					Obtain a token from `/api/v1/auth/login` and send it as `Authorization: Bearer <token>`.
					""".trimIndent(),
				)
				.version("v1")
				.contact(Contact().name("InsureClaim Platform Team"))
				.license(License().name("Proprietary")),
		)
		.components(
			Components()
				.addSecuritySchemes(
					BEARER_SCHEME,
					SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")
						.description("JWT access token issued by /api/v1/auth/login"),
				),
		)
		.addSecurityItem(SecurityRequirement().addList(BEARER_SCHEME))

	companion object {
		const val BEARER_SCHEME = "bearerAuth"
	}
}
