package com.loopers.application.order;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class FakeProductRepository implements ProductRepository {

    private final Map<Long, Product> store = new HashMap<>();
    private long sequence = 1L;

    public Product addProduct(Long brandId, String name, Long price, String description, Integer stock) {
        Product product = new Product(brandId, name, price, description, stock);
        store.put(sequence++, product);
        return product;
    }

    public void addProductWithId(Long id, Product product) {
        store.put(id, product);
    }

    @Override
    public Product save(Product product) {
        return product;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Product> findByIdWithLock(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return store.containsKey(id);
    }

    @Override
    public Page<Product> findAll(Pageable pageable) {
        List<Product> all = List.copyOf(store.values());
        return new PageImpl<>(all, pageable, all.size());
    }

    @Override
    public Page<Product> findByBrandId(Long brandId, Pageable pageable) {
        List<Product> filtered = store.values().stream()
            .filter(p -> p.getBrandId().equals(brandId))
            .toList();
        return new PageImpl<>(filtered, pageable, filtered.size());
    }
}
