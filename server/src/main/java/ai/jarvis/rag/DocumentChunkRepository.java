package ai.jarvis.rag;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for document_chunks table.
 *
 * NOTE: Vector/embedding operations are in
 * RagSearchRepository (uses JdbcTemplate).
 * This repository handles standard CRUD only.
 */
@Repository
public interface DocumentChunkRepository
        extends R2dbcRepository<DocumentChunk, UUID> {

    /**
     * All chunks for a document in order.
     * Used to verify processing or for export.
     */
    Flux<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(
            UUID documentId);

    /**
     * Count chunks for a document.
     * Used to update documents.chunk_count after processing.
     */
    Mono<Long> countByDocumentId(UUID documentId);

    /**
     * Delete all chunks for a document.
     * Called when document is deleted or reprocessed.
     */
    Mono<Void> deleteByDocumentId(UUID documentId);

    /**
     * All chunks for a user across all documents.
     * Used for cross-document semantic search prep.
     */
    Flux<DocumentChunk> findByUserId(UUID userId);
}