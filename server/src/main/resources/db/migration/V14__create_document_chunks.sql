-- ═══════════════════════════════════════════════════
-- V14: Create Document Chunks Table
-- Phase 3: RAG Engine
--
-- Stores individual chunks from documents.
-- Each chunk has:
-- → The text content (for injection into prompts)
-- → A pgvector embedding (for semantic search)
-- → Position metadata (for source citation)
--
-- CHUNKING STRATEGY:
-- chunk_size:    500 tokens (~375 words)
-- chunk_overlap: 50 tokens (~37 words)
-- This preserves context across boundaries.
-- ═══════════════════════════════════════════════════

CREATE TABLE document_chunks (

                                 id              UUID            NOT NULL
                                                                          DEFAULT gen_random_uuid(),

                                 document_id     UUID            NOT NULL,

                                 user_id         UUID            NOT NULL,

    -- The actual text of this chunk
    -- Used when injecting into AI prompt
                                 content         TEXT            NOT NULL,

    -- Position of this chunk in the document
    -- chunk_index=0 is the first chunk
                                 chunk_index     INTEGER         NOT NULL DEFAULT 0,

    -- Approximate page number (for citation)
    -- NULL if page tracking not available
                                 page_number     INTEGER,

    -- Token count estimate for this chunk
    -- Used for context window budget calculation
                                 token_count     INTEGER         NOT NULL DEFAULT 0,

    -- pgvector embedding of the chunk content
    -- 768 dimensions from nomic-embed-text
    -- NULL until embedding is generated
                                 embedding       vector(768),

                                 created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────

                                 CONSTRAINT pk_document_chunks
                                     PRIMARY KEY (id),

                                 CONSTRAINT fk_chunks_document
                                     FOREIGN KEY (document_id)
                                         REFERENCES documents (id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT fk_chunks_user
                                     FOREIGN KEY (user_id)
                                         REFERENCES users (id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT chk_chunks_content_not_empty
                                     CHECK (LENGTH(TRIM(content)) > 0),

                                 CONSTRAINT chk_chunks_index_positive
                                     CHECK (chunk_index >= 0),

                                 CONSTRAINT chk_chunks_token_count
                                     CHECK (token_count >= 0)
);

-- ── Indexes ───────────────────────────────────────

-- All chunks for a document (ordered by position)
CREATE INDEX idx_chunks_document_id
    ON document_chunks (document_id, chunk_index ASC);

-- All chunks for a user (cross-document search)
CREATE INDEX idx_chunks_user_id
    ON document_chunks (user_id);

-- Chunks WITH embeddings (for semantic search)
-- Partial index = only indexes rows where embedding IS NOT NULL
-- Smaller index, faster queries for search
CREATE INDEX idx_chunks_embedding_not_null
    ON document_chunks (user_id)
    WHERE embedding IS NOT NULL;

-- ── Semantic Search Function ──────────────────────

CREATE OR REPLACE FUNCTION search_chunks_by_embedding(
    p_user_id       UUID,
    p_embedding     vector(768),
    p_limit         INTEGER DEFAULT 5,
    p_min_similarity FLOAT DEFAULT 0.5,
    p_document_id   UUID DEFAULT NULL
)
RETURNS TABLE (
    id              UUID,
    document_id     UUID,
    content         TEXT,
    chunk_index     INTEGER,
    page_number     INTEGER,
    token_count     INTEGER,
    similarity      FLOAT
)
LANGUAGE SQL
STABLE
AS $$
SELECT
    c.id,
    c.document_id,
    c.content,
    c.chunk_index,
    c.page_number,
    c.token_count,
    1 - (c.embedding <=> p_embedding) AS similarity
FROM document_chunks c
WHERE
    c.user_id = p_user_id
  AND c.embedding IS NOT NULL
  AND 1 - (c.embedding <=> p_embedding)
    >= p_min_similarity
  -- Optional: filter to specific document
  AND (p_document_id IS NULL
    OR c.document_id = p_document_id)
ORDER BY
    c.embedding <=> p_embedding ASC
    LIMIT p_limit;
$$;

COMMENT ON FUNCTION search_chunks_by_embedding IS
    'Semantic search across document chunks using cosine similarity.
     Optional p_document_id filters to a single document.
     Returns chunks ordered by relevance (most similar first).';

COMMENT ON TABLE document_chunks IS
    'Individual chunks from uploaded documents.
     Each chunk has a pgvector embedding for semantic search.
     Chunks are injected into AI prompts as relevant context.';