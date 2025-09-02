package api.helpers;

import java.util.Random;

public class RegexGenerator {
    private final Random random;

    public RegexGenerator() {
        this.random = new Random();
    }

    public String generateValue(String pattern) {
        // Handle ranges like [0-5]{5}
        if (pattern.matches("\\[[0-9]-[0-9]\\]\\{\\d+\\}")) {
            String inner = pattern.substring(1, pattern.indexOf(']'));
            String[] parts = inner.split("-");
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            int length = Integer.parseInt(pattern.substring(pattern.indexOf('{') + 1, pattern.indexOf('}')));
            return generateFromRange(start, end, length);
        }
        // Handle simple numeric patterns
        else if (pattern.matches("\\d+")) {
            return generateNumeric(Integer.parseInt(pattern));
        }
        // Handle digit count patterns
        else if (pattern.matches("\\\\d\\{\\d+\\}")) {
            int length = Integer.parseInt(pattern.substring(4, pattern.length()-1));
            return generateNumeric(length);
        }
        // fallback
        return pattern;
    }

    private String generateFromRange(int start, int end, int length) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(start + random.nextInt(end - start + 1));
        }
        return result.toString();
    }

    private String generateNumeric(int length) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(random.nextInt(10));
        }
        return result.toString();
    }
}