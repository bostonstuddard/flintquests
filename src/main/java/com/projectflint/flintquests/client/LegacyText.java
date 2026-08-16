package com.projectflint.flintquests.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts author-friendly legacy Minecraft formatting codes into Components.
 * Both '&' and the vanilla section-sign prefix are accepted.
 */
public final class LegacyText {
    private LegacyText() {
    }

    public static Component parse(String raw) {
        String input = raw == null ? "" : raw;
        MutableComponent result = Component.empty();
        List<ChatFormatting> active = new ArrayList<>();
        StringBuilder plain = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if ((current == '&' || current == '\u00A7') && i + 1 < input.length()) {
                char code = Character.toLowerCase(input.charAt(i + 1));
                ChatFormatting formatting = formatting(code);
                if (formatting != null) {
                    flush(result, plain, active);
                    i++;
                    if (code == 'r') {
                        active.clear();
                    } else if (isColorCode(code)) {
                        // Vanilla legacy color codes also clear decoration state.
                        active.clear();
                        active.add(formatting);
                    } else if (!active.contains(formatting)) {
                        active.add(formatting);
                    }
                    continue;
                }
            }
            plain.append(current);
        }

        flush(result, plain, active);
        return result;
    }

    public static List<Component> lines(String raw) {
        String normalized = (raw == null ? "" : raw)
                .replace("\\n", "\n")
                .replace("\r", "");
        String[] split = normalized.split("\n", -1);
        List<Component> lines = new ArrayList<>(split.length);
        for (String line : split) lines.add(parse(line));
        return lines;
    }

    private static void flush(MutableComponent result, StringBuilder plain, List<ChatFormatting> active) {
        if (plain.isEmpty()) return;
        MutableComponent part = Component.literal(plain.toString());
        if (!active.isEmpty()) part.withStyle(active.toArray(new ChatFormatting[0]));
        result.append(part);
        plain.setLength(0);
    }

    private static boolean isColorCode(char code) {
        return (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f');
    }

    private static ChatFormatting formatting(char code) {
        return switch (code) {
            case '0' -> ChatFormatting.BLACK;
            case '1' -> ChatFormatting.DARK_BLUE;
            case '2' -> ChatFormatting.DARK_GREEN;
            case '3' -> ChatFormatting.DARK_AQUA;
            case '4' -> ChatFormatting.DARK_RED;
            case '5' -> ChatFormatting.DARK_PURPLE;
            case '6' -> ChatFormatting.GOLD;
            case '7' -> ChatFormatting.GRAY;
            case '8' -> ChatFormatting.DARK_GRAY;
            case '9' -> ChatFormatting.BLUE;
            case 'a' -> ChatFormatting.GREEN;
            case 'b' -> ChatFormatting.AQUA;
            case 'c' -> ChatFormatting.RED;
            case 'd' -> ChatFormatting.LIGHT_PURPLE;
            case 'e' -> ChatFormatting.YELLOW;
            case 'f' -> ChatFormatting.WHITE;
            case 'k' -> ChatFormatting.OBFUSCATED;
            case 'l' -> ChatFormatting.BOLD;
            case 'm' -> ChatFormatting.STRIKETHROUGH;
            case 'n' -> ChatFormatting.UNDERLINE;
            case 'o' -> ChatFormatting.ITALIC;
            case 'r' -> ChatFormatting.RESET;
            default -> null;
        };
    }
}
