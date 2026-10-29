package com.example.shopping.tutor.lesson;

import java.util.*;
import org.springframework.stereotype.Component;
import com.example.shopping.tutor.support.*;

@Component
public class ShoppingAssistantCases {
    public Shop.Criteria update(RunContext c) {
        c.criteria=MemoryCases.merge(c.criteria,new StructuredOutputCases().extract(c),c.message);return c.criteria;
    }
    private String explain(RunContext c,Object facts) {
        return c.client().prompt().system(RunContext.SYSTEM+" 다음 서버 사실만 설명하세요. 다른 상품이나 가격을 추가하지 마세요.\n"+facts)
            .user(c.userInput()).call().content();
    }
    @TutorCase(id="L13-01",title="후속 조건 상품 추천",sample="3만원 이하 선물 찾아줘",goal="조건 추출 → Java 병합 → 상품 검색 → 사실 기반 설명을 조합합니다.")
    public Outcome recommendWithFollowUp(RunContext c) {
        var criteria=update(c);var products=Shop.search(criteria);
        return new Outcome(products.isEmpty()?"NO_RESULTS":"COMPLETED",products.isEmpty()?"조건에 맞는 상품이 없습니다.":explain(c,products),Map.of("criteria",criteria,"products",products));
    }
    @TutorCase(id="L13-02",title="무료배송 추천 통합",sample="무료배송 채울 상품 추천해줘",goal="현재 견적·부족액·사용자 예산·후보별 견적을 Java로 검증합니다.")
    public Outcome recommendFreeShipping(RunContext c) {
        var criteria=update(c);long subtotal=Shop.subtotal(c.fixture);long missing=Math.max(0,50000-subtotal);
        if(missing==0) return Outcome.ok("이미 무료배송 조건입니다.",Map.of("subtotal",subtotal,"products",List.of()));
        long max=criteria.maxPrice()==null?missing+7000:criteria.maxPrice();
        var candidates=Shop.search(new Shop.Criteria(criteria.keyword(),criteria.category(),max)).stream()
            .filter(p -> p.price()>=missing && p.group().equals("MALL"))
            .filter(p -> !c.fixture.equals("standard") || !Set.of("P001","P002").contains(p.id())).toList();
        var quotes=candidates.stream().map(p -> Map.of("product",p,"quote",Shop.quote(c.fixture,List.of(p.id())))).toList();
        Map<String,Object> facts=Map.of("currentSubtotal",subtotal,"currentShippingFee",Shop.shipping(subtotal),"missing",missing,"threshold",50000,"hypotheticalAlternatives",quotes);
        String answer="조건에 맞는 무료배송 후보가 없습니다.";
        if(!candidates.isEmpty()) {
            String guide=c.client().prompt().system(RunContext.SYSTEM+"\n현재 장바구니는 무료배송 기준에 미달합니다. 아래 견적은 후보 하나를 추가했을 때의 가정입니다. 아직 실제 추가하지 않았습니다. 상품의 선택 기준만 간단히 설명하세요. 현재 무료배송이라고 말하거나 가격·배송비를 다시 계산하지 마세요.\n"+facts)
                .user(c.userInput()).call().content();
            answer=String.format(java.util.Locale.KOREA,"현재 장바구니 %,d원 · 배송비 %,d원 · 무료배송까지 %,d원 부족합니다.\n아래 후보 중 하나를 1개 추가하면 무료배송 조건을 충족합니다. 실제 장바구니는 변경하지 않았습니다.\n\n%s",subtotal,Shop.shipping(subtotal),missing,guide);
        }
        return new Outcome(candidates.isEmpty()?"NO_RESULTS":"COMPLETED",answer,
            Map.of("criteria",criteria,"missing",missing,"maxPrice",max,"products",candidates,"quotes",quotes,"currentSubtotal",subtotal,"currentShippingFee",Shop.shipping(subtotal)));
    }
    @TutorCase(id="L13-03",title="상품 도구와 정책 RAG",sample="텀블러 추천하고 반품 조건도 알려줘",goal="상품 사실과 문서 근거를 각각 조회해 조합합니다.")
    public Outcome combinePolicyAndCatalog(RunContext c) {
        var products=Shop.search(new Shop.Criteria("텀블러",null,null));
        var policy=new RagCases().answer(c,c.message);
        return new Outcome(policy.status(),policy.answer(),Map.of("products",products,"policy",policy.data()));
    }
    @TutorCase(id="L13-04",title="실패 후 복구·초기화",sample="3만원 이하 음료 찾아줘",goal="실패 턴은 상태를 확정하지 않습니다. 실패 실험 후 이전 조건을 확인하세요.")
    public Outcome recoverAndReset(RunContext c) {
        Outcome result=recommendWithFollowUp(c);
        if(c.experiment.equals("timeout")) throw new IllegalStateException("학습용 실패: 변경한 조건을 commit하지 않습니다.");
        return result;
    }
}
