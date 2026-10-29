package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l10mcp.McpLesson;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.springframework.stereotype.Component;

import java.util.*;

/** 웹 caseId를 학습 메소드에 연결한다. 학습할 때는 McpLesson부터 읽는다. */
@Component
public class McpCases {
    @org.springframework.beans.factory.annotation.Value("${spring.ai.mcp.server.enabled:false}")
    private boolean serverEnabled;

    @org.springframework.beans.factory.annotation.Value("${server.port:8090}")
    private int serverPort;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.core.env.Environment environment;

    @TutorCase(
            id = "L10-01",
            title = "MCP discovery와 직접 호출",
            sample = "원격 도구 목록과 배송 정책 보기",
            goal = "실제 Streamable HTTP 서버의 schema와 호출 결과를 확인합니다.",
            requires = "mcp",
            lesson = McpLesson.class,
            learningMethod = "discoverAndCall")
    public Outcome discoverAndCall(LessonExecution execution) {
        var result = new McpLesson().discoverAndCall(execution.settings.mcpUrl());
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L10-02",
            title = "모델의 원격 도구 선택",
            sample = "1만5천원 이하 음료 추천해줘",
            goal = "허용한 MCP callback만 ChatClient에 등록합니다.",
            requires = "mcp",
            lesson = McpLesson.class,
            learningMethod = "letModelUseRemoteTool")
    public Outcome letModelUseRemoteTool(LessonExecution execution) {
        var result =
                new McpLesson()
                        .letModelUseRemoteTool(
                                execution.client(),
                                execution.userInput(),
                                execution.settings.mcpUrl(),
                                execution::bounded);
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L10-03",
            title = "로컬·원격 배송 견적",
            sample = "무료배송 견적 비교",
            goal = "같은 fixture의 사실과 transport 오류를 구분합니다.",
            requires = "mcp",
            lesson = McpLesson.class,
            learningMethod = "compareLocalAndRemote")
    public Outcome compareLocalAndRemote(LessonExecution execution) {
        var result =
                new McpLesson()
                        .compareLocalAndRemote(execution.fixture, execution.settings.mcpUrl());
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L10-04",
            title = "Tutor 정책 도구 서버",
            sample = "Tutor의 정책 조회 도구 노출",
            goal = "선택 서버 모드에서 @Tool을 MCP로 노출합니다.",
            requires = "mcp-server",
            lesson = McpLesson.class,
            learningMethod = "exposeTutorPolicyTool")
    public Outcome exposeTutorPolicyTool(LessonExecution execution) {
        var result = new McpLesson().exposeTutorPolicyTool(serverEnabled, serverPort, environment);
        return new Outcome(result.status(), result.answer(), result.data());
    }
}
