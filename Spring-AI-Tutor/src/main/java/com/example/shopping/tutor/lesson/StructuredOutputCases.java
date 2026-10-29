package com.example.shopping.tutor.lesson;

import java.util.*;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import com.example.shopping.tutor.support.*;

@Component
public class StructuredOutputCases {
    public static final String CRITERIA="""
        TASK:CRITERIA
        쇼핑 질문에서 사용자가 명시한 검색 조건만 추출하세요. 누락된 조건을 추측하거나 채우지 마세요.
        keyword: 구체적 상품명만. '무료배송', '채울', '선물', '상품', '추천'은 목적이므로 keyword가 아닙니다.
        category: 식품/음료/생활 중 사용자가 명시한 분류만. 상품명이나 목적에서 분류를 추론하지 마세요.
        maxPrice: 사용자가 말한 상품 예산의 원 단위 상한. 명시하지 않거나 해제하면 null입니다.
        조건을 말하지 않은 필드는 반드시 null입니다. 기본값/이전 조건은 Java가 병합합니다.
        예시:
        '무료배송 채울 상품 추천해줘' => {"keyword":null,"category":null,"maxPrice":null}
        '3만원 이하 선물 찾아줘' => {"keyword":null,"category":null,"maxPrice":30000}
        '음료로만' => {"keyword":null,"category":"음료","maxPrice":null}
        '3만원 이하 생활용품' => {"keyword":null,"category":"생활","maxPrice":30000}
        '텀블러 찾아줘' => {"keyword":"텀블러","category":null,"maxPrice":null}
        '예산 해제' => {"keyword":null,"category":null,"maxPrice":null}
        """;

    /** entity()는 JSON 파싱을 담당한다. record의 생성자가 업무 유효성을 검증한다. */
    public Shop.Criteria extract(RunContext c) {
        Shop.Criteria criteria=c.client().prompt().system(CRITERIA).user(c.userInput()).call().entity(Shop.Criteria.class);
        if(criteria==null) throw new IllegalStateException("구조화 결과가 없습니다.");
        return criteria;
    }
    @TutorCase(id="L02-01",title="자연어를 record로",sample="3만원 이하 생활용품 찾아줘",goal="entity() 변환과 음수 예산·카테고리 검증을 구분합니다.")
    public Outcome extractCriteria(RunContext c) {
        var criteria=extract(c);
        return Outcome.ok("검색 조건을 Java record로 변환했습니다.",Map.of("criteria",criteria));
    }
    @TutorCase(id="L02-02",title="제네릭 추천 목록",sample="P004와 P005를 비교해줘",goal="List 매핑 후 후보 ID와 중복을 검증합니다.")
    public Outcome mapRecommendationList(RunContext c) {
        List<Shop.Recommendation> items=c.client().prompt().system("TASK:RECOMMENDATIONS\n후보는 P004 허브티, P005 커피입니다. 후보 안에서만 productId와 reason을 반환하세요.")
            .user(c.userInput()).call().entity(new ParameterizedTypeReference<List<Shop.Recommendation>>() {});
        validateRecommendations(items);
        return Outcome.ok("후보 ID를 검증했습니다.",Map.of("recommendations",items,"products",items.stream().map(i -> Shop.product(i.productId())).toList()));
    }
    public static void validateRecommendations(List<Shop.Recommendation> items) {
        if(items==null || items.isEmpty()) throw new IllegalArgumentException("추천이 비었습니다.");
        Set<String> seen=new HashSet<>();
        for(var item:items) if(!Set.of("P004","P005").contains(item.productId()) || !seen.add(item.productId()) || item.reason()==null || item.reason().isBlank())
            throw new IllegalArgumentException("잘못되거나 중복된 후보입니다.");
    }
    @TutorCase(id="L02-03",title="잘못된 JSON과 교정",sample="3만원 이하 음료 찾아줘",goal="변환 실패를 한 번만 교정하며 파싱과 업무 검증 오류를 구분합니다.")
    public Outcome handleInvalidOutput(RunContext c) {
        var converter=new BeanOutputConverter<>(Shop.Criteria.class);
        String first=c.experiment.equals("invalid-json")?"{broken":c.client().prompt().system(CRITERIA).user(c.message+"\n"+converter.getFormat()).call().content();
        try { return Outcome.ok("첫 응답 변환 성공",Map.of("criteria",Objects.requireNonNull(converter.convert(first)),"corrected",false)); }
        catch(RuntimeException e) {
            var corrected=extract(c);
            return Outcome.ok("변환 오류 후 1회 재요청했습니다.",Map.of("criteria",corrected,"corrected",true));
        }
    }
    @TutorCase(id="L02-04",title="Native JSON Schema",sample="3만원 이하 생활용품 찾아줘",goal="일반 변환과 provider의 JSON Schema 옵션을 비교합니다.",requires="native-schema")
    public Outcome compareNativeSchema(RunContext c) {
        if(c.settings.mode().equals("stub") || !c.settings.nativeSchema()) return Outcome.unsupported("LIVE + TUTOR_NATIVE_SCHEMA=true 및 JSON Schema 지원 모델이 필요합니다.");
        var converter=new BeanOutputConverter<>(Shop.Criteria.class);
        var options=OpenAiChatOptions.builder().responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_SCHEMA)
            .jsonSchema(converter.getJsonSchema()).build()).build();
        Shop.Criteria criteria=c.client().prompt().system(CRITERIA).options(options).user(c.message).call().entity(Shop.Criteria.class);
        return Outcome.ok("Native schema 결과도 업무 검증을 거칩니다.",Map.of("criteria",Objects.requireNonNull(criteria),"schema",converter.getJsonSchema()));
    }
}
