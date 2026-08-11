package com.suitech.qhbackend.repository;

import com.suitech.qhbackend.model.SapNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SapNoticeRepository extends JpaRepository<SapNotice, Long> {

    List<SapNotice> findByReportYearAndReportMonth(Integer reportYear, Integer reportMonth);

    void deleteByReportYearAndReportMonth(Integer reportYear, Integer reportMonth);
}
