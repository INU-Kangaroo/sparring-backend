package com.kangaroo.sparring.domain.survey.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Allergy {
    MILK("우유"),
    EGG("달걀"),
    PEANUT("땅콩"),
    TREE_NUT("견과류"),
    WHEAT("밀"),
    SOY("대두"),
    SHRIMP("새우"),
    CRAB("게"),
    FISH("생선"),
    SHELLFISH("조개류");

    private final String description;
}
