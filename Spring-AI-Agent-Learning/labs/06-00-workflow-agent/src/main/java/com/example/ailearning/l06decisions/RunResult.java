package com.example.ailearning.l06decisions;

import java.util.List;

public record RunResult(String mode, String status, String answer, List<ProductPriceDetails> offers) {}
