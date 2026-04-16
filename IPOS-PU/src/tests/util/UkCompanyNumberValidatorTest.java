package tests.util;

import main.util.UkCompanyNumberValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UkCompanyNumberValidatorTest {

    @Test
    void acceptsEightDigits() {
        assertTrue(UkCompanyNumberValidator.isValid("12345678"));
        assertTrue(UkCompanyNumberValidator.isValid(" 12 34 56 78 "));
    }

    @Test
    void acceptsPrefixedFormat() {
        assertTrue(UkCompanyNumberValidator.isValid("SC123456"));
        assertTrue(UkCompanyNumberValidator.isValid("oc301229"));
    }

    @Test
    void rejectsInvalid() {
        assertFalse(UkCompanyNumberValidator.isValid(null));
        assertFalse(UkCompanyNumberValidator.isValid(""));
        assertFalse(UkCompanyNumberValidator.isValid("CH-1234"));
        assertFalse(UkCompanyNumberValidator.isValid("1234567"));
    }
}
