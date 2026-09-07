package ecommerce.modules.notification.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {{placeholder}} tokens in notification template strings against a variables map.
 * Unmatched placeholders are left unchanged so missing data is visible rather than silently dropped.
 */
@Component
public class TemplateInterpolator {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    public String interpolate(String template, Map<String, String> variables) {
        if (template == null || template.isBlank()) return template;
        if (variables == null || variables.isEmpty()) return template;

        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String key         = matcher.group(1);
            String replacement = variables.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
