package com.example.ailearning.l06decisions;

import com.example.ailearning.l06decisions.support.RunState;
import java.util.List;

public record RunResult(String mode, String status, String answer, List<Offer> offers,
                        int clientCalls, int modelCalls, int toolCalls, List<RunState.Step> steps) {}
