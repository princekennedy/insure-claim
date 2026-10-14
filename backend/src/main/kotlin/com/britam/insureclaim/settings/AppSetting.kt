package com.britam.insureclaim.settings

import com.britam.insureclaim.common.AuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

enum class SettingDataType {
	STRING,
	NUMBER,
	BOOLEAN,
}

/**
 * One admin-managed configuration value. Rows override the matching key from
 * `application.properties`; a missing row falls back to the environment
 * default declared in [com.britam.insureclaim.security.Properties].
 */
@Entity
@Table(
	name = "app_settings",
	uniqueConstraints = [UniqueConstraint(name = "uq_app_settings_group_key", columnNames = ["setting_group", "setting_key"])],
)
class AppSetting(
	@Column(name = "setting_group", nullable = false, length = 32)
	var settingGroup: String = "",

	@Column(name = "setting_key", nullable = false, length = 64)
	var settingKey: String = "",

	@Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
	var settingValue: String = "",

	@Enumerated(EnumType.STRING)
	@Column(name = "data_type", nullable = false, length = 16)
	var dataType: SettingDataType = SettingDataType.STRING,

	@Column(name = "description", nullable = false, length = 255)
	var description: String = "",
) : AuditableEntity()
