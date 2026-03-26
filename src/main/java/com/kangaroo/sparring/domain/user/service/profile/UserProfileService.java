package com.kangaroo.sparring.domain.user.service.profile;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;
import com.kangaroo.sparring.domain.measurement.repository.BloodSugarLogRepository;
import com.kangaroo.sparring.domain.user.dto.req.UpdateUserProfileRequest;
import com.kangaroo.sparring.domain.user.dto.res.UserDashboardResponse;
import com.kangaroo.sparring.domain.user.dto.res.UserProfileResponse;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserRepository userRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final BloodSugarLogRepository bloodSugarLogRepository;

    public UserProfileResponse getProfile(Long userId) {
        User user = getUserOrThrow(userId);
        HealthProfile profile = healthProfileRepository.findByUserId(userId).orElse(null);
        return UserProfileResponse.of(user, profile);
    }

    public UserDashboardResponse getDashboard(Long userId) {
        User user = getUserOrThrow(userId);
        HealthProfile profile = healthProfileRepository.findByUserId(userId).orElse(null);

        Long totalCount = bloodSugarLogRepository.countByUserId(userId);
        BigDecimal average = roundOneDecimal(bloodSugarLogRepository.findAverageGlucoseByUserId(userId));
        BigDecimal last7DaysAverage = getLast7DaysAverage(userId);
        int streak = calculateConsecutiveMeasurementDays(userId);

        LocalDate birthDate = profile != null ? profile.getBirthDate() : user.getBirthDate();
        var gender = profile != null && profile.getGender() != null ? profile.getGender() : user.getGender();
        BigDecimal height = profile != null ? profile.getHeight() : null;
        BigDecimal weight = profile != null ? profile.getWeight() : null;

        return UserDashboardResponse.builder()
                .profile(UserDashboardResponse.Profile.builder()
                        .username(user.getUsername())
                        .profileImageUrl(user.getProfileImageUrl())
                        .build())
                .record(UserDashboardResponse.Record.builder()
                        .totalMeasurementCount(totalCount != null ? totalCount : 0L)
                        .consecutiveMeasurementDays(streak)
                        .averageBloodSugarMgDl(average)
                        .last7DaysAverageBloodSugarMgDl(last7DaysAverage)
                        .build())
                .basicInfo(UserDashboardResponse.BasicInfo.builder()
                        .name(user.getUsername())
                        .birthDate(birthDate)
                        .gender(gender)
                        .heightCm(height)
                        .weightKg(weight)
                        .build())
                .build();
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        User user = getUserOrThrow(userId);
        HealthProfile profile = getOrCreateHealthProfile(user);

        if (request.getUsername() != null) {
            user.updateUsername(request.getUsername());
        }
        if (request.getBirthDate() != null) {
            user.updateBirthDate(request.getBirthDate());
        }
        if (request.getGender() != null) {
            user.updateGender(request.getGender());
        }
        if (request.getProfileImageUrl() != null) {
            user.updateProfileImageUrl(request.getProfileImageUrl());
        }

        profile.updateProfile(
                request.getBirthDate(),
                request.getGender(),
                request.getHeight(),
                request.getWeight(),
                null,
                null,
                null,
                null,
                null,
                null
        );

        healthProfileRepository.save(profile);
        return UserProfileResponse.of(user, profile);
    }

    private BigDecimal getLast7DaysAverage(Long userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        Double average = bloodSugarLogRepository.findAverageGlucoseByUserIdAndDateRange(
                userId, startDateTime, endDateTime
        );
        return roundOneDecimal(average);
    }

    private int calculateConsecutiveMeasurementDays(Long userId) {
        List<BloodSugarLog> logs = bloodSugarLogRepository.findByUserIdAndIsDeletedFalseOrderByMeasurementTimeDesc(userId);
        Set<LocalDate> dateSet = new LinkedHashSet<>();
        for (BloodSugarLog log : logs) {
            dateSet.add(log.getMeasurementTime().toLocalDate());
        }

        List<LocalDate> measuredDates = dateSet.stream().toList();
        if (measuredDates.isEmpty()) {
            return 0;
        }

        LocalDate expected = LocalDate.now();
        int streak = 0;

        for (LocalDate measuredDate : measuredDates) {
            if (measuredDate == null) {
                continue;
            }
            if (!measuredDate.equals(expected)) {
                break;
            }
            streak++;
            expected = expected.minusDays(1);
        }

        return streak;
    }

    private BigDecimal roundOneDecimal(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private HealthProfile getOrCreateHealthProfile(User user) {
        return healthProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> healthProfileRepository.save(HealthProfile.builder().user(user).build()));
    }
}
