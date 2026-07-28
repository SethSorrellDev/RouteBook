package com.seth.routebook.service;

import com.seth.routebook.domain.Attachment;
import com.seth.routebook.domain.KnowledgeEntry;
import com.seth.routebook.exception.FileTooLargeException;
import com.seth.routebook.exception.UnsupportedFileTypeException;
import com.seth.routebook.repository.AttachmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AttachmentService's content-type and size validation -
 * the checks that must run BEFORE any call reaches R2. S3Client and
 * S3Presigner are mocked, so these tests never touch real Cloudflare
 * infrastructure or require R2 credentials.
 */
@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private KnowledgeEntryService knowledgeEntryService;

    @Mock
    private S3Client r2Client;

    @Mock
    private S3Presigner r2Presigner;

    @InjectMocks
    private AttachmentService attachmentService;

    private KnowledgeEntry entry;

    @BeforeEach
    void setUp() throws Exception {
        // @Value fields aren't populated outside a Spring context, so set
        // them directly for this plain Mockito unit test.
        ReflectionTestUtils.setField(attachmentService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(attachmentService, "presignedUrlExpiryMinutes", 15L);

        entry = new KnowledgeEntry();
        entry.setId(1L);
    }

    @Test
    void upload_withDisallowedContentType_throwsBeforeTouchingR2() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/x-msdownload", "fake content".getBytes());

        assertThatThrownBy(() -> attachmentService.upload(1L, file))
                .isInstanceOf(UnsupportedFileTypeException.class)
                .hasMessageContaining("not supported");

        verifyNoInteractions(r2Client);
        verifyNoInteractions(attachmentRepository);
    }

    @Test
    void upload_withOversizedPhoto_throwsFileTooLargeException() {
        when(knowledgeEntryService.getEntityOrThrow(1L)).thenReturn(entry);

        // 26MB of content - over the 25MB photo/document limit
        byte[] oversized = new byte[26 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", oversized);

        assertThatThrownBy(() -> attachmentService.upload(1L, file))
                .isInstanceOf(FileTooLargeException.class)
                .hasMessageContaining("25MB");

        verifyNoInteractions(r2Client);
    }

    @Test
    void upload_withOversizedVideo_throwsFileTooLargeException() {
        when(knowledgeEntryService.getEntityOrThrow(1L)).thenReturn(entry);

        // A file just over 25MB is fine for video (limit is 250MB) - use
        // getSize() override via a lightweight fake rather than allocating
        // 251MB in a test, since MockMultipartFile derives size from the
        // actual byte array length.
        byte[] mediumContent = new byte[10];
        MockMultipartFile file = new MockMultipartFile(
                "file", "clip.mp4", "video/mp4", mediumContent) {
            @Override
            public long getSize() {
                return 251L * 1024 * 1024; // 251MB - over the video limit
            }
        };

        assertThatThrownBy(() -> attachmentService.upload(1L, file))
                .isInstanceOf(FileTooLargeException.class)
                .hasMessageContaining("250MB");

        verifyNoInteractions(r2Client);
    }

    @Test
    void upload_withValidPhoto_savesAttachmentAndReturnsPresignedUrl() throws Exception {
        when(knowledgeEntryService.getEntityOrThrow(1L)).thenReturn(entry);
        when(r2Client.putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
                any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        when(attachmentRepository.save(any(Attachment.class)))
                .thenAnswer(invocation -> {
                    Attachment a = invocation.getArgument(0);
                    a.setId(1L);
                    return a;
                });

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create("https://test.r2.cloudflarestorage.com/test-bucket/file.jpg").toURL());
        when(r2Presigner.presignGetObject(any(software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        MockMultipartFile file = new MockMultipartFile(
                "file", "gate-photo.jpg", "image/jpeg", "fake image bytes".getBytes());

        var result = attachmentService.upload(1L, file);

        assertThat(result.fileName()).isEqualTo("gate-photo.jpg");
        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.downloadUrl()).contains("test-bucket");
        verify(r2Client, times(1)).putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
                any(software.amazon.awssdk.core.sync.RequestBody.class));
    }
}
