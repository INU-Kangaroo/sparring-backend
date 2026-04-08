package com.kangaroo.sparring.domain.record.steps.service;

import com.kangaroo.sparring.domain.record.steps.dto.req.StepSyncRequest;
import com.kangaroo.sparring.domain.record.steps.dto.res.StepDailyResponse;
import com.kangaroo.sparring.domain.record.steps.dto.res.StepSyncResponse;
import com.kangaroo.sparring.domain.record.steps.dto.res.StepTodayResponse;
import com.kangaroo.sparring.domain.record.steps.entity.StepLog;
import com.kangaroo.sparring.domain.record.steps.repository.StepLogRepository;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StepLogService {

    private final StepLogRepository stepLogRepository;
    private final UserRepository userRepository;

    @Qualifier("kstClock")
    private final Clock kstClock;

    @Transactional
    public StepSyncResponse syncStepLog(Long userId, StepSyncRequest request) {
        User user = findUser(userId);

        LocalDate today = LocalDate.now(kstClock);
        if (request.getStepDate().isAfter(today)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "걸음수 날짜는 미래일 수 없습니다.");
        }

        LocalDateTime now = LocalDateTime.now(kstClock);
        StepLog stepLog = stepLogRepository.findByUserIdAndStepDateAndSourceAndIsDeletedFalse(
                        userId,
                        request.getStepDate(),
                        request.getSource()
                )
                .map(existing -> {
                    existing.updateSteps(request.getSteps(), now);
                    return existing;
                })
                .orElseGet(() -> StepLog.create(
                        user,
                        request.getStepDate(),
                        request.getSteps(),
                        request.getSource(),
                        now
                ));

        StepLog saved = stepLogRepository.save(stepLog);
        return StepSyncResponse.from(saved);
    }

    public StepTodayResponse getTodaySteps(Long userId) {
        LocalDate today = LocalDate.now(kstClock);
        Integer totalSteps = stepLogRepository.sumStepsByUserIdAndStepDate(userId, today);
        int resolvedSteps = totalSteps != null ? totalSteps : 0;

        LocalDateTime updatedAt = stepLogRepository.findLatestSyncedAtByUserIdAndStepDate(userId, today);

        return StepTodayResponse.builder()
                .stepDate(today)
                .totalSteps(resolvedSteps)
                .updatedAt(updatedAt)
                .build();
    }

    public List<StepDailyResponse> getStepLogs(Long userId, LocalDateTime start, LocalDateTime end) {
        return stepLogRepository.findDailyStepsByUserIdAndDateRange(
                userId,
                start.toLocalDate(),
                end.toLocalDate()
        );
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
