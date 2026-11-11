package com.example.ailearning.l13shopping.workflow;

import com.example.ailearning.l13shopping.domain.Intent;
import com.example.ailearning.l13shopping.domain.ProductPriceDetails;
import com.example.ailearning.l13shopping.policy.PolicySearchResult;
import java.util.List;

/** 검증을 마친 상담 결과 또는 추가 질문·보류 상태를 담는다. */
public record CounselResult(String status, String answer, Intent intent,
                            List<ProductPriceDetails> offers, List<PolicySearchResult> sources) {}
