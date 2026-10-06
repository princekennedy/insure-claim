package com.britam.insureclaim.storage

import com.britam.insureclaim.common.BadRequestException
import com.britam.insureclaim.security.StorageProperties
import io.minio.MinioClient
import io.minio.PutObjectOptions
import io.minio.errors.MinioException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.time.LocalDate
import java.util.UUID

/**
 * MinIO/S3-compatible object storage for claim evidence and KYC images.
 *
 * Paths are namespaced by date and entity id and the stored name is always
 * generated, so a hostile file name can never overwrite an existing file.
 *
 * Requires these environment variables or application properties:
 * - MINIO_ENDPOINT (e.g., minio:9000 or localhost:9000)
 * - MINIO_ACCESS_KEY
 * - MINIO_SECRET_KEY
 * - MINIO_BUCKET (default: insureclaim)
 * - MINIO_USE_HTTPS (default: false)
 * - MINIO_URL_EXPIRY_SECONDS (default: 3600)
 */
@Component
class MinioFileStorageService(
	private val properties: StorageProperties,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	private val minioClient: MinioClient by lazy {
		MinioClient.builder()
			.endpoint(properties.minioEndpoint)
			.credentials(properties.minioAccessKey, properties.minioSecretKey)
			.secure(properties.minioUseHttps)
			.build()
	}

	private val bucketName: String by lazy {
		val bucket = properties.minioBucket
		if (!minioClient.bucketExists(io.minio.GetBucketInfoRequest.builder().bucket(bucket).build())) {
			minioClient.makeBucket(io.minio.MakeBucketRequest.builder().bucket(bucket).build())
			log.info("Created MinIO bucket: {}", bucket)
		}
		bucket
	}

	fun store(
		file: MultipartFile,
		namespace: String,
		entityId: Long,
	): StoredFile {
		validate(file)
		val today = LocalDate.now()
		val storedName = "${UUID.randomUUID()}${extensionOf(file.originalFilename)}"
		val objectName = "$namespace/$entityId/$today/$storedName"

		valInputStream = file.inputStream
		val size = file.size.toLong()
		val contentType = file.contentType ?: "application/octet-stream"

		val options = PutObjectOptions()
		options.setContentType(contentType)

		minioClient.putObject(
			io.minio.PutObjectRequest.builder()
				.bucket(bucketName)
				.object(objectName)
				.stream(minInputStream)
				.size(size)
				.build(),
			options,
		)

		val checksum = calculateChecksum(file.inputStream)

		log.debug("Stored {} for {}/{} in MinIO bucket {}", file.originalFilename, namespace, entityId, bucketName)
		return StoredFile(
			storagePath = objectName,
			originalFileName = sanitiseFileName(file.originalFilename),
			contentType = contentType,
			sizeBytes = size,
			checksum = checksum,
		)
	}

	fun load(storagePath: String): ByteArray {
		val stream = loadAsStream(storagePath)
		return stream.use { it.readAllBytes() }
	}

	fun loadAsStream(storagePath: String): InputStream {
		return minioClient.getObject(
			io.minio.GetObjectRequest.builder()
				.bucket(bucketName)
				.object(storagePath)
				.build(),
		)
	}

	fun getDownloadUrl(storagePath: String): String {
		val expiry = properties.minioUrlExpirySeconds
		return minioClient.getPresignedObjectUrl(
			io.minio.GetPresignedObjectUrlRequest.builder()
				.bucket(bucketName)
				.object(storagePath)
				.method(io.minio.HttpMethod.GET)
				.expiry(expiry)
				.build(),
		)
	}

	fun delete(storagePath: String) {
		minioClient.removeObject(
			io.minio.RemoveObjectRequest.builder()
				.bucket(bucketName)
				.object(storagePath)
				.build(),
		)
		log.debug("Deleted {} from MinIO bucket {}", storagePath, bucketName)
	}

	fun deleteForEntity(namespace: String, entityId: Long) {
		// MinIO doesn't support bulk delete by prefix in a single call
		// We'd need to list objects and delete them one by one, or use the RemoveObjects API
		log.info("Purging MinIO objects for {}/{} - implementation depends on listing capability", namespace, entityId)
	}

	fun getPublicUrl(storagePath: String): String {
		// For public buckets, return the direct URL
		// For private buckets, use presigned URLs (getDownloadUrl)
		if (properties.minioPublicBucket) {
			return "${properties.minioUrlBase}/$bucketName/$storagePath"
		}
		return getDownloadUrl(storagePath)
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

	private fun calculateChecksum(inputStream: InputStream): String {
		val digest = MessageDigest.getInstance("SHA-256")
		inputStream.use { stream ->
			val buffer = ByteArray(8192)
			while (true) {
				val read = stream.read(buffer)
				if (read <= 0) break
				digest.update(buffer, 0, read)
			}
		}
		return digest.digest().joinToString("") { "%02x".format(it) }
	}
}
