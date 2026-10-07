package com.britam.insureclaim.seed

import com.britam.insureclaim.garage.Garage
import com.britam.insureclaim.garage.GarageRepository
import com.britam.insureclaim.policy.Policy
import com.britam.insureclaim.policy.PolicyRepository
import com.britam.insureclaim.policy.PolicyStatus
import com.britam.insureclaim.role.Role
import com.britam.insureclaim.user.User
import com.britam.insureclaim.user.UserRepository
import com.britam.insureclaim.vehicle.Vehicle
import com.britam.insureclaim.vehicle.VehicleRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDate

class SeedGarage {
	var code: String? = null
	var name: String? = null
	var address: String? = null
	var city: String? = null
	var contactPhone: String? = null
	var contactEmail: String? = null
	var panelRating: BigDecimal? = null
	var active: Boolean? = null
}

class SeedUser {
	var email: String? = null
	var fullName: String? = null
	var phone: String? = null
	var nic: String? = null
	var role: String? = null
	var enabled: Boolean? = null
}

class SeedVehicle {
	var ownerEmail: String? = null
	var registrationNumber: String? = null
	var make: String? = null
	var model: String? = null
	var year: Int? = null
	var color: String? = null
	var chassisNumber: String? = null
	var engineNumber: String? = null
}

class SeedPolicy {
	var customerEmail: String? = null
	var vehicleRegistration: String? = null
	var policyNumber: String? = null
	var insurerName: String? = null
	var productCode: String? = null
	var startDate: String? = null
	var endDate: String? = null
	var premiumAmount: BigDecimal? = null
	var sumInsured: BigDecimal? = null
	var excessAmount: BigDecimal? = null
	var status: String? = null
}

/**
 * Loads reference and onboarding data from the JSON documents in
 * `classpath:seed` once Flyway has migrated the schema.
 *
 * Seed data lives as JSON rather than inside a migration because it is data,
 * not schema: it can be read, reviewed and diffed like any other asset, and it
 * no longer pins a migration version. Every row is applied insert-if-absent, so
 * the seeder is safe to run against an already-populated database and never
 * overwrites something an operator changed.
 *
 * Files are applied in dependency order - accounts before the vehicles and
 * policies that reference them. Adding a file means dropping it in
 * `resources/seed` and adding one branch to [dispatch]; an unregistered file is
 * reported, never silently ignored.
 */
