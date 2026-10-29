package com.example.shopping.tutor.lesson;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;
import com.example.shopping.tutor.support.*;

@Component
public class OperationsCases {
    @TutorCase(id="L12-01",title="사용량과 지연 관측",sample="텀블러 선택 기준 알려줘",goal="provider usage와 실제 지연을 관찰합니다. 모델 호출은 Micrometer ObservationRegistry에 연결됩니다.")
    public Outcome recordUsageAndLatency(RunContext c) { return Outcome.text(c.client().prompt().user(c.userInput()).call().content()); }
    public static class ReadFailure extends RuntimeException {
        public final int status;
        public ReadFailure(int status) { super("학습용 HTTP "+status);this.status=status; }
    }
    public static <T> T retryRead(Supplier<T> call) {
        for(int attempt=1;;attempt++) {
            try { return call.get(); }
            catch(ReadFailure e) { if(attempt>=2 || !Set.of(429,503,504).contains(e.status)) throw e; }
        }
    }
    @TutorCase(id="L12-02",title="일시적 읽기 실패 재시도",sample="503 다음 성공을 재현해줘",goal="명시적으로 분류한 읽기 요청만 최대 2회 실행합니다. 변경 요청에는 적용하지 않습니다.")
    public Outcome retryTransientRead(RunContext c) {
        AtomicInteger calls=new AtomicInteger();
        String result=retryRead(() -> { if(calls.incrementAndGet()==1) throw new ReadFailure(503);return "읽기 요청 성공"; });
        return Outcome.ok(result,Map.of("attempts",calls.get(),"notice","결정적 장애 실험입니다. 일반 모델 호출의 숨은 자동 재시도는 비활성화했습니다."));
    }
    @TutorCase(id="L12-03",title="모델 계약 비교",sample="선물 고르는 기준 알려줘",goal="같은 ChatClient API에서 STUB과 설정된 모델 응답을 비교합니다.")
    public Outcome switchCompatibleModel(RunContext c) {
        ChatModel alternative=new ScriptedChatModel();
        String baseline=ChatClient.create(alternative).prompt().user(c.message).call().content();
        String configured=c.client().prompt().user(c.message).call().content();
        return Outcome.ok(configured,Map.of("baseline",baseline,"configuredMode",c.settings.mode(),"notice","다른 OpenAI 호환 공급자는 서버의 TUTOR_OPENAI_BASE_URL과 모델 설정으로 실행 환경을 분리해 비교하세요."));
    }
}
