package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingInfo;

import java.util.List;

public class RankingV1Dto {

    public record RankingResponse(int ranking, Long productId, double score) {
        public static RankingResponse from(RankingInfo info) {
            return new RankingResponse(info.ranking(), info.productId(), info.score());
        }
    }

    public record RankingListResponse(List<RankingResponse> rankings) {
        public static RankingListResponse from(List<RankingInfo> infos) {
            List<RankingResponse> rankings = infos.stream()
                    .map(RankingResponse::from)
                    .toList();
            return new RankingListResponse(rankings);
        }
    }
}
