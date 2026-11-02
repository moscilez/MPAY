package com.mpay.wallet;

import java.time.YearMonth;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure local parsing. Network detection is a hint, never proof of payment eligibility. */
public final class CardRules {
    private CardRules() {}
    public static final String[] NETWORKS = {"Мир", "Visa", "Mastercard", "UnionPay", "Другая"};
    public static String digits(String value) { return value.replaceAll("[^0-9]", ""); }
    public static boolean luhn(String value) {
        String n = digits(value);
        if (n.length() < 12 || n.length() > 19 || n.matches("0+")) return false;
        int sum = 0;
        boolean twice = false;
        for (int i = n.length() - 1; i >= 0; i--) {
            int d = n.charAt(i) - '0';
            if (twice) { d *= 2; if (d > 9) d -= 9; }
            sum += d; twice = !twice;
        }
        return sum % 10 == 0;
    }
    public static String network(String value) {
        String n = digits(value);
        if (n.length() < 4) return "Другая";
        int p = Integer.parseInt(n.substring(0, 4));
        if (p >= 2200 && p <= 2204) return "Мир";
        if (n.startsWith("4")) return "Visa";
        if ((p >= 5100 && p <= 5599) || (p >= 2221 && p <= 2720)) return "Mastercard";
        if (n.startsWith("62")) return "UnionPay";
        return "Другая";
    }
    public static boolean validExpiry(String value, YearMonth now) {
        if (!value.matches("(0[1-9]|1[0-2])/\\d{2}")) return false;
        return !YearMonth.of(2000 + Integer.parseInt(value.substring(3)), Integer.parseInt(value.substring(0, 2))).isBefore(now);
    }
    public static String[] fromOcr(String text) {
        String number = "", expiry = "", holder = "";
        Matcher m = Pattern.compile("(?<!\\d)(?:\\d[ -]?){11,18}\\d(?!\\d)").matcher(text);
        while (m.find()) { String n = digits(m.group()); if (luhn(n)) { number = n; break; } }
        m = Pattern.compile("(?<!\\d)(0[1-9]|1[0-2])\\s*[/]\\s*(\\d{2})(?!\\d)").matcher(text);
        if (m.find()) expiry = m.group(1) + "/" + m.group(2);
        for (String line : text.split("\\n")) {
            String s = line.trim();
            if (s.matches("[A-Z]{2,}(?: +[A-Z]{2,}){1,3}") && !s.contains("VALID") && !s.contains("MEMBER") && !s.contains("BANK")) holder = s;
        }
        return new String[]{number, expiry, holder};
    }
}
