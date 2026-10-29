package com.example.shopping.tutor.lesson;

import java.util.*;
import org.springframework.ai.content.Media;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import com.example.shopping.tutor.support.*;

@Component
public class MultimodalCases {
    private final MediaModels models;
    public MultimodalCases(MediaModels models) { this.models=models; }
    @TutorCase(id="L14-01",title="상품 이미지로 질문",sample="이 컵의 특징과 비슷한 상품을 알려줘",goal="이미지 특징을 추출한 뒤 실제 카탈로그 결과와 구분합니다.",requires="live-image-input")
    public Outcome searchFromProductImage(RunContext c) {
        if(c.inputFile==null || c.inputMime==null || !c.inputMime.startsWith("image/")) throw new IllegalArgumentException("이미지 파일이 필요합니다.");
        if(c.settings.mode().equals("stub")) return Outcome.unsupported("파일 검증은 통과했습니다. 실제 이미지 이해는 LIVE의 vision 지원 채팅 모델이 필요합니다.");
        var media=Media.builder().mimeType(MimeTypeUtils.parseMimeType(c.inputMime)).data(c.inputFile).build();
        var user=UserMessage.builder().text("가격·재고를 추측하지 말고 이미지의 외형과 용도만 설명하세요. "+c.message).media(media).build();
        String features=c.model().call(new Prompt(user)).getResult().getOutput().getText();
        var criteria=c.client().prompt().system(StructuredOutputCases.CRITERIA).user(features).call().entity(Shop.Criteria.class);
        return Outcome.ok(features,Map.of("products",Shop.search(Objects.requireNonNull(criteria)),"notice","가격과 재고는 카탈로그 기준입니다."));
    }
    @TutorCase(id="L14-02",title="음성 쇼핑 질문 전사",sample="녹음 파일을 질문으로 변환해줘",goal="전사 결과는 수정 가능한 텍스트입니다. 자동으로 도구나 변경을 실행하지 않습니다.",requires="live-audio-input")
    public Outcome transcribeShoppingRequest(RunContext c) {
        if(c.inputFile==null || c.inputMime==null || !c.inputMime.startsWith("audio/")) throw new IllegalArgumentException("음성 파일이 필요합니다.");
        if(c.settings.mode().equals("stub")) return Outcome.unsupported("파일 검증은 통과했습니다. 실제 전사는 LIVE에서 실행하세요.");
        String transcript=models.transcription.call(c.inputFile);
        return Outcome.ok(transcript,Map.of("transcript",transcript,"automaticExecution",false));
    }
    @TutorCase(id="L14-03",title="추천 설명 음성 합성",sample="허브티는 9,900원입니다. 배송 조건을 함께 확인하세요.",goal="Text-to-Speech API가 반환한 오디오를 브라우저에서 재생합니다.",requires="live")
    public Outcome speakRecommendation(RunContext c) {
        if(c.settings.mode().equals("stub")) return Outcome.unsupported("실제 음성 생성은 LIVE에서 실행하세요.");
        byte[] bytes=models.speech.call(c.message);
        if(bytes==null || bytes.length==0) throw new IllegalStateException("빈 오디오");
        return Outcome.ok("AI 생성 음성입니다.",Map.of("audioBase64",Base64.getEncoder().encodeToString(bytes),"contentType","audio/mpeg"));
    }
    @TutorCase(id="L14-04",title="가상 선물 포장 이미지",sample="녹색 리본으로 포장한 가상 선물 상자 이미지를 만들어줘",goal="ImageModel을 호출합니다. 실제 상품 사진이나 판매 보장이 아닙니다.",requires="live")
    public Outcome generateGiftPreview(RunContext c) {
        if(c.settings.mode().equals("stub")) return Outcome.unsupported("실제 이미지 생성은 LIVE에서 실행하세요. 별도 이미지 모델 비용이 발생합니다.");
        var output=models.image.call(new ImagePrompt("학습용 가상 선물 포장 시안. 실제 상품 사진으로 오인되지 않게 표현하세요. "+c.message)).getResult().getOutput();
        if(output.getB64Json()!=null) return Outcome.ok("AI 생성 이미지 · 가상 포장 시안",Map.of("imageBase64",output.getB64Json()));
        if(output.getUrl()!=null) return Outcome.ok("AI 생성 이미지 · 가상 포장 시안",Map.of("imageUrl",output.getUrl()));
        throw new IllegalStateException("이미지 결과가 없습니다.");
    }
}
