package com.britam.insureclaim.settings

import com.britam.insureclaim.chatbot.ChatProvider
import com.britam.insureclaim.chatbot.ChatProviders
import com.britam.insureclaim.common.ValidationException
import com.britam.insureclaim.security.ChatbotProperties
import com.britam.insureclaim.security.ClaimProperties
import com.britam.insureclaim.security.FraudProperties
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Schema(name = "SettingValue", description = "One configuration value with its type and help text")
data class SettingValueResponse(
	val key: String,
	@param:Schema(description = "Empty for secret values; use `configured` to tell whether one is stored")
	val value: String,
	@param:Schema(description = "STRING, NUMBER or BOOLEAN")
	val dataType: SettingDataType,
	val description: String,
	@param:Schema(description = "True when the value is write-only and never returned")
	val secret: Boolean = false,
	@param:Schema(description = "For secret values: whether a value is currently stored")
	val configured: Boolean = false,
	@param:Schema(description = "Allowed values for enum-like fields, empty when free-form")
	val options: List<String> = emptyList(),
)

data class SettingGroupResponse(
	val group: String,
	val settings: List<SettingValueResponse>,
)

data class SettingsResponse(val groups: List<SettingGroupResponse>)

@Schema(name = "SettingUpdate", description = "A single key and its new value")
data class SettingUpdateRequest(
	@field:NotBlank @field:Size(max = 64) val key: String,
	@field:Size(max = 2000) val value: String = "",
)

data class SettingGroupUpdateRequest(
	@field:NotBlank @field:Size(max = 32) val group: String,
	@field:NotEmpty val settings: List<SettingUpdateRequest>,
)

@Schema(name = "SettingsUpdateRequest", description = "Partial updates grouped by setting group")
data class SettingsUpdateRequest(
	@field:NotEmpty val groups: List<SettingGroupUpdateRequest>,
)

/**
 * Admin-managed runtime configuration.
 *
 * The registry below is the source of truth: it knows every key the portal
 * accepts, its type, its help text and how to validate it. Row values from
 * `app_settings` (V9) override the environment defaults captured from
 * [ChatbotProperties] / [ClaimProperties] / [FraudProperties], so behaviour
 * without any database rows is exactly what `application.properties` produces.
 *
 * [SettingController] exposes read/write to the Settings screen; services that
 * need the effective value call the typed accessors ([maxOpenClaimsPerCustomer]
 * and friends) instead of reading properties directly.
 */
