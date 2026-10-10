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
    var password: String? = null
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
    var permissions: List<String?>? = null
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

    companion object {
        private const val SEED_PATTERN = "classpath:seed/*.json"
        private val ORDER = listOf(
            "roles.json",
            "permissions.json",
            "role_permissions.json",
            "users.json",
            "garages.json",
            "vehicles.json",
            "policies.json",
        )
    }

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
            "roles.json" -> seedRoles(fileName, root)
            "permissions.json" -> seedPermissions(fileName, root)
            "role_permissions.json" -> seedRolePermissions(fileName, root)
            "users.json" -> seedUsers(fileName, root)
            "garages.json" -> seedGarages(fileName, root)
            "vehicles.json" -> seedVehicles(fileName, root)
            "policies.json" -> seedPolicies(fileName, root)
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private fun <T> items(fileName: String, root: JsonNode, clazz: Class<T>): List<T> {
        val arrayNode = root.path("items").takeIf { it.isArray }
            ?: root.takeIf { it.isArray }
            ?: throw IllegalStateException("Seed $fileName: expected a top-level array or an 'items' array")
        val list = mutableListOf<T>()
        for (element in arrayNode) {
            list.add(objectMapper.treeToValue(element, clazz))
        }
        return list
    }

    private fun requireText(fileName: String, value: String?, fieldName: String): String =
        value?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Seed $fileName: '$fieldName' is required and must not be blank")

    private fun parseRole(fileName: String, code: String?): Role {
        val raw = code?.uppercase()?.trim()
            ?: throw IllegalStateException("Seed $fileName: 'role' is required")
        return try {
            Role.fromCode(raw)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("Seed $fileName: unknown role '$raw'", e)
        }
    }

    // ── role / permission seeders (code-only, validated against Role) ────

    private fun seedRoles(fileName: String, root: JsonNode) {
        var valid = 0
        items(fileName, root, SeedRole::class.java).forEach { seed ->
            val code = requireText(fileName, seed.code, "code").uppercase()
            if (code in Role.VALID_CODES) {
                valid++
            } else {
                logger.warn("Seed {}: role code '{}' is not a canonical role code - ignored", fileName, code)
            }
        }
        logger.info("Seed {}: {} role definitions validated (roles are code-defined, no persistence)", fileName, valid)
    }

    private fun seedPermissions(fileName: String, root: JsonNode) {
        var valid = 0
        items(fileName, root, SeedPermission::class.java).forEach { seed ->
            requireText(fileName, seed.code, "code")
            requireText(fileName, seed.name, "name")
            valid++
        }
        logger.info(
            "Seed {}: {} permission definitions validated (permissions are reserved for future use, no persistence)",
            fileName,
            valid,
        )
    }

    private fun seedRolePermissions(fileName: String, root: JsonNode) {
        var grants = 0
        var unknown = 0
        items(fileName, root, SeedRolePermission::class.java).forEach { seed ->
            val roleCode = requireText(fileName, seed.role, "role").uppercase()
            if (roleCode !in Role.VALID_CODES) {
                logger.warn("Seed {}: unknown role {} - skipping grant", fileName, roleCode)
                return@forEach
            }
            seed.permissions.orEmpty().forEach { pcode ->
                val code = pcode?.trim().orEmpty()
                if (code.isEmpty()) return@forEach
                grants++
            }
        }
        logger.info(
            "Seed {}: {} role-permission grants validated ({} unknown roles skipped); permissions are not persisted",
            fileName,
            grants,
            unknown,
        )
    }

    // ── seeders ─────────────────────────────────────────────────────────

    private fun seedUsers(fileName: String, root: JsonNode) {
        var inserted = 0
        var present = 0
        transactionTemplate.executeWithoutResult {
            items(fileName, root, SeedUser::class.java).forEach { seed ->
                val rawPassword = requireText(fileName, seed.password, "password")
                val encodedPassword = passwordEncoder.encode(rawPassword)!!

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
                        password = encodedPassword
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
        var inserted = 0
        var present = 0
        var orphaned = 0
        transactionTemplate.executeWithoutResult {
            items(fileName, root, SeedVehicle::class.java).forEach { seed ->
                val registration = requireText(fileName, seed.registrationNumber, "registrationNumber")
                val ownerEmail = requireText(fileName, seed.ownerEmail, "ownerEmail").lowercase()
                val owner = userRepository.findByEmailIgnoreCase(ownerEmail).orElse(null)
                if (owner == null) {
                    logger.warn("Seed {}: owner {} not found for vehicle {} - skipped", fileName, ownerEmail, registration)
                    orphaned++
                    return@forEach
                }
                if (vehicleRepository.findByRegistrationNumberIgnoreCase(registration).isPresent) {
                    present++
                    return@forEach
                }
                vehicleRepository.save(
                    Vehicle().apply {
                        this.owner = owner
                        registrationNumber = registration
                        make = seed.make ?: ""
                        model = seed.model ?: ""
                        year = seed.year ?: 0
                        color = seed.color
                        chassisNumber = seed.chassisNumber
                        engineNumber = seed.engineNumber
                    },
                )
                inserted++
            }
        }
        if (orphaned > 0) {
            logger.warn("Seed {}: {} vehicles skipped (owner not found)", fileName, orphaned)
        }
        logger.info("Seed {}: {} inserted, {} already present", fileName, inserted, present)
    }

    private fun seedPolicies(fileName: String, root: JsonNode) {
        var inserted = 0
        var present = 0
        var skipped = 0
        transactionTemplate.executeWithoutResult {
            items(fileName, root, SeedPolicy::class.java).forEach { seed ->
                val policyNumber = requireText(fileName, seed.policyNumber, "policyNumber")
                val customerEmail = requireText(fileName, seed.customerEmail, "customerEmail").lowercase()
                val vehicleReg = requireText(fileName, seed.vehicleRegistration, "vehicleRegistration")

                if (policyRepository.findByPolicyNumber(policyNumber).isPresent) {
                    present++
                    return@forEach
                }
                val customer = userRepository.findByEmailIgnoreCase(customerEmail).orElse(null)
                if (customer == null) {
                    logger.warn("Seed {}: customer {} not found for policy {} - skipped", fileName, customerEmail, policyNumber)
                    skipped++
                    return@forEach
                }
                val vehicle = vehicleRepository.findByRegistrationNumberIgnoreCase(vehicleReg).orElse(null)
                if (vehicle == null) {
                    logger.warn("Seed {}: vehicle {} not found for policy {} - skipped", fileName, vehicleReg, policyNumber)
                    skipped++
                    return@forEach
                }
                val status = seed.status?.let {
                    try {
                        PolicyStatus.valueOf(it.uppercase())
                    } catch (_: IllegalArgumentException) {
                        logger.warn("Seed {}: unknown status '{}' for policy {} - defaulting to ACTIVE", fileName, it, policyNumber)
                        PolicyStatus.ACTIVE
                    }
                } ?: PolicyStatus.ACTIVE

                policyRepository.save(
                    Policy().apply {
                        this.policyNumber = policyNumber
                        this.customer = customer
                        this.vehicle = vehicle
                        insurerName = seed.insurerName ?: Policy.DEFAULT_INSURER
                        productCode = seed.productCode ?: Policy.DEFAULT_PRODUCT
                        startDate = seed.startDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
                        endDate = seed.endDate?.let { LocalDate.parse(it) } ?: LocalDate.now().plusYears(1)
                        premiumAmount = seed.premiumAmount ?: BigDecimal.ZERO
                        sumInsured = seed.sumInsured ?: BigDecimal.ZERO
                        excessAmount = seed.excessAmount ?: BigDecimal.ZERO
                        this.status = status
                    },
                )
                inserted++
            }
        }
        if (skipped > 0) {
            logger.warn("Seed {}: {} policies skipped (missing references)", fileName, skipped)
        }
        logger.info("Seed {}: {} inserted, {} already present", fileName, inserted, present)
    }
}
