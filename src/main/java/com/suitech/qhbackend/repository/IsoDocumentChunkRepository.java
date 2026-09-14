package com.suitech.qhbackend.repository;

import com.suitech.qhbackend.model.IsoDocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IsoDocumentChunkRepository extends JpaRepository<IsoDocumentChunk, Long> {

    List<IsoDocumentChunk> findByCategory(String category);

    long countByCategory(String category);

    List<IsoDocumentChunk> findByDocumentCode(String documentCode);

    void deleteByDocumentName(String documentName);

    @Query("SELECT c FROM IsoDocumentChunk c WHERE (c.status IS NULL OR c.status = 'ACTIVO') AND (:category IS NULL OR c.category = :category)")
    List<IsoDocumentChunk> findAllWithCategoryFilter(@Param("category") String category);

    @Query("SELECT c FROM IsoDocumentChunk c WHERE (c.status IS NULL OR c.status = 'ACTIVO')")
    List<IsoDocumentChunk> findAllActive();

    @Query("SELECT c FROM IsoDocumentChunk c WHERE (c.status IS NULL OR c.status = 'ACTIVO') AND c.category = :category")
    List<IsoDocumentChunk> findActiveByCategory(@Param("category") String category);

    @Query("SELECT DISTINCT c.documentName, c.category, c.documentCode FROM IsoDocumentChunk c WHERE (c.status IS NULL OR c.status = 'ACTIVO') ORDER BY c.category, c.documentName")
    List<Object[]> findDistinctDocumentSummaries();
}
