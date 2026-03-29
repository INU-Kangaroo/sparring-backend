package com.kangaroo.sparring.domain.catalog.repository;

import com.kangaroo.sparring.domain.catalog.entity.Food;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {

    /**
     * 활성 음식명 중복 조회 (공백/대소문자 정규화)
     */
    @Query("SELECT f FROM Food f WHERE f.isDeleted = false AND LOWER(TRIM(f.name)) = LOWER(TRIM(:name))")
    Optional<Food> findActiveByNormalizedName(@Param("name") String name);

    /**
     * ID로 조회
     */
    @Query("SELECT f FROM Food f WHERE f.id = :id AND f.isDeleted = false")
    Optional<Food> findByIdWithNutrition(@Param("id") Long id);

    /**
     * food_code로 ID 조회
     */
    @Query("SELECT f.id FROM Food f WHERE f.foodCode = :foodCode AND f.isDeleted = false")
    Optional<Long> findIdByFoodCode(@Param("foodCode") String foodCode);

    @Query("SELECT f FROM Food f WHERE f.foodCode IN :foodCodes AND f.isDeleted = false")
    List<Food> findActiveByFoodCodeIn(@Param("foodCodes") List<String> foodCodes);

    /**
     * 정확한 이름으로 조회
     */
    @Query("""
            SELECT f
            FROM Food f
            WHERE f.isDeleted = false
              AND LOWER(TRIM(f.name)) = LOWER(TRIM(:keyword))
            ORDER BY f.name ASC
            """)
    List<Food> searchActiveByExactName(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT f
            FROM Food f
            WHERE f.isDeleted = false
              AND LOWER(f.name) LIKE LOWER(CONCAT(:keyword, '%'))
            ORDER BY f.name ASC
            """)
    List<Food> searchActiveByPrefixName(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT f
            FROM Food f
            WHERE f.isDeleted = false
              AND LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY f.name ASC
            """)
    List<Food> searchActiveByName(@Param("keyword") String keyword, Pageable pageable);
}
