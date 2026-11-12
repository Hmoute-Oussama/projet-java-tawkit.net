package com.tawkit.service.content;

import com.tawkit.model.content.ContentItem;
import com.tawkit.model.content.ContentType;
import com.tawkit.model.content.Slide;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads the rotating display content: Qur'an verses, hadith, adhkar and
 * user announcements. Bundled data lives in classpath resources
 * (<code>/data/*.txt</code>, one item per line, fields separated by '|'):
 *
 * <pre>arabic text | translation | reference</pre>
 *
 * User slides are read from <code>&lt;home&gt;/.tawkit/slides/</code>:
 * <code>slides.txt</code> for text/URL entries and image files (.jpg/.png)
 * placed directly in the folder.
 */
public class ContentService {

    private final List<ContentItem> verses = new ArrayList<>();
    private final List<ContentItem> hadiths = new ArrayList<>();
    private final List<ContentItem> adhkar = new ArrayList<>();
    private final List<String> announcements = new ArrayList<>();
    private final List<Slide> slides = new ArrayList<>();

    private final File slidesFolder;

    public ContentService() {
        this(new File(System.getProperty("user.home"), ".tawkit/slides"));
    }

    public ContentService(File slidesFolder) {
        this.slidesFolder = slidesFolder;
        loadBundled();
        loadUserSlides();
    }

    public List<ContentItem> getVerses() {
        return Collections.unmodifiableList(verses);
    }

    public List<ContentItem> getHadiths() {
        return Collections.unmodifiableList(hadiths);
    }

    public List<ContentItem> getAdhkar() {
        return Collections.unmodifiableList(adhkar);
    }

    public List<String> getAnnouncements() {
        return Collections.unmodifiableList(announcements);
    }

    public List<Slide> getSlides() {
        return Collections.unmodifiableList(slides);
    }

    public File getSlidesFolder() {
        return slidesFolder;
    }

    public void setAnnouncements(List<String> items) {
        announcements.clear();
        if (items != null) announcements.addAll(items);
    }

    public void reloadSlides() {
        slides.clear();
        loadUserSlides();
    }

    // ------------------------------------------------------------- loading

    private void loadBundled() {
        verses.addAll(readItems("/data/quran.txt", ContentType.QURAN_VERSE));
        hadiths.addAll(readItems("/data/hadith.txt", ContentType.HADITH));
        adhkar.addAll(readItems("/data/azkar.txt", ContentType.ADHKAR));
        announcements.addAll(readLines("/data/announcements.txt"));
    }

    private List<ContentItem> readItems(String resource, ContentType type) {
        List<ContentItem> items = new ArrayList<>();
        for (String line : readLines(resource)) {
            String[] parts = line.split("\\|", 3);
            if (parts.length >= 1) {
                String arabic = parts.length > 0 ? parts[0].trim() : "";
                String translation = parts.length > 1 ? parts[1].trim() : "";
                String reference = parts.length > 2 ? parts[2].trim() : "";
                if (arabic.isEmpty() && translation.isEmpty()) continue;
                items.add(new ContentItem(type, arabic, translation, reference));
            }
        }
        return items;
    }

    private List<String> readLines(String resource) {
        List<String> lines = new ArrayList<>();
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            if (in == null) return lines;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) lines.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("[Content] failed reading " + resource + ": " + e.getMessage());
        }
        return lines;
    }

    private void loadUserSlides() {
        if (slidesFolder.isDirectory()) {
            File index = new File(slidesFolder, "slides.txt");
            if (index.isFile()) {
                try {
                    for (String line : Files.readAllLines(index.toPath(), StandardCharsets.UTF_8)) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        Slide slide = Slide.parse(line);
                        if (slide != null) slides.add(slide);
                    }
                } catch (IOException e) {
                    System.err.println("[Content] slides.txt unreadable: " + e.getMessage());
                }
            }
            // Also pick up image files dropped in the folder directly
            File[] images = slidesFolder.listFiles(
                    (dir, name) -> name.toLowerCase().endsWith(".jpg")
                            || name.toLowerCase().endsWith(".jpeg")
                            || name.toLowerCase().endsWith(".png"));
            if (images != null) {
                for (File img : images) {
                    Slide s = new Slide(Slide.Kind.IMAGE, img.getName());
                    if (!slides.contains(s)) slides.add(s);
                }
            }
        } else {
            try {
                Files.createDirectories(slidesFolder.toPath());
            } catch (IOException ignored) {
            }
        }
        Path marker = slidesFolder.toPath().resolve("README.txt");
        if (!Files.exists(marker)) {
            try {
                Files.writeString(marker, """
                        Put your slides here.
                        - slides.txt : one slide per line (text, or a URL, or a picture filename)
                        - '|' inside a text creates a line break
                        - .jpg / .png files in this folder are picked up automatically
                        """, StandardCharsets.UTF_8);
            } catch (IOException ignored) {
            }
        }
    }
}
