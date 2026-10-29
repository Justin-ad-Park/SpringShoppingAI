package com.example.shopping.tutor.lesson;

import java.util.*;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.memory.repository.jdbc.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import com.example.shopping.tutor.support.*;

@Component
public class MemoryCases {
    public String chat(RunContext c) {
        return c.client().prompt().advisors(MessageChatMemoryAdvisor.builder(c.memory).build())
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID,c.conversationId)).user(c.userInput()).call().content();
    }
    @TutorCase(id="L04-01",title="대화 기억 비교",sample="예산 3만원이야",goal="다음 턴에서 이전 메시지가 모델에 전달되는지 관찰합니다.")
    public Outcome compareNoMemoryAndMemory(RunContext c) {
        String without=c.client().prompt().user(c.userInput()).call().content();
        return Outcome.ok(chat(c),Map.of("withoutMemory",without));
    }
    @TutorCase(id="L04-02",title="대화 격리와 초기화",sample="나는 음료를 찾고 있어",goal="새 대화·다른 탭의 메모리를 분리합니다.")
    public Outcome isolateAndReset(RunContext c) { return Outcome.text(chat(c)); }
    @TutorCase(id="L04-03",title="메시지 window",sample="최근 메시지만 기억해줘",goal="메시지 개수 제한과 토큰 제한의 차이를 확인합니다.")
    public Outcome windowMessages(RunContext c) {
        var window=MessageWindowChatMemory.builder().maxMessages(4).build();
        for(int i=0;i<8;i++) window.add("demo",i%2==0?new UserMessage("질문 "+i):new AssistantMessage("답변 "+i));
        return Outcome.ok("최근 4개 메시지를 유지합니다. 토큰 예산 제한은 별도입니다.",Map.of("window",RunContext.messages(window.get("demo"))));
    }
    @TutorCase(id="L04-04",title="JDBC memory 저장소",sample="저장소를 다시 만들어도 대화가 남을까?",goal="H2에 저장하고 새 JdbcChatMemoryRepository로 복구합니다.")
    public Outcome persistWithJdbc(RunContext c) {
        String db="jdbc:h2:mem:tutor_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1";
        var ds=new DriverManagerDataSource(db,"sa","");
        try {
            new ResourceDatabasePopulator(new ClassPathResource("org/springframework/ai/chat/memory/repository/jdbc/schema-h2.sql")).execute(ds);
            var first=JdbcChatMemoryRepository.builder().dataSource(ds).dialect(new H2ChatMemoryRepositoryDialect()).build();
            first.saveAll("demo",List.of(new UserMessage(c.message)));
            var second=JdbcChatMemoryRepository.builder().dataSource(ds).dialect(new H2ChatMemoryRepositoryDialect()).build();
            return Outcome.ok("repository 인스턴스를 다시 만들어 저장된 메시지를 조회했습니다.",Map.of("restored",RunContext.messages(second.findByConversationId("demo"))));
        } finally { new org.springframework.jdbc.core.JdbcTemplate(ds).execute("SHUTDOWN"); }
    }
    @TutorCase(id="L04-05",title="대화 원문과 검색 상태",sample="3만원 이하 음료 찾아줘",goal="원문 기억과 검증된 업무 조건을 구분합니다. 다음 턴에 예산 해제를 입력해 보세요.")
    public Outcome compareTaskState(RunContext c) {
        Shop.Criteria proposed=new StructuredOutputCases().extract(c);
        c.criteria=merge(c.criteria,proposed,c.message);
        return Outcome.ok(chat(c),Map.of("criteria",c.criteria));
    }
    public static Shop.Criteria merge(Shop.Criteria previous,Shop.Criteria proposed,String message) {
        return new Shop.Criteria(proposed.keyword()!=null?proposed.keyword():previous.keyword(),
            message.contains("카테고리 해제")?null:proposed.category()!=null?proposed.category():previous.category(),
            message.contains("예산 해제")?null:proposed.maxPrice()!=null?proposed.maxPrice():previous.maxPrice());
    }
}
