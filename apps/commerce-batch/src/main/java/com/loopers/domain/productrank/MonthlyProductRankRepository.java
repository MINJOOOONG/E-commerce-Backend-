package com.loopers.domain.productrank;

import java.time.LocalDate;
import java.util.List;

public interface MonthlyProductRankRepository {

    List<MonthlyProductRank> saveAll(List<MonthlyProductRank> ranks);

    void deleteByBaseDate(LocalDate baseDate);
}
