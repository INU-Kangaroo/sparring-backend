package com.kangaroo.sparring.domain.catalog.controller;

import com.kangaroo.sparring.domain.food.catalog.dto.res.FoodDetailResponse;
import com.kangaroo.sparring.domain.food.catalog.dto.res.FoodResponse;
import com.kangaroo.sparring.domain.food.catalog.service.FoodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "음식 카탈로그", description = "음식 기준 데이터 조회 API")
@RestController
@RequestMapping("/api/catalog/foods")
@RequiredArgsConstructor
public class FoodCatalogController {

    private final FoodService foodService;

    @Operation(summary = "음식 검색")
    @GetMapping("/search")
    public ResponseEntity<List<FoodResponse>> searchFood(
            @Parameter(description = "검색 키워드", example = "닭가슴살")
            @RequestParam String keyword,
            @Parameter(description = "페이지 크기 (1~50)", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(required = false) Integer page
    ) {
        return ResponseEntity.ok(foodService.searchFood(keyword, size, page));
    }

    @Operation(summary = "음식 상세 조회")
    @GetMapping("/{foodId}")
    public ResponseEntity<FoodDetailResponse> getFoodDetail(@PathVariable Long foodId) {
        return ResponseEntity.ok(foodService.getFoodDetail(foodId));
    }
}
