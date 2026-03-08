package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class LikeFacade {

    private final LikeService likeService;
    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;

    @Transactional
    public LikeInfo like(Long userId, Long productId) {
        Product product = getProductWithLock(productId);
        boolean alreadyLiked = likeRepository.findByUserIdAndProductId(userId, productId).isPresent();

        Like like = likeService.like(userId, productId);

        if (!alreadyLiked) {
            product.increaseLikeCount();
        }

        return LikeInfo.from(like);
    }

    @Transactional
    public void unlike(Long userId, Long productId) {
        Product product = getProductWithLock(productId);
        boolean liked = likeRepository.findByUserIdAndProductId(userId, productId).isPresent();

        likeService.unlike(userId, productId);

        if (liked) {
            product.decreaseLikeCount();
        }
    }

    @Transactional(readOnly = true)
    public List<LikeInfo> getLikesByUserId(Long userId) {
        return likeService.findByUserId(userId).stream()
            .map(LikeInfo::from)
            .toList();
    }

    private Product getProductWithLock(Long productId) {
        return productRepository.findByIdWithLock(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다"));
    }
}
