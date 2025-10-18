package io.github.chi2l3s.nextlib.color;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtilImpl implements ColorUtil {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");

    @Override
    public String formatMessage(String message) {
        if (message == null) return "";

        Matcher matcher = HEX_PATTERN.matcher(message);
        while (matcher.find()) {
            String color = matcher.group();
            message = message.replace(color, ChatColor.of(color.substring(1)) + "");
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
