package com.britam.insureclaim

import kotlin.test.Test
import kotlin.test.assertEquals
import org.flywaydb.core.Flyway
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ScriptUtils
import java.sql.DriverManager

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

	@Test
	fun `requeue only identifiable unreviewed automatic KYC rejections`() {
		DriverManager.getConnection("jdbc:h2:mem:kyc-migration;DB_CLOSE_DELAY=-1", "sa", "").use { connection ->
			connection.createStatement().use { statement ->
				statement.execute("""
					CREATE TABLE kyc_verifications (
						id INT PRIMARY KEY, status VARCHAR(24), verified_by BIGINT, verified_at TIMESTAMP,
						resubmission_allowed BOOLEAN, confidence_score DECIMAL(5, 4),
						failure_reason VARCHAR(255), updated_at TIMESTAMP
					)
				""".trimIndent())
				statement.execute("""
					INSERT INTO kyc_verifications VALUES
					(1, 'REJECTED', NULL, NULL, TRUE, 0, 'The passport number is not a valid format', CURRENT_TIMESTAMP),
					(2, 'REJECTED', 7, CURRENT_TIMESTAMP, TRUE, 0, 'The passport number is not a valid format', CURRENT_TIMESTAMP),
					(3, 'REJECTED', NULL, CURRENT_TIMESTAMP, TRUE, 0, 'The passport number is not a valid format', CURRENT_TIMESTAMP),
					(4, 'REJECTED', NULL, NULL, TRUE, 0, 'Other rejection', CURRENT_TIMESTAMP),
					(5, 'PENDING', NULL, NULL, FALSE, 0, 'The passport number is not a valid format', CURRENT_TIMESTAMP)
				""".trimIndent())
			}
			ScriptUtils.executeSqlScript(
				connection,
				ClassPathResource("db/migration/V15__requeue_automatically_rejected_kyc.sql"),
			)
			connection.createStatement().use { statement ->
				statement.executeQuery("SELECT status, resubmission_allowed FROM kyc_verifications ORDER BY id").use { rows ->
					val results = mutableListOf<Pair<String, Boolean>>()
					while (rows.next()) results.add(rows.getString(1) to rows.getBoolean(2))
					assertEquals(
						listOf("PENDING" to false, "REJECTED" to true, "REJECTED" to true, "REJECTED" to true, "PENDING" to false),
						results,
					)
				}
			}
		}
	}
}
