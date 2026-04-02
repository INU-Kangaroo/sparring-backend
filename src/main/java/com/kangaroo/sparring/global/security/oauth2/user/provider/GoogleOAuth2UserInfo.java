package com.kangaroo.sparring.global.security.oauth2.user.provider;

import com.kangaroo.sparring.domain.user.type.Gender;
import com.kangaroo.sparring.global.security.oauth2.user.OAuth2UserInfo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getProvider() {
        return "GOOGLE";
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getProfileImageUrl() {
        return (String) attributes.get("picture");
    }

    @Override
    public LocalDate getBirthDate() {
        Object birthdaysValue = attributes.get("birthdays");
        if (!(birthdaysValue instanceof List<?> birthdays)) {
            return null;
        }

        for (Object birthdayObj : birthdays) {
            if (!(birthdayObj instanceof Map<?, ?> birthdayMap)) {
                continue;
            }
            Object dateValue = birthdayMap.get("date");
            if (!(dateValue instanceof Map<?, ?> dateMap)) {
                continue;
            }
            Integer year = toInteger(dateMap.get("year"));
            Integer month = toInteger(dateMap.get("month"));
            Integer day = toInteger(dateMap.get("day"));
            if (year == null || month == null || day == null) {
                continue;
            }
            try {
                return LocalDate.of(year, month, day);
            } catch (RuntimeException ignored) {
                // ignore malformed date entry and continue
            }
        }
        return null;
    }

    @Override
    public Gender getGender() {
        Object gendersValue = attributes.get("genders");
        if (!(gendersValue instanceof List<?> genders) || genders.isEmpty()) {
            return null;
        }

        Object firstGenderObj = genders.get(0);
        if (!(firstGenderObj instanceof Map<?, ?> genderMap)) {
            return null;
        }
        Object rawValue = genderMap.get("value");
        if (rawValue == null) {
            return null;
        }

        return switch (String.valueOf(rawValue).toLowerCase()) {
            case "male" -> Gender.MALE;
            case "female" -> Gender.FEMALE;
            default -> Gender.OTHER;
        };
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
