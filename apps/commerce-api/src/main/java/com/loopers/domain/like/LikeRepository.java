package com.loopers.domain.like;

import java.util.List;
import java.util.Optional;

public interface LikeRepository {

    Like save(Like like);

    Optional<Like> findByUserIdAndProductId(Long userId, Long productId);

    List<Like> findByUserId(Long userId);

    void delete(Like like);

    long countByProductId(Long productId);
}