@Component
class JsonDataSeeder(
	private val objectMapper: ObjectMapper,
	private val properties: SeedProperties,
	private val userRepository: UserRepository,
	private val garageRepository: GarageRepository,
	private val vehicleRepository: VehicleRepository,
	private val policyRepository: PolicyRepository,
	private val passwordEncoder: PasswordEncoder,
	private val transactionTemplate: TransactionTemplate,
) : ApplicationRunner {

	private val logger = LoggerFactory.getLogger(javaClass)
	private val resolver = PathMatchingResourcePatternResolver()

	override fun run(args: ApplicationArguments) {
		if (!properties.enabled) {
			logger.info("JSON seeding is disabled (insureclaim.seed.enabled=false)")
			return
		}
		val resources = resolver.getResources(SEED_PATTERN)
		if (resources.isEmpty()) {
			logger.info("No JSON seed files found under classpath:seed")
			return
		}
		val byName = resources.mapNotNull { resource -> resource.filename?.let { it to resource } }.toMap()
		ORDER.filter { byName.containsKey(it) }.forEach { name ->
			val root = byName.getValue(name).inputStream.use { objectMapper.readTree(it) }
			dispatch(name, root)
		}
		val unregistered = byName.keys - ORDER.toSet()
		unregistered.sorted().forEach {
			logger.warn("Seed file {} has no registered handler - it was not applied", it)
		}
	}

	private fun dispatch(fileName: String, root: JsonNode) {
		when (fileName) {
			"users.json" -> seedUsers(fileName, root)
			"garages.json" -> seedGarages(fileName, root)
			"vehicles.json" -> seedVehicles(fileName, root)
			"policies.json" -> seedPolicies(fileName, root)
		}
	}

	// ------------------------------------------------------------- accounts --

	private fun seedUsers(fileName: String, root: JsonNode) {
		if (!properties.canSeedAccounts) {
			logger.warn(
				"Skipping {} - no seed password configured. Set INSURECLAIM_SEED_PASSWORD to seed accounts.",
				fileName,
			)
			return
		}
		val encodedPassword = passwordEncoder.encode(properties.password)
			?: throw IllegalStateException("Password encoder could not hash the seed password")
		var inserted = 0
		var present = 0
		transactionTemplate.executeWithoutResult {
			items(fileName, root, SeedUser::class.java).forEach { seed ->
				val email = requireText(fileName, seed.email, "email").lowercase()
				val fullName = requireText(fileName, seed.fullName, "fullName")
				val role = parseRole(fileName, seed.role)
				val existing = userRepository.findByEmailIgnoreCase(email).orElse(null)
				if (existing != null) {
				if (existing.role != role.code) {
					logger.warn(
						"Seed {}: {} already exists with role {}; seed says {} - left untouched",
						fileName,
						email,
						existing.role,
						role.code,
					)
				}
				present++
				return@forEach
			}
			userRepository.save(
				User().apply {
					this.email = email
					passwordHash = encodedPassword
password = properties.password
					this.fullName = fullName
					phone = seed.phone
					nic = seed.nic
					this.role = role.code
						enabled = seed.enabled ?: true
						emailVerified = true
					},
				)
				inserted++
			}
		}
		logger.info("Seed {}: {} inserted, {} already present", fileName, inserted, present)
	}

	// ------------------------------------------------------- garage panel ----

	private fun seedGarages(fileName: String, root: JsonNode) {
		var inserted = 0
		var present = 0
		transactionTemplate.executeWithoutResult {
			items(fileName, root, SeedGarage::class.java).forEach { seed ->
				val code = requireText(fileName, seed.code, "code")
				val fullName = requireText(fileName, seed.name, "name")
				val street = requireText(fileName, seed.address, "address")
				val cityName = requireText(fileName, seed.city, "city")
				val phone = requireText(fileName, seed.contactPhone, "contactPhone")

				if (garageRepository.findByCode(code).isPresent) {
					present++
					return@forEach
				}
				garageRepository.save(
					Garage().apply {
						this.code = code
						name = fullName
						address = street
						city = cityName
						contactPhone = phone
						contactEmail = seed.contactEmail
						panelRating = seed.panelRating
						active = seed.active ?: true
					},
				)
				inserted++
			}
		}
		logger.info("Seed {}: {} inserted, {} already present", fileName, inserted, present)
	}

	// ------------------------------------------------------------- vehicles --

	private fun seedVehicles(fileName: String, root: JsonNode) {
		if (!requireAccountsSeedable(fileName)) return
		var inserted = 0
		var present = 0
		var orphaned = 0
		transactionTemplate.executeWithoutResult {
			items(fileName, root, SeedVehicle::class.java).forEach { seed ->
				val registration = requireText(fileName, seed.registrationNumber, "registrationNumber")
				val owner = userRepository.findByEmailIgnoreCase(
					requireText(fileName, seed.ownerEmail, "ownerEmail").lowercase(),
				).orElse(null)
				if (owner == null) {
					orphaned++
					logger.warn("Seed {}: {} has no owner account - skipped", fileName, registration)
					return@forEach
				}
				if (vehicleRepository.existsByRegistrationNumberIgnoreCase(registration)) {
					present++
					return@forEach
				}
				vehicleRepository.save(
					Vehicle().apply {
						this.owner = owner
						registrationNumber = registration
						make = requireText(fileName, seed.make, "make")
						model = requireText(fileName, seed.model, "model")
						year = requireInt(fileName, seed.year, "year")
						color = seed.color
						chassisNumber = seed.chassisNumber
						engineNumber = seed.engineNumber
					},
				)
				inserted++
			}
		}
		logger.info("Seed {}: {} inserted, {} already present, {} skipped", fileName, inserted, present, orphaned)
	}

	// ------------------------------------------------------------- policies --

	private fun seedPolicies(fileName: String, root: JsonNode) {
		if (!requireAccountsSeedable(fileName)) return
		var inserted = 0
		var present = 0
		var orphaned = 0
		transactionTemplate.executeWithoutResult {
			items(fileName, root, SeedPolicy::class.java).forEach { seed ->
				val number = requireText(fileName, seed.policyNumber, "policyNumber")
				if (policyRepository.findByPolicyNumber(number).isPresent) {
					present++
					return@forEach
				}
				val customer = userRepository.findByEmailIgnoreCase(
					requireText(fileName, seed.customerEmail, "customerEmail").lowercase(),
				).orElse(null)
				val registration = requireText(fileName, seed.vehicleRegistration, "vehicleRegistration")
				val vehicle = vehicleRepository.findByRegistrationNumberIgnoreCase(registration).orElse(null)
				if (customer == null || vehicle == null) {
					orphaned++
					logger.warn("Seed {}: {} has no customer or vehicle - skipped", fileName, number)
					return@forEach
				}
				val startDate = parseDate(fileName, seed.startDate, "startDate")
				val endDate = parseDate(fileName, seed.endDate, "endDate")
				if (endDate.isBefore(startDate)) {
					throw IllegalStateException("Seed file $fileName: policy $number ends before it starts")
				}
				policyRepository.save(
					Policy().apply {
						policyNumber = number
						this.customer = customer
						this.vehicle = vehicle
						insurerName = seed.insurerName?.takeIf { it.isNotBlank() } ?: Policy.DEFAULT_INSURER
						productCode = seed.productCode?.takeIf { it.isNotBlank() } ?: Policy.DEFAULT_PRODUCT
						this.startDate = startDate
						this.endDate = endDate
						premiumAmount = requireAmount(fileName, seed.premiumAmount, "premiumAmount")
						sumInsured = requireAmount(fileName, seed.sumInsured, "sumInsured")
						excessAmount = seed.excessAmount ?: BigDecimal.ZERO
						status = seed.status?.let { parseStatus(fileName, it) } ?: PolicyStatus.ACTIVE
					},
				)
				inserted++
			}
		}
		logger.info("Seed {}: {} inserted, {} already present, {} skipped", fileName, inserted, present, orphaned)
	}

	// ------------------------------------------------------------- plumbing --

	private fun <T> items(fileName: String, root: JsonNode, type: Class<T>): List<T> {
		val array = when {
			root.isArray -> root
			root.path("items").isArray -> root.path("items")
			else -> throw IllegalStateException("$fileName must be a JSON array or an object with an 'items' array")
		}
		val seeds = ArrayList<T>(array.size())
		for (node in array) {
			seeds.add(objectMapper.convertValue(node, type))
		}
		return seeds
	}

	private fun requireAccountsSeedable(fileName: String): Boolean {
		if (properties.canSeedAccounts) return true
		logger.warn("Skipping {} - no seed password configured (INSURECLAIM_SEED_PASSWORD)", fileName)
		return false
	}

	private fun requireText(fileName: String, value: String?, field: String): String {
		if (value.isNullOrBlank()) {
			throw IllegalStateException("Seed file $fileName has a row without '$field'")
		}
		return value
	}

	private fun requireInt(fileName: String, value: Int?, field: String): Int {
		if (value == null) throw IllegalStateException("Seed file $fileName has a row without '$field'")
		return value
	}

	private fun requireAmount(fileName: String, value: BigDecimal?, field: String): BigDecimal {
		if (value == null) throw IllegalStateException("Seed file $fileName has a row without '$field'")
		return value
	}

	private fun parseDate(fileName: String, value: String?, field: String): LocalDate {
		val raw = requireText(fileName, value, field)
		return try {
			LocalDate.parse(raw)
		} catch (ex: Exception) {
			throw IllegalStateException("Seed file $fileName has an unparseable '$field': $raw")
		}
	}

	private fun parseRole(fileName: String, value: String?): com.britam.insureclaim.role.Role {
		val raw = requireText(fileName, value, "role").uppercase()
		return com.britam.insureclaim.role.Role.entries.firstOrNull { it.code == raw }
			?: throw IllegalStateException("Seed file $fileName has an unknown role: $value")
}

	private fun parseStatus(fileName: String, value: String): PolicyStatus {
		val raw = value.uppercase()
		return PolicyStatus.entries.firstOrNull { it.name == raw }
			?: throw IllegalStateException("Seed file $fileName has an unknown policy status: $value")
	}

	companion object {
		const val SEED_PATTERN = "classpath:seed/*.json"

		/** Dependency order: accounts first, then the records that reference them. */
		val ORDER = listOf("users.json", "garages.json", "vehicles.json", "policies.json")
	}
}

class SeedRole {
	var code: String? = null
	var name: String? = null
	var description: String? = null
}

class SeedPermission {
	var code: String? = null
	var name: String? = null
	var description: String? = null
}

class SeedRolePermission {
	var role: String? = null
	var permissions: List<String>? = null
}
