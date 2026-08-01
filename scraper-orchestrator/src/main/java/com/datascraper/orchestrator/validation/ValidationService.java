package com.datascraper.orchestrator.validation;

import com.datascraper.orchestrator.model.CompanyDraft;
import com.datascraper.orchestrator.model.ValidationOutcome;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ValidationService {

    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNTRY = Pattern.compile("^[A-Z]{2}$");
    private static final Pattern WEBSITE = Pattern.compile("^https://[^\\s/$.?#].[^\\s]*$", Pattern.CASE_INSENSITIVE);
    private static final double CONFIDENCE_WARN_THRESHOLD = 0.3;

    public ValidationOutcome validate(CompanyDraft draft) {
        List<String> warnings = new ArrayList<>();
        boolean softFailure = false;

        if (draft.getName() == null || draft.getName().isBlank()) {
            draft.setIncomplete(true);
            warnings.add("Company name is blank");
        }

        if (draft.getEmail() != null && !draft.getEmail().isBlank() && !EMAIL.matcher(draft.getEmail()).matches()) {
            warnings.add("Invalid email format: " + draft.getEmail());
            softFailure = true;
        }

        if (draft.getPhone() != null && !draft.getPhone().isBlank()) {
            int digits = draft.getPhone().replace("+", "").length();
            if (digits < 8 || digits > 15) {
                warnings.add("Phone digit length out of range: " + draft.getPhone());
                softFailure = true;
            }
        }

        if (draft.getWebsite() != null && !draft.getWebsite().isBlank() && !WEBSITE.matcher(draft.getWebsite()).matches()) {
            warnings.add("Invalid website URL: " + draft.getWebsite());
            softFailure = true;
        }

        if (draft.getCountryCode() != null && !draft.getCountryCode().isBlank()
                && !COUNTRY.matcher(draft.getCountryCode()).matches()) {
            warnings.add("Country code must be 2 letters: " + draft.getCountryCode());
            softFailure = true;
        }

        if (draft.getConfidenceScore() < CONFIDENCE_WARN_THRESHOLD) {
            warnings.add("Confidence below threshold (%.2f < %.2f)"
                    .formatted(draft.getConfidenceScore(), CONFIDENCE_WARN_THRESHOLD));
        }

        draft.setValidationFailed(softFailure);
        draft.getNotes().addAll(warnings);

        boolean valid = !draft.isIncomplete();
        return new ValidationOutcome(valid, draft.isIncomplete(), softFailure, warnings);
    }
}
