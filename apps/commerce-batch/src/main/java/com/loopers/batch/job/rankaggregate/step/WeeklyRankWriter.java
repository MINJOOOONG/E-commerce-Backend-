package com.loopers.batch.job.rankaggregate.step;

import com.loopers.domain.productrank.WeeklyProductRank;
import com.loopers.domain.productrank.WeeklyProductRankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
public class WeeklyRankWriter implements ItemWriter<ProductScoreAggregation> {

    private final WeeklyProductRankRepository weeklyProductRankRepository;
    private final LocalDate baseDate;
    private final AtomicBoolean deleted = new AtomicBoolean(false);

    @Override
    public void write(Chunk<? extends ProductScoreAggregation> chunk) {
        if (deleted.compareAndSet(false, true)) {
            weeklyProductRankRepository.deleteByBaseDate(baseDate);
            log.info("기존 weekly 랭킹 삭제 완료 - baseDate: {}", baseDate);
        }

        List<WeeklyProductRank> ranks = new ArrayList<>();
        int rankOffset = getRankOffset(chunk);

        for (int i = 0; i < chunk.size(); i++) {
            ProductScoreAggregation agg = chunk.getItems().get(i);
            ranks.add(new WeeklyProductRank(
                    agg.productId(),
                    agg.totalScore(),
                    rankOffset + i + 1,
                    baseDate
            ));
        }

        weeklyProductRankRepository.saveAll(ranks);
        log.info("weekly 랭킹 적재 - {} 건 (ranking {} ~ {})",
                ranks.size(), rankOffset + 1, rankOffset + ranks.size());
    }

    private int getRankOffset(Chunk<? extends ProductScoreAggregation> chunk) {
        // TOP 100이므로 chunkSize >= 100이면 항상 첫 번째 chunk에서 모두 처리됨
        // 만약 chunkSize가 100보다 작은 경우를 대비한 안전 장치
        return 0;
    }
}
