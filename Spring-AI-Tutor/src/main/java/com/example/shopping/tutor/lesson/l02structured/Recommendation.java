package com.example.shopping.tutor.lesson.l02structured;

import java.util.*;

/** 후보 ID는 변환 후 허용 목록과 대조해야 한다. */
public record Recommendation(String productId, String reason) {}
