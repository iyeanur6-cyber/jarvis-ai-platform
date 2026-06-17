package ai.jarvis.rag;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for the documents table.
 * Spring Data R2DBC generates all implementations.
 */
@Repository
public interface DocumentRepository
        extends R2dbcRepository<Document, UUID> {

    /**
     * Find all READY documents for a user.
     * Only READY documents have searchable chunks.
     */
    Flux<Document> findByUserIdAndStatusOrderByCreatedAtDesc(
            UUID userId, DocumentStatus status);

    /**
     * Find all documents for a user (any status).
     * Used for document list UI.
     */
    Flux<Document> findByUserIdOrderByCreatedAtDesc(
            UUID userId);

    /**
     * Find specific document with ownership check.
     * Security: user can only access their documents.
     */
    Mono<Document> findByIdAndUserId(
            UUID id, UUID userId);

    /**
     * Count documents for a user.
     */
    Mono<Long> countByUserId(UUID userId);

    /**
     * Update document status and chunk count.
     * Called when processing completes.
     */
    @Modifying
    @Query("""
            UPDATE documents
            SET status = :status,
                chunk_count = :chunkCount,
                error_message = :errorMessage,
                updated_at = NOW()
            WHERE id = :documentId
            """)
    Mono<Integer> updateStatus(
            UUID documentId,
            String status,
            int chunkCount,
            String errorMessage);
}