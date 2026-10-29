package com.example.shopping.tutor.web;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class TutorErrors {
    @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class,HttpMessageNotReadableException.class})
    ResponseEntity<?> badRequest(Exception e) { return response(400,"INVALID_INPUT","입력·옵션·파일 또는 구조화 결과를 확인하세요."); }
    @ExceptionHandler(NoSuchElementException.class) ResponseEntity<?> missing(Exception e) { return response(404,"NOT_FOUND","없는 실습입니다."); }
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class) ResponseEntity<?> missingResource(Exception e) { return response(404,"NOT_FOUND","없는 경로입니다."); }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class) ResponseEntity<?> tooLarge(Exception e) { return response(413,"FILE_TOO_LARGE","파일은 5MB 이하여야 합니다."); }
    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class) ResponseEntity<?> missingPart(Exception e) { return response(400,"INVALID_INPUT","질문과 파일을 함께 전송하세요."); }
    @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e) { return response(e.getStatusCode().value(),"REQUEST_REJECTED",e.getReason()); }
    @ExceptionHandler(Exception.class) ResponseEntity<?> failed(Exception e) {
        // 원시 provider 오류에는 요청/키가 포함될 수 있으므로 응답에 복사하지 않는다.
        return response(502,"EXECUTION_FAILED","실습 실행에 실패했습니다. 모델 설정·선택 인프라 연결을 확인하세요. 실패 턴은 상태에 반영하지 않았습니다.");
    }
    private ResponseEntity<?> response(int status,String code,String message) {
        return ResponseEntity.status(status).body(Map.of("code",code,"message",message,"retryable",status>=500,"runId",UUID.randomUUID().toString()));
    }
}
