package com.loopers.domain.ranking;

import java.util.List;

public interface RankingSnapshotRepository {

    List<ProductScore> getAllScores(String dateKey);

    record ProductScore(Long productId, double score) {}
}
