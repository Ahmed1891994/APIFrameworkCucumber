package api.helpers;

import java.util.Random;

public class RegexGenerator {
    private final Random random;

    public RegexGenerator() {
        this.random = new Random();
    }

    public String generateValue(String pattern) {
        try {
            // Handle ranges like [0-5]{5}
            if (pattern.matches("\\[[0-9]-[0-9]]\\{\\d+\\}")) {
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
            // Handle email pattern
            else if (pattern.equalsIgnoreCase("email")) {
                return generateEmail();
            }
            // Handle phone pattern
            else if (pattern.equalsIgnoreCase("phone")) {
                return generatePhone();
            }
            // Handle UUID pattern
            else if (pattern.equalsIgnoreCase("uuid")) {
                return generateUUID();
            }
            // Handle string with length pattern
            else if (pattern.matches("string\\(\\d+\\)")) {
                int length = Integer.parseInt(pattern.substring(7, pattern.length()-1));
                return generateString(length);
            }
            // fallback - return as is
            return pattern;
        } catch (Exception e) {
            return pattern; // Return the pattern as fallback
        }
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

    private String generateEmail() {
        return "test" + System.currentTimeMillis() + "@example.com";
    }

    private String generatePhone() {
        return "+1" + generateNumeric(10);
    }

    private String generateUUID() {
        return java.util.UUID.randomUUID().toString();
    }

    private String generateString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(characters.charAt(random.nextInt(characters.length())));
        }
        return result.toString();
    }
}