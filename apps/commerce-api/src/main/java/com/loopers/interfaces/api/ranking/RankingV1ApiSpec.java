package com.loopers.interfaces.api.ranking;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

@Tag(name = "Ranking V1 API", description = "상품 랭킹 조회 API")
public interface RankingV1ApiSpec {

    @Operation(
            summary = "랭킹 조회",
            description = "기간별(DAILY/WEEKLY/MONTHLY) 상품 랭킹을 조회합니다."
    )
    ApiResponse<RankingV1Dto.RankingListResponse> getRankings(
            @Parameter(description = "조회 기간 (DAILY, WEEKLY, MONTHLY)", required = true)
            String period,
            @Parameter(description = "기준 날짜 (yyyy-MM-dd)", required = true)
            LocalDate date,
            @Parameter(description = "조회 개수 (기본 100, 최대 100)")
            int size
    );
}
