package com.loopers.batch.job.rankaggregate.step;

import com.loopers.domain.productrank.MonthlyProductRank;
import com.loopers.domain.productrank.MonthlyProductRankRepository;
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
public class MonthlyRankWriter implements ItemWriter<ProductScoreAggregation> {

    private final MonthlyProductRankRepository monthlyProductRankRepository;
    private final LocalDate baseDate;
    private final AtomicBoolean deleted = new AtomicBoolean(false);

    @Override
    public void write(Chunk<? extends ProductScoreAggregation> chunk) {
        if (deleted.compareAndSet(false, true)) {
            monthlyProductRankRepository.deleteByBaseDate(baseDate);
            log.info("기존 monthly 랭킹 삭제 완료 - baseDate: {}", baseDate);
        }

        List<MonthlyProductRank> ranks = new ArrayList<>();

        for (int i = 0; i < chunk.size(); i++) {
            ProductScoreAggregation agg = chunk.getItems().get(i);
            ranks.add(new MonthlyProductRank(
                    agg.productId(),
                    agg.totalScore(),
                    i + 1,
                    baseDate
            ));
        }

        monthlyProductRankRepository.saveAll(ranks);
        log.info("monthly 랭킹 적재 - {} 건 (ranking 1 ~ {})",
                ranks.size(), ranks.size());
    }
}
