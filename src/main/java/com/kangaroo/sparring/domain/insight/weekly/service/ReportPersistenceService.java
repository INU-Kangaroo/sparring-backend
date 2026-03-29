package com.kangaroo.sparring.domain.insight.weekly.service;

import com.kangaroo.sparring.domain.insight.weekly.entity.Report;
import com.kangaroo.sparring.domain.insight.weekly.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ReportPersistenceService {

    private final ReportRepository reportRepository;

    @Transactional
    void save(Report report) {
        reportRepository.save(report);
    }
}
