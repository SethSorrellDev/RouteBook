package com.seth.routebook.service;

import com.seth.routebook.domain.Attachment;
import com.seth.routebook.domain.KnowledgeEntry;
import com.seth.routebook.dto.AttachmentDto;
import com.seth.routebook.exception.FileTooLargeException;
import com.seth.routebook.exception.ResourceNotFoundException;
import com.seth.routebook.exception.UnsupportedFileTypeException;
import com.seth.routebook.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private static final long PHOTO_DOC_MAX_BYTES = 25L * 1024 * 1024;   // 25MB
    private static final long VIDEO_MAX_BYTES = 250L * 1024 * 1024;      // 250MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            // Photos
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/heic", "image/heif",
            // Documents
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            // Videos
            "video/mp4", "video/quicktime", "video/webm", "video/x-msvideo"
    );

    private final AttachmentRepository attachmentRepository;
    private final KnowledgeEntryService knowledgeEntryService;
    private final S3Client r2Client;
    private final S3Presigner r2Presigner;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    @Value("${cloudflare.r2.presigned-url-expiry-minutes}")
    private long presignedUrlExpiryMinutes;

    public List<AttachmentDto> findAllForKnowledgeEntry(Long knowledgeEntryId) {
        knowledgeEntryService.getEntityOrThrow(knowledgeEntryId);
        return attachmentRepository.findByKnowledgeEntryId(knowledgeEntryId).stream()
                .map(this::toDto)
                .toList();
    }

    public AttachmentDto upload(Long knowledgeEntryId, MultipartFile file) {
        KnowledgeEntry entry = knowledgeEntryService.getEntityOrThrow(knowledgeEntryId);
        validateContentType(file.getContentType());
        validateFileSize(file.getContentType(), file.getSize());

        String r2Key = buildR2Key(knowledgeEntryId, file.getOriginalFilename());

        try {
            r2Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(r2Key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }

        Attachment attachment = new Attachment();
        attachment.setFileName(file.getOriginalFilename());
        attachment.setContentType(file.getContentType());
        attachment.setFileSizeBytes(file.getSize());
        attachment.setR2Key(r2Key);
        attachment.setKnowledgeEntry(entry);

        Attachment saved = attachmentRepository.save(attachment);
        return toDto(saved);
    }

    public void delete(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No attachment found with id " + attachmentId));

        r2Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(attachment.getR2Key())
                .build());

        attachmentRepository.delete(attachment);
    }

    private void validateContentType(String contentType) {
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new UnsupportedFileTypeException(
                    "File type '" + contentType + "' is not supported. Allowed: photos, PDFs, " +
                    "Word documents, plain text, spreadsheets, and common video formats."
            );
        }
    }

    private void validateFileSize(String contentType, long sizeBytes) {
        boolean isVideo = contentType != null && contentType.startsWith("video/");
        long limit = isVideo ? VIDEO_MAX_BYTES : PHOTO_DOC_MAX_BYTES;
        if (sizeBytes > limit) {
            String limitLabel = isVideo ? "250MB" : "25MB";
            throw new FileTooLargeException(
                    "File exceeds the " + limitLabel + " limit for its type (size: " +
                    (sizeBytes / (1024 * 1024)) + "MB)."
            );
        }
    }

    private String buildR2Key(Long knowledgeEntryId, String originalFileName) {
        String safeName = originalFileName == null ? "file" : originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "knowledge-entries/" + knowledgeEntryId + "/" + UUID.randomUUID() + "-" + safeName;
    }

    private AttachmentDto toDto(Attachment attachment) {
        String downloadUrl = r2Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(presignedUrlExpiryMinutes))
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(bucketName)
                                .key(attachment.getR2Key())
                                .build())
                        .build())
                .url()
                .toString();

        return new AttachmentDto(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getFileSizeBytes(),
                attachment.getKnowledgeEntry().getId(),
                attachment.getUploadedAt(),
                downloadUrl
        );
    }
}
