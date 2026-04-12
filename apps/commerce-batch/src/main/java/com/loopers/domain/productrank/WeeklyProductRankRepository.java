package com.loopers.domain.productrank;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyProductRankRepository {

    List<WeeklyProductRank> saveAll(List<WeeklyProductRank> ranks);

    void deleteByBaseDate(LocalDate baseDate);
}
