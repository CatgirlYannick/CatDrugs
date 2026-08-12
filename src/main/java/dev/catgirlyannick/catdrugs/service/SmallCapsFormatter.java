package dev.catgirlyannick.catdrugs.service;

public final class SmallCapsFormatter {
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String[] SMALL_CAPS = {
            "ᴀ", "ʙ", "ᴄ", "ᴅ", "ᴇ", "ꜰ", "ɢ", "ʜ", "ɪ", "ᴊ", "ᴋ", "ʟ", "ᴍ",
            "ɴ", "ᴏ", "ᴘ", "ǫ", "ʀ", "ꜱ", "ᴛ", "ᴜ", "ᴠ", "ᴡ", "x", "ʏ", "ᴢ"
    };

    private SmallCapsFormatter() {
    }

    public static String formatTemplate(String input) {
        return format(input, true);
    }

    public static String formatValue(String input) {
        return format(input, false);
    }

    private static String format(String input, boolean preservePlaceholders) {
        StringBuilder result = new StringBuilder(input.length());
        boolean insideTag = false;
        boolean insidePlaceholder = false;
        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);
            if (character == '<' && !insidePlaceholder) {
                insideTag = true;
                result.append(character);
                continue;
            }
            if (character == '>' && insideTag) {
                insideTag = false;
                result.append(character);
                continue;
            }
            if (preservePlaceholders && character == '{' && !insideTag) {
                insidePlaceholder = true;
                result.append(character);
                continue;
            }
            if (preservePlaceholders && character == '}' && insidePlaceholder) {
                insidePlaceholder = false;
                result.append(character);
                continue;
            }
            if (insideTag || insidePlaceholder) {
                result.append(character);
                continue;
            }
            int letter = LOWER.indexOf(character);
            if (letter < 0) letter = UPPER.indexOf(character);
            result.append(letter < 0 ? character : SMALL_CAPS[letter]);
        }
        return result.toString();
    }
}
