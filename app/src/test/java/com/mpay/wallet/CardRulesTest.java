package com.mpay.wallet;

import org.junit.Test;
import java.time.YearMonth;
import static org.junit.Assert.*;

public class CardRulesTest {
    @Test public void recognizesNetworksAtBoundaries() {
        assertEquals("Мир", CardRules.network("2204000000000000"));
        assertEquals("Visa", CardRules.network("4111111111111111"));
        assertEquals("Mastercard", CardRules.network("2221000000000000"));
        assertEquals("Mastercard", CardRules.network("2720000000000000"));
        assertEquals("Другая", CardRules.network("2721000000000000"));
        assertEquals("UnionPay", CardRules.network("6200000000000000"));
    }
    @Test public void rejectsInvalidNumbers() {
        assertTrue(CardRules.luhn("4111 1111 1111 1111"));
        assertFalse(CardRules.luhn("4111111111111112"));
        assertFalse(CardRules.luhn("0000000000000000"));
        assertFalse(CardRules.luhn("1111"));
    }
    @Test public void expiryIncludesCurrentMonth() {
        YearMonth now = YearMonth.of(2026, 11);
        assertTrue(CardRules.validExpiry("11/26", now));
        assertFalse(CardRules.validExpiry("10/26", now));
        assertFalse(CardRules.validExpiry("13/26", now));
        assertFalse(CardRules.validExpiry("1/27", now));
    }
    @Test public void parsesOcrAndRejectsBadChecksum() {
        String[] result = CardRules.fromOcr("VISA\n4111 1111 1111 1111\nVALID THRU 12/29\nIVAN IVANOV");
        assertEquals("4111111111111111", result[0]);
        assertEquals("12/29", result[1]);
        assertEquals("IVAN IVANOV", result[2]);
        assertEquals("", CardRules.fromOcr("4111 1111 1111 1112")[0]);
    }
}
