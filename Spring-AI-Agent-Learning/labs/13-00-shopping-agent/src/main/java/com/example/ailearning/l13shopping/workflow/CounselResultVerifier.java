package com.example.ailearning.l13shopping.workflow;

import com.example.ailearning.l13shopping.domain.Intent;
import com.example.ailearning.l13shopping.domain.Product;
import com.example.ailearning.l13shopping.domain.ProductPriceDetails;
import com.example.ailearning.l13shopping.policy.PolicySearchResult;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 모델이 고른 ID를 이번 요청에서 실제 조회한 사실 및 업무 조건과 대조한다. */
@Component
public class CounselResultVerifier {
    public CounselResult verifySelection(Intent requestIntent,
            CandidateEvidenceSelection candidateSelection, CounselToolResults toolResults) {
        if (hasInvalidSelection(candidateSelection)) {
            return verificationFailed(requestIntent);
        }

        boolean shoppingRequest = requestIntent.task().equals("SHOPPING");
        List<ProductPriceDetails> verifiedOffers = new ArrayList<>();
        for (String productId : candidateSelection.productIds().stream().distinct().toList()) {
            ProductPriceDetails priceDetails = toolResults.priceDetailsFor(productId);
            if (!isValidProductSelection(shoppingRequest, requestIntent, priceDetails)) {
                return verificationFailed(requestIntent);
            }
            verifiedOffers.add(priceDetails);
        }

        List<PolicySearchResult> verifiedSources = new ArrayList<>();
        for (String sourceId : candidateSelection.sourceIds().stream().distinct().toList()) {
            PolicySearchResult policyResult = toolResults.policyResultFor(sourceId);
            if (policyResult == null) {
                return verificationFailed(requestIntent);
            }
            verifiedSources.add(policyResult);
        }

        if (shoppingRequest && verifiedOffers.isEmpty()) {
            String resultStatus = hasNoAffordableProduct(requestIntent, toolResults)
                    ? "NO_RESULTS"
                    : "NO_VERIFIED_OFFER";
            return new CounselResult(resultStatus, "현재 조건에서 확인된 추천 상품이 없습니다.",
                    requestIntent, List.of(), List.of());
        }

        boolean policyRequest = requestIntent.task().equals("POLICY");
        if (policyRequest && verifiedSources.isEmpty()) {
            String resultStatus = toolResults.policySearchPerformed() && !toolResults.hasPolicySearchResults()
                    ? "NO_CONTEXT"
                    : "NO_VERIFIED_SOURCE";
            return new CounselResult(resultStatus, "확인할 수 있는 정책 근거가 없습니다.",
                    requestIntent, List.of(), List.of());
        }

        String verifiedAnswer = createVerifiedAnswer(verifiedOffers, verifiedSources);
        return new CounselResult("VERIFIED", verifiedAnswer, requestIntent,
                List.copyOf(verifiedOffers), List.copyOf(verifiedSources));
    }

    private boolean hasInvalidSelection(CandidateEvidenceSelection candidateSelection) {
        return candidateSelection == null
                || candidateSelection.productIds() == null
                || candidateSelection.sourceIds() == null;
    }

    private boolean isValidProductSelection(boolean shoppingRequest, Intent requestIntent,
            ProductPriceDetails priceDetails) {
        if (!shoppingRequest || priceDetails == null) {
            return false;
        }
        boolean withinBudget = priceDetails.total() <= requestIntent.totalBudget();
        boolean categoryMatches = requestIntent.category() == null
                || priceDetails.product().category().equals(requestIntent.category());
        return withinBudget && categoryMatches;
    }

    private boolean hasNoAffordableProduct(Intent requestIntent, CounselToolResults toolResults) {
        if (!toolResults.productSearchPerformed()) {
            return false;
        }
        return toolResults.lastProductSearchResults().stream()
                .allMatch(product -> isVerifiedAndOverBudget(product, requestIntent, toolResults));
    }

    private boolean isVerifiedAndOverBudget(Product product, Intent requestIntent,
            CounselToolResults toolResults) {
        ProductPriceDetails priceDetails = toolResults.priceDetailsFor(product.id());
        return priceDetails != null && priceDetails.total() > requestIntent.totalBudget();
    }

    private String createVerifiedAnswer(List<ProductPriceDetails> verifiedOffers,
            List<PolicySearchResult> verifiedSources) {
        String productAnswer = verifiedOffers.stream()
                .map(offer -> offer.product().name() + " 총 " + offer.total() + "원")
                .collect(Collectors.joining(" / "));
        if (verifiedSources.isEmpty()) {
            return productAnswer.strip();
        }

        String policyEvidence = verifiedSources.stream()
                .map(PolicySearchResult::text)
                .collect(Collectors.joining(" / "));
        return (productAnswer + "\n정책 근거: " + policyEvidence).strip();
    }

    private CounselResult verificationFailed(Intent requestIntent) {
        return new CounselResult("VERIFY_FAILED", "조회한 사실과 선택 결과가 맞지 않아 답변을 보류합니다.",
                requestIntent, List.of(), List.of());
    }
}
