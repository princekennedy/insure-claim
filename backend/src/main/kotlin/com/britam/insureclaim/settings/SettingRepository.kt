package com.britam.insureclaim.settings

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface AppSettingRepository : JpaRepository<AppSetting, Long> {
	fun findBySettingGroupAndSettingKey(settingGroup: String, settingKey: String): Optional<AppSetting>

	fun findBySettingGroupOrderBySettingKey(settingGroup: String): List<AppSetting>

	fun findAllByOrderBySettingGroupAscSettingKeyAsc(): List<AppSetting>
}
