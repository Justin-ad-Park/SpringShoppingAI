package com.example.ailearning.l07runtime.domain;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProductCatalog {
    public record Product(String name, int price) {}

    public List<Product> findAll() {
        return List.of(new Product("허브티", 12000), new Product("커피", 28000), new Product("텀블러", 35000));
    }
}
