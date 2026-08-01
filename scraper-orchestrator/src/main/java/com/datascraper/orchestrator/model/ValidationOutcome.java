package com.datascraper.orchestrator.model;

import java.util.ArrayList;
import java.util.List;

public record ValidationOutcome(
        boolean valid,
        boolean incomplete,
        boolean softFailure,
        List<String> warnings
) {
    public ValidationOutcome {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
    }

    public static ValidationOutcome ok() {
        return new ValidationOutcome(true, false, false, List.of());
    }
}
