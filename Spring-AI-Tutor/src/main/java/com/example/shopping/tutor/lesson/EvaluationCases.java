package com.example.shopping.tutor.lesson;

import java.util.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.stereotype.Component;
import com.example.shopping.tutor.support.*;

@Component
public class EvaluationCases {
    @TutorCase(id="L11-01",title="업무 사실 검증",sample="P004와 P005의 가격과 배송비 확인",goal="모델 판정 전에 결정적인 상품·금액 검증을 수행합니다.")
    public Outcome checkBusinessFacts(RunContext c) {
        var results=List.of("P004","P005").stream().map(id -> Map.of("product",Shop.product(id),"quote",Shop.quote(c.fixture,List.of(id)))).toList();
        return Outcome.ok("상품 ID·재고·배송그룹·금액은 Java가 검증합니다.",Map.of("verified",results));
    }
    @TutorCase(id="L11-02",title="RelevancyEvaluator",sample="미개봉 반품은 7일 이내인가요?",goal="실제 Spring AI Evaluator를 사용하되 사실성 보증으로 오해하지 않습니다.")
    public Outcome judgeRelevance(RunContext c) {
        var evaluator=new RelevancyEvaluator(ChatClient.builder(c.model()));
        var evaluation=evaluator.evaluate(new EvaluationRequest(c.message,List.of(Knowledge.policies().getFirst()),"미개봉 상품은 수령 후 7일 이내 반품 가능합니다."));
        return Outcome.ok("관련성 판정은 보조 신호입니다. STUB에서는 판정 경로만 확인합니다.",Map.of("pass",evaluation.isPass(),"feedback",String.valueOf(evaluation.getFeedback())));
    }
    @TutorCase(id="L11-03",title="회귀 질문셋",sample="검색 조건 질문셋 실행",goal="정답 라벨과 조건 추출 결과를 비교하고 실패를 보고합니다.")
    public Outcome runRegressionDataset(RunContext c) {
        List<Map<String,Object>> results=new ArrayList<>();
        for(var entry:Map.of("3만원 이하 생활용품",new Shop.Criteria(null,"생활",30000L),"15000원 이하 음료",new Shop.Criteria(null,"음료",15000L)).entrySet()) {
            var actual=c.client().prompt().system(StructuredOutputCases.CRITERIA).user(entry.getKey()).call().entity(Shop.Criteria.class);
            results.add(Map.of("question",entry.getKey(),"expected",entry.getValue(),"actual",Objects.requireNonNull(actual),"pass",entry.getValue().equals(actual)));
        }
        return Outcome.ok("질문별 성공과 실패를 모두 표시합니다.",Map.of("results",results,"successes",results.stream().filter(r -> Boolean.TRUE.equals(r.get("pass"))).count()));
    }
}
