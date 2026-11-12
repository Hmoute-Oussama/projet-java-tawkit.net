package com.tawkit.model.content;

import java.io.File;
import java.net.URI;

/**
 * A user-defined slide shown as a slideshow between prayers.
 * A slide is either a text message (possibly multi-line via '|'),
 * a picture file (.jpg/.png) or a URL.
 */
public class Slide {

    public enum Kind { TEXT, IMAGE, URL }

    private final Kind kind;
    private final String content;   // text, image path, or URL

    public Slide(Kind kind, String content) {
        this.kind = kind;
        this.content = content;
    }

    public static Slide text(String text) {
        return new Slide(Kind.TEXT, text);
    }

    /** Creates the right kind of slide from user input (text / file / URL). */
    public static Slide parse(String input) {
        String v = input.trim();
        if (v.isEmpty()) return null;
        if (v.startsWith("http://") || v.startsWith("https://")) {
            return new Slide(Kind.URL, v);
        }
        if (v.toLowerCase().endsWith(".jpg") || v.toLowerCase().endsWith(".png")
                || v.toLowerCase().endsWith(".jpeg")) {
            return new Slide(Kind.IMAGE, v);
        }
        return new Slide(Kind.TEXT, v);
    }

    public Kind getKind() {
        return kind;
    }

    public String getContent() {
        return content;
    }

    /** Text with '|' converted to line breaks; null for image/url slides. */
    public String toDisplayText() {
        return kind == Kind.TEXT ? content.replace("|", "\n") : null;
    }

    /** Resolves the image to an absolute file in the slides folder if relative. */
    public File resolveImage(File slidesFolder) {
        File f = new File(content);
        if (f.isAbsolute() && f.exists()) return f;
        File resolved = new File(slidesFolder, content);
        return resolved.exists() ? resolved : null;
    }

    public boolean isReachableUrl() {
        try {
            new URI(content);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
