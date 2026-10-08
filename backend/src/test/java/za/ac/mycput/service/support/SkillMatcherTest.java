package za.ac.mycput.service.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.ac.mycput.dto.JobDtos.MatchResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillMatcherTest {

    @Test
    @DisplayName("Splits, trims and de-duplicates comma-separated lists")
    void toList() {
        assertThat(SkillMatcher.toList(" Java,SQL ; python,, java ")).containsExactly("Java", "SQL", "python");
        assertThat(SkillMatcher.toList(null)).isEmpty();
        assertThat(SkillMatcher.toList("   ")).isEmpty();
    }

    @Test
    @DisplayName("Full match scores 100")
    void fullMatch() {
        MatchResult result = SkillMatcher.match("Java, SQL, Spring Boot", "java, sql, spring boot");
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.missing()).isEmpty();
    }

    @Test
    @DisplayName("Partial match lists matched and missing requirements")
    void partialMatch() {
        MatchResult result = SkillMatcher.match("Java, SQL, Python", "Java, Spring Boot, SQL, HTML/CSS");
        assertThat(result.score()).isEqualTo(50);
        assertThat(result.matched()).containsExactly("Java", "SQL");
        assertThat(result.missing()).containsExactly("Spring Boot", "HTML/CSS");
    }

    @Test
    @DisplayName("A skill inside a longer requirement counts as a whole-word match")
    void wholeWordContainment() {
        assertThat(SkillMatcher.match("Java", "Java or Python").score()).isEqualTo(100);
        assertThat(SkillMatcher.match("html/css", "HTML/CSS").score()).isEqualTo(100);
        // "Java" must not match "JavaScript"
        assertThat(SkillMatcher.match("Java", "JavaScript").score()).isZero();
    }

    @Test
    @DisplayName("No skills or no requirements gives a score of 0")
    void emptyInputs() {
        assertThat(SkillMatcher.match(null, "Java").score()).isZero();
        assertThat(SkillMatcher.match("Java", null).score()).isZero();
        assertThat(SkillMatcher.match("Java", null).matched()).isEqualTo(List.of());
    }
}
