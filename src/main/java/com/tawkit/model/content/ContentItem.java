package com.tawkit.model.content;

/**
 * A single piece of rotating content: a Quran verse, a hadith, or adhkar.
 * The Arabic text is optional; the translation is what appears on screen.
 */
public class ContentItem {

    private final ContentType type;
    private final String arabicText;
    private final String translation;
    private final String reference;

    public ContentItem(ContentType type, String arabicText, String translation, String reference) {
        this.type = type;
        this.arabicText = arabicText;
        this.translation = translation;
        this.reference = reference;
    }

    public ContentType getType() {
        return type;
    }

    public String getArabicText() {
        return arabicText;
    }

    public String getTranslation() {
        return translation;
    }

    public String getReference() {
        return reference;
    }

    public boolean hasArabic() {
        return arabicText != null && !arabicText.isBlank();
    }
}
