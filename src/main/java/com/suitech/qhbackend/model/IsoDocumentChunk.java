package com.suitech.qhbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "iso_document_chunks")
public class IsoDocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_code", length = 100)
    private String documentCode;

    @Column(name = "document_name", nullable = false, length = 255)
    private String documentName;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "embedding_json", columnDefinition = "TEXT")
    private String embeddingJson;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "document_type", length = 50)
    private String documentType;

    @Column(name = "revision", length = 20)
    private String revision;

    @Column(name = "area", length = 100)
    private String area;

    @Column(name = "section", length = 50)
    private String section;

    @Column(name = "section_title", length = 255)
    private String sectionTitle;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null || status.trim().isEmpty()) {
            status = "ACTIVO";
        }
    }
}
