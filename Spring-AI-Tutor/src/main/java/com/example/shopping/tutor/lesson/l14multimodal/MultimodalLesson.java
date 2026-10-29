package com.example.shopping.tutor.lesson.l14multimodal;

import com.example.shopping.tutor.lesson.l02structured.SearchCriteria;
import com.example.shopping.tutor.lesson.l02structured.StructuredOutputLesson;
import com.example.shopping.tutor.lesson.l06tools.support.DemoProductCatalog;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.*;
import org.springframework.util.MimeTypeUtils;

import java.util.*;

/** 14 단원: 핵심 API를 직접 호출한다. 웹 실행·세션 확정은 platform에서 담당한다. */
public class MultimodalLesson {

    /** 이미지 특징을 추출한 뒤 실제 카탈로그 결과와 구분합니다. */
    public Result searchFromProductImage(
            ChatClient chatClient,
            ChatModel chatModel,
            String question,
            String mode,
            org.springframework.core.io.Resource inputFile,
            String inputMime) {
        if (inputFile == null || inputMime == null || !inputMime.startsWith("image/"))
            throw new IllegalArgumentException("이미지 파일이 필요합니다.");
        if (mode.equals("stub"))
            return Result.unsupported("파일 검증은 통과했습니다. 실제 이미지 이해는 LIVE의 vision 지원 채팅 모델이 필요합니다.");
        var media =
                Media.builder()
                        .mimeType(MimeTypeUtils.parseMimeType(inputMime))
                        .data(inputFile)
                        .build();
        var user =
                UserMessage.builder()
                        .text("가격·재고를 추측하지 말고 이미지의 외형과 용도만 설명하세요. " + question)
                        .media(media)
                        .build();
        String features = chatModel.call(new Prompt(user)).getResult().getOutput().getText();
        var criteria =
                chatClient
                        .prompt()
                        .system(StructuredOutputLesson.CRITERIA)
                        .user(features)
                        .call()
                        .entity(SearchCriteria.class);
        return Result.ok(
                features,
                Map.of(
                        "products",
                        DemoProductCatalog.search(Objects.requireNonNull(criteria)),
                        "notice",
                        "가격과 재고는 카탈로그 기준입니다."));
    }

    /** 전사 결과는 수정 가능한 텍스트입니다. 자동으로 도구나 변경을 실행하지 않습니다. */
    public Result transcribeShoppingRequest(
            OpenAiAudioTranscriptionModel transcriptionModel,
            String mode,
            org.springframework.core.io.Resource inputFile,
            String inputMime) {
        if (inputFile == null || inputMime == null || !inputMime.startsWith("audio/"))
            throw new IllegalArgumentException("음성 파일이 필요합니다.");
        if (mode.equals("stub")) return Result.unsupported("파일 검증은 통과했습니다. 실제 전사는 LIVE에서 실행하세요.");
        String transcript = transcriptionModel.call(inputFile);
        return Result.ok(transcript, Map.of("transcript", transcript, "automaticExecution", false));
    }

    /** Text-to-Speech API가 반환한 오디오를 브라우저에서 재생합니다. */
    public Result speakRecommendation(
            OpenAiAudioSpeechModel speechModel, String question, String mode) {
        if (mode.equals("stub")) return Result.unsupported("실제 음성 생성은 LIVE에서 실행하세요.");
        byte[] bytes = speechModel.call(question);
        if (bytes == null || bytes.length == 0) throw new IllegalStateException("빈 오디오");
        return Result.ok(
                "AI 생성 음성입니다.",
                Map.of(
                        "audioBase64",
                        Base64.getEncoder().encodeToString(bytes),
                        "contentType",
                        "audio/mpeg"));
    }

    /** ImageModel을 호출합니다. 실제 상품 사진이나 판매 보장이 아닙니다. */
    public Result generateGiftPreview(OpenAiImageModel imageModel, String question, String mode) {
        if (mode.equals("stub"))
            return Result.unsupported("실제 이미지 생성은 LIVE에서 실행하세요. 별도 이미지 모델 비용이 발생합니다.");
        var output =
                imageModel
                        .call(
                                new ImagePrompt(
                                        "학습용 가상 선물 포장 시안. 실제 상품 사진으로 오인되지 않게 표현하세요. " + question))
                        .getResult()
                        .getOutput();
        if (output.getB64Json() != null)
            return Result.ok("AI 생성 이미지 · 가상 포장 시안", Map.of("imageBase64", output.getB64Json()));
        if (output.getUrl() != null)
            return Result.ok("AI 생성 이미지 · 가상 포장 시안", Map.of("imageUrl", output.getUrl()));
        throw new IllegalStateException("이미지 결과가 없습니다.");
    }

    public record Result(String status, String answer, Map<String, Object> data) {
        public static Result ok(String answer, Map<String, Object> data) {
            return new Result("COMPLETED", answer, data);
        }

        public static Result text(String answer) {
            if (answer == null || answer.isBlank())
                throw new IllegalStateException("모델이 빈 응답을 반환했습니다.");
            return ok(answer, Map.of());
        }

        public static Result unsupported(String reason) {
            return new Result("UNSUPPORTED", reason, Map.of());
        }
    }
}
