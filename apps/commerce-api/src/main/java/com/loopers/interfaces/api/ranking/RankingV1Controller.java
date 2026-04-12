package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingFacade;
import com.loopers.application.ranking.RankingInfo;
import com.loopers.domain.ranking.RankPeriod;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rankings")
public class RankingV1Controller implements RankingV1ApiSpec {

    private static final int DEFAULT_SIZE = 100;
    private static final int MAX_SIZE = 100;

    private final RankingFacade rankingFacade;

    @GetMapping
    @Override
    public ApiResponse<RankingV1Dto.RankingListResponse> getRankings(
            @RequestParam("period") String period,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "size", defaultValue = "100") int size
    ) {
        RankPeriod rankPeriod = parseRankPeriod(period);
        int validSize = Math.min(Math.max(size, 1), MAX_SIZE);

        List<RankingInfo> rankings = rankingFacade.getRanking(rankPeriod, date, validSize);
        RankingV1Dto.RankingListResponse response = RankingV1Dto.RankingListResponse.from(rankings);

        return ApiResponse.success(response);
    }

    private RankPeriod parseRankPeriod(String period) {
        try {
            return RankPeriod.valueOf(period.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "잘못된 period 값입니다: " + period);
        }
    }
}
