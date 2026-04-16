package main.util;

import java.util.regex.Pattern;

/**
 * Validates UK company registration numbers as used on Companies House records
 * (8 digits, or 2-letter prefix + 6 digits, optional spaces).
 */
public final class UkCompanyNumberValidator {

    private static final Pattern EIGHT_DIGITS = Pattern.compile("^\\d{8}$");
    private static final Pattern PREFIXED = Pattern.compile("^[A-Za-z]{2}\\d{6}$");

    private UkCompanyNumberValidator() {
    }

    public static boolean isValid(String raw) {
        if (raw == null) {
            return false;
        }
        String compact = raw.replaceAll("\\s", "").trim().toUpperCase();
        if (compact.isEmpty()) {
            return false;
        }
        return EIGHT_DIGITS.matcher(compact).matches() || PREFIXED.matcher(compact).matches();
    }
}
