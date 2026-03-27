package com.kangaroo.sparring.domain.recommendation.service;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.service.HealthProfileService;
import com.kangaroo.sparring.domain.record.common.read.BloodPressureRecord;
import com.kangaroo.sparring.domain.record.common.read.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.read.RecordReadService;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationContextService {

    private final UserRepository userRepository;
    private final HealthProfileService healthProfileService;
    private final RecordReadService recordReadService;

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public HealthProfile getOrCreateHealthProfile(Long userId) {
        return healthProfileService.getOrCreateHealthProfile(userId);
    }

    public List<BloodSugarRecord> getRecentBloodSugars(Long userId, int count) {
        return recordReadService.getRecentBloodSugarRecords(userId, count);
    }

    public List<BloodPressureRecord> getRecentBloodPressures(Long userId, int count) {
        return recordReadService.getRecentBloodPressureRecords(userId, count);
    }
}