@Service
class SettingService(
	private val settingRepository: AppSettingRepository,
	private val chatbotProperties: ChatbotProperties,
	private val claimProperties: ClaimProperties,
	private val fraudProperties: FraudProperties,
) {

	data class SettingDefinition(
		val group: String,
		val key: String,
		val dataType: SettingDataType,
		val description: String,
		val defaultValue: String,
		val validate: (String) -> String? = { null },
		/** Write-only values (API keys) are never returned; a blank update keeps the stored value. */
		val secret: Boolean = false,
		/** Allowed values for enum-like fields, surfaced to the UI. */
		val options: List<String> = emptyList(),
	)

	private val definitions: List<SettingDefinition> = listOf(
		SettingDefinition(
			CHATBOT, "enabled", SettingDataType.BOOLEAN,
			"Whether the AI assistant answers customer questions",
			chatbotProperties.enabled.toString(),
			booleanRule,
		),
		SettingDefinition(
			CHATBOT, "provider", SettingDataType.STRING,
			"AI provider the assistant talks to",
			chatbotProperties.provider,
			enumRule(ChatProviders.ids),
			options = ChatProviders.ids,
		),
		SettingDefinition(
			CHATBOT, "model", SettingDataType.STRING,
			"Model used for assistant replies",
			chatbotProperties.model,
			nonBlankRule,
		),
		SettingDefinition(
			CHATBOT, "api_key", SettingDataType.STRING,
			"API key for the selected provider (stored write-only)",
			chatbotProperties.apiKey,
			{ null },
			secret = true,
		),
		SettingDefinition(
			CHATBOT, "max_tokens", SettingDataType.NUMBER,
			"Maximum tokens in a single assistant reply",
			chatbotProperties.maxTokens.toString(),
			intRule(64, 4096),
		),
		SettingDefinition(
			CHATBOT, "escalate_threshold", SettingDataType.NUMBER,
			"Confidence below which a conversation escalates to a human",
			chatbotProperties.escalateScoreThreshold.toString(),
			decimalRule(0.0, 1.0),
		),
		SettingDefinition(
			CLAIMS, "max_open_claims_per_customer", SettingDataType.NUMBER,
			"Open claims a customer may have at once",
			claimProperties.maxOpenClaimsPerCustomer.toString(),
			intRule(1, 100),
		),
		SettingDefinition(
			CLAIMS, "settlement_window_days", SettingDataType.NUMBER,
			"Days a settlement offer stays valid",
			claimProperties.settlementWindowDays.toString(),
			intRule(1, 365),
		),
		SettingDefinition(
			FRAUD, "auto_alert_threshold", SettingDataType.NUMBER,
			"Score at which a claim is automatically flagged for fraud review",
			fraudProperties.autoAlertThreshold.toString(),
			intRule(0, 100),
		),
	)

	private val definitionIndex: Map<String, SettingDefinition> =
		definitions.associateBy { "${it.group}.${it.key}" }

	/** Every known setting, grouped in registry order, with stored overrides applied. */
	@Transactional(readOnly = true)
	fun settings(): SettingsResponse {
		val stored = settingRepository.findAllByOrderBySettingGroupAscSettingKeyAsc()
			.associateBy { "${it.settingGroup}.${it.settingKey}" }
		val groups = definitions.groupBy { it.group }.map { (group, groupDefinitions) ->
			SettingGroupResponse(
				group = group,
				settings = groupDefinitions.map { definition ->
					val row = stored["${definition.group}.${definition.key}"]
					val storedValue = row?.settingValue
					// The model default depends on the selected provider, so show
					// exactly what the assistant will call.
					val effective = if (definition.group == CHATBOT && definition.key == "model") {
						chatbotModel()
					} else {
						storedValue ?: definition.defaultValue
					}
					SettingValueResponse(
						key = definition.key,
						value = if (definition.secret) "" else effective,
						dataType = definition.dataType,
						description = row?.description?.takeIf { it.isNotBlank() } ?: definition.description,
						secret = definition.secret,
						configured = definition.secret &&
							(storedValue?.isNotBlank() == true || definition.defaultValue.isNotBlank()),
						options = definition.options,
					)
				},
			)
		}
		return SettingsResponse(groups)
	}

	/**
	 * Validates the whole payload before touching the table so a rejected key
	 * never leaves a section half-applied, then upserts each row.
	 */
	@Transactional
	fun update(request: SettingsUpdateRequest): SettingsResponse {
		val updates = mutableListOf<Pair<SettingDefinition, String>>()
		for (groupUpdate in request.groups) {
			for (setting in groupUpdate.settings) {
				val definition = definitionIndex["${groupUpdate.group}.${setting.key}"]
					?: throw ValidationException(
						"Unknown setting ${groupUpdate.group}.${setting.key}",
						"UNKNOWN_SETTING",
					)
				val value = setting.value.trim()
				// A blank secret means "leave the stored key untouched".
				if (definition.secret && value.isBlank()) continue
				val error = definition.validate(value)
					?: if (value.length > MAX_VALUE_LENGTH) "must be at most $MAX_VALUE_LENGTH characters" else null
				if (error != null) {
					throw ValidationException(
						"Invalid value for ${definition.group}.${definition.key}: $error",
						"INVALID_SETTING_VALUE",
					)
				}
				updates += definition to value
			}
		}
		updates.forEach { (definition, value) -> upsert(definition, value) }
		return settings()
	}

	// ------------------------------------------------- typed value access ----

	fun chatbotEnabled(): Boolean =
		storedValue(CHATBOT, "enabled")?.toBooleanStrictOrNull() ?: chatbotProperties.enabled

	fun chatbotProvider(): String =
		storedValue(CHATBOT, "provider")
			?.takeIf { it.isNotBlank() && ChatProviders.ids.contains(it) }
			?: chatbotProperties.provider.takeIf { ChatProviders.ids.contains(it) }
			?: ChatProviders.ANTHROPIC

	fun chatbotApiKey(): String =
		storedValue(CHATBOT, "api_key")?.takeIf { it.isNotBlank() } ?: chatbotProperties.apiKey

	/** True when an effective key exists (stored in Settings or supplied by the environment). */
	fun chatbotHasProvider(): Boolean = chatbotApiKey().isNotBlank()

	fun chatbotModel(): String {
		storedValue(CHATBOT, "model")?.takeIf { it.isNotBlank() }?.let { return it }
		val provider = chatbotProvider()
		return if (provider == ChatProviders.ANTHROPIC) {
			chatbotProperties.model
		} else {
			ChatProviders.byId(provider)?.defaultModel ?: chatbotProperties.model
		}
	}

	/** Providers the assistant can be pointed at, with their model catalogs. */
	fun chatbotProviders(): List<ChatProvider> = ChatProviders.all

	fun chatbotMaxTokens(): Int =
		storedValue(CHATBOT, "max_tokens")?.toIntOrNull() ?: chatbotProperties.maxTokens

	fun chatbotEscalateThreshold(): Double =
		storedValue(CHATBOT, "escalate_threshold")?.toDoubleOrNull() ?: chatbotProperties.escalateScoreThreshold

	fun maxOpenClaimsPerCustomer(): Int =
		storedValue(CLAIMS, "max_open_claims_per_customer")?.toIntOrNull()
			?: claimProperties.maxOpenClaimsPerCustomer

	fun settlementWindowDays(): Int =
		storedValue(CLAIMS, "settlement_window_days")?.toIntOrNull() ?: claimProperties.settlementWindowDays

	fun autoAlertThreshold(): Int =
		storedValue(FRAUD, "auto_alert_threshold")?.toIntOrNull() ?: fraudProperties.autoAlertThreshold

	// ------------------------------------------------------- internals -------

	private fun storedValue(group: String, key: String): String? =
		settingRepository.findBySettingGroupAndSettingKey(group, key)
			.map { it.settingValue }
			.orElse(null)

	private fun upsert(definition: SettingDefinition, value: String) {
		val row = settingRepository.findBySettingGroupAndSettingKey(definition.group, definition.key)
			.orElseGet {
				AppSetting(
					settingGroup = definition.group,
					settingKey = definition.key,
					dataType = definition.dataType,
					description = definition.description,
				)
			}
		row.settingValue = value
		row.dataType = definition.dataType
		if (row.description.isBlank()) row.description = definition.description
		settingRepository.save(row)
	}

	companion object {
		private const val CHATBOT = "chatbot"
		private const val CLAIMS = "claims"
		private const val FRAUD = "fraud"
		private const val MAX_VALUE_LENGTH = 2000

		private val booleanRule: (String) -> String? = { value ->
			if (value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true)) {
				null
			} else {
				"must be true or false"
			}
		}

		private val nonBlankRule: (String) -> String? = { value ->
			if (value.isBlank()) "must not be empty" else null
		}

		private fun enumRule(allowed: List<String>): (String) -> String? = { value ->
			if (allowed.contains(value)) null else "must be one of ${allowed.joinToString(", ")}"
		}

		private fun intRule(min: Int, max: Int): (String) -> String? = { value ->
			val number = value.toIntOrNull()
			when {
				number == null -> "must be a whole number"
				number < min || number > max -> "must be between $min and $max"
				else -> null
			}
		}

		private fun decimalRule(min: Double, max: Double): (String) -> String? = { value ->
			val number = value.toDoubleOrNull()
			when {
				number == null -> "must be a number"
				number < min || number > max -> "must be between $min and $max"
				else -> null
			}
		}
	}
}
