package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingRepository;

public record RankingInfo(Long productId, double score, int ranking) {

    public static RankingInfo from(RankingRepository.RankingEntry entry) {
        return new RankingInfo(entry.productId(), entry.score(), entry.ranking());
    }
}
