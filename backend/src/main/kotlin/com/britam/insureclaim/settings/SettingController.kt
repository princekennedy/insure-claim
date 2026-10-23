package com.britam.insureclaim.settings

import com.britam.insureclaim.chatbot.ChatProvider
import com.britam.insureclaim.common.Auditable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.security.access.prepost.PreAuthorize

@Tag(name = "Settings", description = "Admin-managed runtime configuration for the portal")
@RestController
@RequestMapping("/api/v1/admin/settings")
class SettingController(private val settingService: SettingService) {

	@Operation(summary = "Every settings group with current values")
	@GetMapping
	@PreAuthorize("hasAnyRole('INSURER_ADMIN', 'ADMIN')")
	fun settings(): SettingsResponse = settingService.settings()

	@Operation(summary = "AI providers and the models the assistant can use")
	@GetMapping("/chatbot/providers")
	@PreAuthorize("hasAnyRole('INSURER_ADMIN', 'ADMIN')")
	fun chatbotProviders(): List<ChatProvider> = settingService.chatbotProviders()

	@Operation(summary = "Apply partial settings updates (insurer admins and admins)")
	@PutMapping
	@PreAuthorize("hasAnyRole('INSURER_ADMIN', 'ADMIN')")
	@Auditable(action = "SETTINGS_UPDATE", description = "Updated portal settings", entityType = "SETTING")
	fun update(@Valid @RequestBody request: SettingsUpdateRequest): SettingsResponse =
		settingService.update(request)
}
