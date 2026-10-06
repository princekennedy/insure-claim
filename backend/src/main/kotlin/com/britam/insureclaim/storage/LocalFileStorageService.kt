package com.britam.insureclaim.storage

import com.britam.insureclaim.common.BadRequestException
import com.britam.insureclaim.security.StorageProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.LocalDate
import java.util.UUID

data class StoredFile(
	val storagePath: String,
	val originalFileName: String,
	val contentType: String,
	val sizeBytes: Long,
	val checksum: String,
)

/**
 * Local filesystem storage for claim evidence and KYC images.
 *
 * Paths are namespaced by date and entity id and the stored name is always
 * generated, so a hostile file name can never escape the upload root or
 * overwrite an existing file.
 */
@Component
class LocalFileStorageService(
	private val properties: StorageProperties,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	private val root: Path by lazy {
		val path = Paths.get(properties.uploadDir).toAbsolutePath().normalize()
		Files.createDirectories(path)
		path
	}

	fun store(
		file: MultipartFile,
		namespace: String,
		entityId: Long,
	): StoredFile {
		validate(file)
		val today = LocalDate.now()
		val directory = root.resolve(namespace).resolve(entityId.toString()).resolve(today.toString())
		Files.createDirectories(directory)

		val storedName = "${UUID.randomUUID()}${extensionOf(file.originalFilename)}"
		val target = directory.resolve(storedName).normalize()
		require(target.startsWith(root)) { "Resolved storage path escapes the upload root" }

		file.inputStream.use { input ->
			Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
		}
		val size = Files.size(target)
		val checksum = sha256(target)

		log.debug("Stored {} for {}/{} ({} bytes)", file.originalFilename, namespace, entityId, size)
		return StoredFile(
			storagePath = root.relativize(target).toString().replace('\\', '/'),
			originalFileName = sanitiseFileName(file.originalFilename),
			contentType = file.contentType ?: "application/octet-stream",
			sizeBytes = size,
			checksum = checksum,
		)
	}

	fun load(storagePath: String): ByteArray {
		val resolved = resolveSafely(storagePath)
		if (!Files.exists(resolved)) {
			throw BadRequestException("Stored file is no longer available", "FILE_NOT_FOUND")
		}
		return Files.readAllBytes(resolved)
	}

	fun loadAsStream(storagePath: String): InputStream {
		val resolved = resolveSafely(storagePath)
		if (!Files.exists(resolved)) {
			throw BadRequestException("Stored file is no longer available", "FILE_NOT_FOUND")
		}
		return Files.newInputStream(resolved)
	}

	fun delete(storagePath: String) {
		try {
			Files.deleteIfExists(resolveSafely(storagePath))
		} catch (ex: Exception) {
			// Orphaned files are cleaned up by the scheduled maintenance task.
			log.warn("Could not delete stored file {}: {}", storagePath, ex.message)
		}
	}

	fun deleteForEntity(namespace: String, entityId: Long) {
		try {
			val directory = root.resolve(namespace).resolve(entityId.toString())
			if (Files.exists(directory)) {
				Files.walk(directory).use { stream ->
					stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
				}
			}
		} catch (ex: Exception) {
			log.warn("Could not purge {} for entity {}: {}", namespace, entityId, ex.message)
		}
	}

	private fun resolveSafely(storagePath: String): Path {
		val resolved = root.resolve(storagePath).normalize()
		if (!resolved.startsWith(root)) {
			throw BadRequestException("Invalid storage reference", "INVALID_STORAGE_PATH")
		}
		return resolved
	}

	private fun validate(file: MultipartFile) {
		if (file.isEmpty) {
			throw BadRequestException("Uploaded file is empty", "EMPTY_FILE")
		}
		if (file.size > properties.maxFileSizeBytes) {
			throw BadRequestException(
				"File is larger than the ${properties.maxFileSizeBytes / (1024 * 1024)} MB limit",
				"FILE_TOO_LARGE",
			)
		}
		val contentType = file.contentType?.lowercase()
		if (contentType == null || !properties.allowedContentTypes.contains(contentType)) {
			throw BadRequestException(
				"Unsupported file type '${file.contentType}'. Allowed: ${properties.allowedContentTypes.joinToString()}",
				"UNSUPPORTED_FILE_TYPE",
			)
		}
	}

	private fun extensionOf(fileName: String?): String {
		if (fileName.isNullOrBlank()) return ""
		val dot = fileName.lastIndexOf('.')
		if (dot < 0 || dot == fileName.length - 1) return ""
		val ext = fileName.substring(dot + 1).lowercase()
		return if (ext.matches(Regex("^[a-z0-9]{1,8}$"))) ".$ext" else ""
	}

	private fun sanitiseFileName(fileName: String?): String {
		if (fileName.isNullOrBlank()) return "upload"
		val cleaned = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(180)
		return cleaned.ifBlank { "upload" }
	}

	private fun sha256(path: Path): String {
		val digest = MessageDigest.getInstance("SHA-256")
		Files.newInputStream(path).use { input ->
			val buffer = ByteArray(8192)
			while (true) {
				val read = input.read(buffer)
				if (read <= 0) break
				digest.update(buffer, 0, read)
			}
		}
		return digest.digest().joinToString("") { "%02x".format(it) }
	}
}
