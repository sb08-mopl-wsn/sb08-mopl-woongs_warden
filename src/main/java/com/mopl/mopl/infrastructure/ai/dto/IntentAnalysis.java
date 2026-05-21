package com.mopl.mopl.infrastructure.ai.dto;

import java.util.List;

public record IntentAnalysis
(
        String intent,
        List<String> keywords,
        String contentType
) {}
