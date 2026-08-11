package com.suitech.qhbackend.repository;

import com.suitech.qhbackend.model.DailyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {

    Optional<DailyReport> findByReportDate(LocalDate reportDate);

    List<DailyReport> findByYearNumberAndMonthNumberOrderByDayNumberAsc(Integer yearNumber, Integer monthNumber);

    @Query("SELECT DISTINCT d.yearNumber, d.monthNumber FROM DailyReport d ORDER BY d.yearNumber DESC, d.monthNumber DESC")
    List<Object[]> findAvailableMonths();
}
