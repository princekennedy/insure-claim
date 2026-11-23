package com.britam.insureclaim

import kotlin.test.Test
import kotlin.test.assertEquals
import org.flywaydb.core.Flyway

class MigrationDiscoveryTest {

	@Test
	fun `migration versions resolve without conflicts`() {
		val flyway = Flyway.configure()
			.dataSource("jdbc:h2:mem:migration-discovery", "sa", "")
			.locations("classpath:db/migration")
			.load()

		val migrations = flyway.info().all()
		assertEquals("feedback dimensions", migrations.single { it.version?.version == "10" }.description)
	}
}
