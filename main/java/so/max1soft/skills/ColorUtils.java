package so.max1soft.skills;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.bungee.api.ChatColor;

public class ColorUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("&x(&[0-9a-fA-F]){6}");

    public static String formatColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = message.substring(matcher.start() + 2, matcher.end());
            String minecraftColor = ChatColor.of(hex).toString();
            matcher.appendReplacement(buffer, minecraftColor);
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}

