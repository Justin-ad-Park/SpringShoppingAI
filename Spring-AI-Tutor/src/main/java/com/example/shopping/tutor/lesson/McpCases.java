package com.example.shopping.tutor.lesson;

import java.time.Duration;
import java.util.*;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import com.example.shopping.tutor.support.*;

@Component
public class McpCases {
    @org.springframework.beans.factory.annotation.Value("${spring.ai.mcp.server.enabled:false}") private boolean serverEnabled;
    @org.springframework.beans.factory.annotation.Value("${server.port:8090}") private int serverPort;
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.core.env.Environment environment;
    /** 연결을 선택한 케이스에서만 생성하고 닫는다. 기본 시작은 MCP에 의존하지 않는다. */
    public McpSyncClient connect(String url) {
        var transport=HttpClientStreamableHttpTransport.builder(url).endpoint("/mcp").build();
        var client=McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build();
        try { client.initialize();return client; }
        catch(RuntimeException e) { client.closeGracefully();throw new IllegalStateException("MCP 연결 실패. shopping-mcp 실행과 TUTOR_MCP_URL을 확인하세요.",e); }
    }
    @TutorCase(id="L10-01",title="MCP discovery와 직접 호출",sample="원격 도구 목록과 배송 정책 보기",goal="실제 Streamable HTTP 서버의 schema와 호출 결과를 확인합니다.",requires="mcp")
    public Outcome discoverAndCall(RunContext c) {
        try(var client=connect(c.settings.mcpUrl())) {
            var tools=client.listTools().tools();
            var result=client.callTool(new McpSchema.CallToolRequest("getShippingPolicy",Map.of()));
            if(Boolean.TRUE.equals(result.isError())) throw new IllegalStateException("MCP 도구 오류");
            return Outcome.ok("원격 MCP 도구를 발견하고 배송 정책을 조회했습니다.",Map.of("tools",tools,"result",result));
        }
    }
    @TutorCase(id="L10-02",title="모델의 원격 도구 선택",sample="1만5천원 이하 음료 추천해줘",goal="허용한 MCP callback만 ChatClient에 등록합니다.",requires="mcp")
    public Outcome letModelUseRemoteTool(RunContext c) {
        try(var client=connect(c.settings.mcpUrl())) {
            ToolCallback[] callbacks=Arrays.stream(new SyncMcpToolCallbackProvider(client).getToolCallbacks())
                .filter(t -> t.getToolDefinition().name().endsWith("searchProducts")).toArray(ToolCallback[]::new);
            if(callbacks.length!=1) throw new IllegalStateException("상품 검색 도구를 하나만 허용해야 합니다.");
            return Outcome.text(c.client().prompt().toolCallbacks(c.bounded(callbacks)).user(c.userInput()).call().content());
        }
    }
    @TutorCase(id="L10-03",title="로컬·원격 배송 견적",sample="무료배송 견적 비교",goal="같은 fixture의 사실과 transport 오류를 구분합니다.",requires="mcp")
    public Outcome compareLocalAndRemote(RunContext c) {
        try(var client=connect(c.settings.mcpUrl())) {
            var result=client.callTool(new McpSchema.CallToolRequest("quoteCart",Map.of("scenario",c.fixture,"additionalProductIds",List.of("P004"))));
            if(Boolean.TRUE.equals(result.isError())) throw new IllegalStateException("원격 견적 실패");
            return Outcome.ok("로컬 계산과 원격 응답을 비교하세요.",Map.of("local",Shop.quote(c.fixture,List.of("P004")),"remote",result));
        }
    }
    @TutorCase(id="L10-04",title="Tutor 정책 도구 서버",sample="Tutor의 정책 조회 도구 노출",goal="선택 서버 모드에서 @Tool을 MCP로 노출합니다.",requires="mcp-server")
    public Outcome exposeTutorPolicyTool(RunContext c) {
        if(!serverEnabled) return Outcome.unsupported("선택 MCP 서버는 scripts/run.sh --mcp-server로 실행하세요. Tutor의 /mcp에 가상 정책 도구를 노출합니다.");
        int port=environment.getProperty("local.server.port",Integer.class,serverPort);
        try(var client=connect("http://127.0.0.1:"+port)) {
            var result=client.callTool(new McpSchema.CallToolRequest("getTutorPolicy",Map.of()));
            if(Boolean.TRUE.equals(result.isError())) throw new IllegalStateException("Tutor 정책 도구 실패");
            return Outcome.ok("Tutor의 @Tool을 MCP로 발견하고 호출했습니다.",Map.of("tools",client.listTools().tools(),"result",result));
        }
    }
}
