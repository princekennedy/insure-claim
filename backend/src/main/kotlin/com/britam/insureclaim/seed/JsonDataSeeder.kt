package com.britam.insureclaim.seed

import com.britam.insureclaim.garage.Garage
import com.britam.insureclaim.garage.GarageRepository
import com.britam.insureclaim.policy.Policy
import com.britam.insureclaim.policy.PolicyRepository
import com.britam.insureclaim.policy.PolicyStatus
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

    private fun seedVehicles(fileName: String, root: JsonNode) {
        if (!requireAccountsSeedable(fileName)) return
        var inserted = 0
        var present = 0
        var orphaned = 0
        transactionTemplate.executeWithoutResult {
            items(fileName, root, SeedVehicle::class.java).forEach { seed ->
                val registration = requireText(fileName, seed.registrationNumber, "registrationNumber")
                val owner = userRepository.findByEmailIgnoreCase(
                    req
