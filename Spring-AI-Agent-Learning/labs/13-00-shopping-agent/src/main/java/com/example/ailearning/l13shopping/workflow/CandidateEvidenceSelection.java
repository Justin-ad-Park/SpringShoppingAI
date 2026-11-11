package com.example.ailearning.l13shopping.workflow;

import java.util.List;

/** 모델이 답변에 사용할 후보로 고른, 실제 도구 결과의 ID 목록이다. */
public record CandidateEvidenceSelection(List<String> productIds, List<String> sourceIds) {}
