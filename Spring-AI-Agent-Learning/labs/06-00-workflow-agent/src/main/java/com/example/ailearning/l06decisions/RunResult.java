package com.example.ailearning.l06decisions;

import com.example.ailearning.l06decisions.domain.ProductPriceDetails;

import java.util.List;

public record RunResult(ExecutionMode mode, String status, String answer, List<ProductPriceDetails> offers) {}
