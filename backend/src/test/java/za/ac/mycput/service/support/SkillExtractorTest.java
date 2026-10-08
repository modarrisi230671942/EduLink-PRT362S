package za.ac.mycput.service.support;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class SkillExtractorTest {

    private final SkillExtractor extractor = new SkillExtractor();

    @Test
    void findsSkillsAndAliasesAsWholeWords() {
        String cv = "Final-year IT student. Built a REST API with Spring Boot and a JS frontend in React.js. "
                + "Deployed with Docker on AWS; used k8s in a group project. Strong teamwork and problem-solving.";
        assertThat(extractor.extractFromText(cv))
                .contains("JavaScript", "React", "Spring Boot", "Docker", "AWS", "Kubernetes", "REST APIs",
                        "Teamwork", "Problem Solving");
    }

    @Test
    void ignoresPartialWordsAndCommonWords() {
        // "Java" must not be found inside "JavaScript"; "go", "node", "express" and "spring" are ordinary words
        assertThat(extractor.extractFromText("I know JavaScript. Let's go! I express myself each spring at the node."))
                .containsExactly("JavaScript");
    }

    @Test
    void readsTextFromARealPdf() throws IOException {
        byte[] pdf = pdfContaining("Skills: Python, SQL, Power BI and Excel");
        assertThat(extractor.extractFromPdf(new ByteArrayResource(pdf)))
                .containsExactlyInAnyOrder("Python", "SQL", "Power BI", "Excel");
    }

    @Test
    void damagedPdfYieldsNoSkillsInsteadOfFailing() {
        assertThat(extractor.extractFromPdf(new ByteArrayResource("%PDF-1.7 not really a pdf".getBytes()))).isEmpty();
    }

    private static byte[] pdfContaining(String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 700);
                content.showText(text);
                content.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
