package dev.cursedatom.cursedaddons.features.images;

import dev.cursedatom.cursedaddons.config.ConfigKeys;
import dev.cursedatom.cursedaddons.utils.ConfigProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans a Component tree for plain-text image URLs and wraps them
 * with ClickEvent.OpenUrl so the existing hover preview logic picks them up.
 *
 * Uses Component.visit() to flatten the entire tree (including TranslatableContents
 * arguments) into styled text segments, then rebuilds with annotated URLs.
 */
public final class PlainTextUrlAnnotator {

    private static final Pattern URL_PATTERN = Pattern.compile(
        "https?://[^\\s<>\"')\\]]+", Pattern.CASE_INSENSITIVE
    );

    private PlainTextUrlAnnotator() {}

    public static boolean isEnabled() {
        return ConfigProvider.getBoolean(ConfigKeys.IMAGE_DETECT_PLAIN_URLS, false);
    }

    /**
     * Annotates plain-text image URLs in the given component tree.
     * Returns the original component if no changes are needed.
     */
    public static Component annotate(Component component) {
        if (!isEnabled()) return component;

        // Flatten the entire component tree into (style, text) segments
        List<StyledSegment> segments = new ArrayList<>();
        component.visit((style, text) -> {
            segments.add(new StyledSegment(style, text));
            return Optional.empty();
        }, Style.EMPTY);

        // Check if any segment needs annotation
        boolean needsAnnotation = false;
        for (StyledSegment seg : segments) {
            if (seg.style.getClickEvent() != null) continue;
            Matcher m = URL_PATTERN.matcher(seg.text);
            while (m.find()) {
                String url = m.group();
                if (ImageHoverPreview.isImageUrl(url) && ImageHoverPreview.isWhitelisted(url)) {
                    needsAnnotation = true;
                    break;
                }
            }
            if (needsAnnotation) break;
        }

        if (!needsAnnotation) return component;

        // Rebuild the component with annotated URLs
        MutableComponent result = Component.empty();
        for (StyledSegment seg : segments) {
            if (seg.style.getClickEvent() != null) {
                result.append(Component.literal(seg.text).setStyle(seg.style));
                continue;
            }

            Matcher m = URL_PATTERN.matcher(seg.text);
            int lastEnd = 0;
            boolean foundImageUrl = false;

            while (m.find()) {
                String url = m.group();
                if (!ImageHoverPreview.isImageUrl(url) || !ImageHoverPreview.isWhitelisted(url)) {
                    continue;
                }
                foundImageUrl = true;

                if (m.start() > lastEnd) {
                    result.append(Component.literal(seg.text.substring(lastEnd, m.start())).setStyle(seg.style));
                }

                try {
                    URI uri = new URI(url);
                    Style urlStyle = seg.style.withClickEvent(new ClickEvent.OpenUrl(uri));
                    result.append(Component.literal(url).setStyle(urlStyle));
                } catch (Exception e) {
                    result.append(Component.literal(url).setStyle(seg.style));
                }

                lastEnd = m.end();
            }

            if (!foundImageUrl) {
                result.append(Component.literal(seg.text).setStyle(seg.style));
            } else if (lastEnd < seg.text.length()) {
                result.append(Component.literal(seg.text.substring(lastEnd)).setStyle(seg.style));
            }
        }

        return result;
    }

    private record StyledSegment(Style style, String text) {}
}
