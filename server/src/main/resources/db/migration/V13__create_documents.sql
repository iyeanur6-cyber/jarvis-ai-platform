-- ═══════════════════════════════════════════════════
-- V13: Create Documents Table
-- Phase 3: RAG Engine
--
-- Stores metadata about uploaded documents.
-- Actual document content is chunked and stored
-- in document_chunks table (V13).
--
-- Document lifecycle:
-- PENDING   → uploaded, waiting to process
-- PROCESSING → currently chunking + embedding
-- READY     → all chunks embedded, ready for search
-- FAILED    → processing failed, can retry
-- ═══════════════════════════════════════════════════

CREATE TABLE documents (

                           id              UUID            NOT NULL
                                                                    DEFAULT gen_random_uuid(),

                           user_id         UUID            NOT NULL,

    -- Original filename as uploaded by user
                           filename        VARCHAR(500)    NOT NULL,

    -- File type for parser selection
    -- Values: PDF, TXT, MARKDOWN
                           file_type       VARCHAR(20)     NOT NULL,

    -- File size in bytes (for display + limits)
                           file_size_bytes BIGINT          NOT NULL DEFAULT 0,

    -- Processing status
                           status          VARCHAR(20)     NOT NULL
                                                                    DEFAULT 'PENDING',

    -- Total chunks created during processing
    -- 0 = not yet processed
                           chunk_count     INTEGER         NOT NULL DEFAULT 0,

    -- Optional user-provided description
                           description     TEXT,

    -- Error message if processing failed
                           error_message   TEXT,

                           created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                           updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────

                           CONSTRAINT pk_documents
                               PRIMARY KEY (id),

                           CONSTRAINT fk_documents_user
                               FOREIGN KEY (user_id)
                                   REFERENCES users (id)
                                   ON DELETE CASCADE,

                           CONSTRAINT chk_documents_file_type
                               CHECK (file_type IN ('PDF', 'TXT', 'MARKDOWN')),

                           CONSTRAINT chk_documents_status
                               CHECK (status IN (
                                                 'PENDING',
                                                 'PROCESSING',
                                                 'READY',
                                                 'FAILED'
                                   )),

                           CONSTRAINT chk_documents_filename_not_empty
                               CHECK (LENGTH(TRIM(filename)) > 0),

                           CONSTRAINT chk_documents_file_size
                               CHECK (file_size_bytes >= 0)
);

-- ── Indexes ───────────────────────────────────────

-- User's documents (most common query)
CREATE INDEX idx_documents_user_id
    ON documents (user_id);

-- Filter by status (find READY documents for search)
CREATE INDEX idx_documents_user_status
    ON documents (user_id, status);

-- ── Auto-update trigger ───────────────────────────

CREATE TRIGGER trigger_documents_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ── Comments ──────────────────────────────────────

COMMENT ON TABLE documents IS
    'Documents uploaded by users for RAG search.
     Actual content stored in document_chunks table.';

COMMENT ON COLUMN documents.status IS
    'PENDING: uploaded, awaiting processing.
     PROCESSING: chunking and embedding in progress.
     READY: all chunks embedded, available for search.
     FAILED: processing error, see error_message.';

COMMENT ON COLUMN documents.chunk_count IS
    'Number of chunks created from this document.
     Updated when processing completes.
     Use to estimate search coverage.';