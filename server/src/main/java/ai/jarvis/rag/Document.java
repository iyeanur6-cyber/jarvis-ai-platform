package ai.jarvis.rag;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an uploaded document in the RAG system.
 *
 * Maps to the 'documents' table (V12 migration).
 * Stores metadata only — actual content in document_chunks.
 *
 * IMMUTABLE RECORD:
 * Use factory methods to create instances.
 * Use withXxx() methods to create updated copies.
 */
@Table("documents")
public record Document(

        @Id
        UUID id,

        @Column("user_id")
        UUID userId,

        String filename,

        @Column("file_type")
        DocumentFileType fileType,

        @Column("file_size_bytes")
        long fileSizeBytes,

        DocumentStatus status,

        @Column("chunk_count")
        int chunkCount,

        String description,

        @Column("error_message")
        String errorMessage,

        @Column("created_at")
        Instant createdAt,

        @Column("updated_at")
        Instant updatedAt

) {
    /**
     * Create a new document in PENDING status.
     * Called when user uploads a document.
     *
     * @param userId       owner of this document
     * @param filename     original filename
     * @param fileType     PDF/TXT/MARKDOWN
     * @param fileSizeBytes file size in bytes
     * @param description  optional user description
     */
    public static Document create(
            UUID userId,
            String filename,
            DocumentFileType fileType,
            long fileSizeBytes,
            String description) {
        return new Document(
                UUID.randomUUID(),
                userId,
                filename,
                fileType,
                fileSizeBytes,
                DocumentStatus.PENDING,
                0,
                description,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * Create copy with PROCESSING status.
     * Called when background processing starts.
     */
    public Document withProcessing() {
        return new Document(
                id, userId, filename, fileType,
                fileSizeBytes,
                DocumentStatus.PROCESSING,
                chunkCount, description, null,
                createdAt, Instant.now()
        );
    }

    /**
     * Create copy with READY status and chunk count.
     * Called when all chunks are embedded successfully.
     *
     * @param totalChunks number of chunks created
     */
    public Document withReady(int totalChunks) {
        return new Document(
                id, userId, filename, fileType,
                fileSizeBytes,
                DocumentStatus.READY,
                totalChunks, description, null,
                createdAt, Instant.now()
        );
    }

    /**
     * Create copy with FAILED status and error message.
     * Called when processing encounters an error.
     *
     * @param error description of what went wrong
     */
    public Document withFailed(String error) {
        return new Document(
                id, userId, filename, fileType,
                fileSizeBytes,
                DocumentStatus.FAILED,
                chunkCount, description, error,
                createdAt, Instant.now()
        );
    }
}