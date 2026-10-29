package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l14multimodal.MultimodalLesson;
import com.example.shopping.tutor.platform.config.MediaModelConfiguration;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.springframework.stereotype.Component;

import java.util.*;

/** 웹 caseId를 학습 메소드에 연결한다. 학습할 때는 MultimodalLesson부터 읽는다. */
@Component
public class MultimodalCases {
    private final MediaModelConfiguration models;

    public MultimodalCases(MediaModelConfiguration models) {
        this.models = models;
    }

    @TutorCase(
            id = "L14-01",
            title = "상품 이미지로 질문",
            sample = "이 컵의 특징과 비슷한 상품을 알려줘",
            goal = "이미지 특징을 추출한 뒤 실제 카탈로그 결과와 구분합니다.",
            requires = "live-image-input",
            lesson = MultimodalLesson.class,
            learningMethod = "searchFromProductImage")
    public Outcome searchFromProductImage(LessonExecution execution) {
        var result =
                new MultimodalLesson()
                        .searchFromProductImage(
                                execution.client(),
                                execution.model(),
                                execution.userInput(),
                                execution.settings.mode(),
                                execution.inputFile,
                                execution.inputMime);
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L14-02",
            title = "음성 쇼핑 질문 전사",
            sample = "녹음 파일을 질문으로 변환해줘",
            goal = "전사 결과는 수정 가능한 텍스트입니다. 자동으로 도구나 변경을 실행하지 않습니다.",
            requires = "live-audio-input",
            lesson = MultimodalLesson.class,
            learningMethod = "transcribeShoppingRequest")
    public Outcome transcribeShoppingRequest(LessonExecution execution) {
        var result =
                new MultimodalLesson()
                        .transcribeShoppingRequest(
                                models.transcription,
                                execution.settings.mode(),
                                execution.inputFile,
                                execution.inputMime);
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L14-03",
            title = "추천 설명 음성 합성",
            sample = "허브티는 9,900원입니다. 배송 조건을 함께 확인하세요.",
            goal = "Text-to-Speech API가 반환한 오디오를 브라우저에서 재생합니다.",
            requires = "live",
            lesson = MultimodalLesson.class,
            learningMethod = "speakRecommendation")
    public Outcome speakRecommendation(LessonExecution execution) {
        var result =
                new MultimodalLesson()
                        .speakRecommendation(
                                models.speech, execution.userInput(), execution.settings.mode());
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L14-04",
            title = "가상 선물 포장 이미지",
            sample = "녹색 리본으로 포장한 가상 선물 상자 이미지를 만들어줘",
            goal = "ImageModel을 호출합니다. 실제 상품 사진이나 판매 보장이 아닙니다.",
            requires = "live",
            lesson = MultimodalLesson.class,
            learningMethod = "generateGiftPreview")
    public Outcome generateGiftPreview(LessonExecution execution) {
        var result =
                new MultimodalLesson()
                        .generateGiftPreview(
                                models.image, execution.userInput(), execution.settings.mode());
        return new Outcome(result.status(), result.answer(), result.data());
    }
}
