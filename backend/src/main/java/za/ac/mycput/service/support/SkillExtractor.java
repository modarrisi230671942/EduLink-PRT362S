package za.ac.mycput.service.support;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Finds known skills in the text of a CV.
 *
 * <p>The dictionary ({@code resources/skills/skill-dictionary.txt}) lists each skill with optional aliases,
 * e.g. {@code JavaScript | JS}. A skill is found when its name or any alias appears in the CV as whole words
 * (case-insensitive, using the same normalisation as {@link SkillMatcher}), so "JS" in a CV suggests "JavaScript".
 * Runs fully offline — no external AI service is needed.
 */
@Component
public class SkillExtractor {

    /** Only the first pages are read — enough for any CV, and it caps the work for huge files. */
    private static final int MAX_PAGES = 10;

    private record Entry(String canonical, List<String> phrases) {}

    private final List<Entry> dictionary;

    public SkillExtractor() {
        this(new ClassPathResource("skills/skill-dictionary.txt"));
    }

    SkillExtractor(Resource dictionaryFile) {
        this.dictionary = load(dictionaryFile);
    }

    /** Skills found in a PDF, in dictionary order. */
    public List<String> extractFromPdf(Resource pdf) {
        try (PDDocument document = Loader.loadPDF(pdf.getContentAsByteArray())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(MAX_PAGES);
            return extractFromText(stripper.getText(document));
        } catch (IOException e) {
            // A damaged or encrypted PDF simply yields no suggestions
            return List.of();
        }
    }

    /** Skills found in plain text, in dictionary order. */
    public List<String> extractFromText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String haystack = " " + SkillMatcher.normalise(text) + " ";
        List<String> found = new ArrayList<>();
        for (Entry entry : dictionary) {
            if (entry.phrases().stream().anyMatch(phrase -> haystack.contains(" " + phrase + " "))) {
                found.add(entry.canonical());
            }
        }
        return found;
    }

    private static List<Entry> load(Resource file) {
        List<Entry> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\|");
                String canonical = parts[0].trim();
                LinkedHashSet<String> phrases = new LinkedHashSet<>();
                for (String part : parts) {
                    String phrase = SkillMatcher.normalise(part.trim());
                    // Single letters (e.g. "C", "R") are too ambiguous to match on their own in free text
                    if (phrase.length() > 1 || phrase.equals("c++") || phrase.equals("c#")) {
                        phrases.add(phrase);
                    }
                }
                if (!phrases.isEmpty()) {
                    entries.add(new Entry(canonical, List.copyOf(phrases)));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load skill dictionary", e);
        }
        return List.copyOf(entries);
    }
}
