package za.ac.mycput.service.support;

import za.ac.mycput.dto.JobDtos.MatchResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Compares a student's skills with a job's requirements (both comma-separated lists).
 *
 * <p>A requirement counts as matched when it and one of the student's skills are the same phrase,
 * or one contains the other as whole words — so the skill "Java" matches the requirement
 * "Java or Python", and "HTML/CSS" matches "html css". Matching is case-insensitive.
 *
 * <p>Score = matched requirements / total requirements × 100, rounded.
 */
public final class SkillMatcher {

    private SkillMatcher() {}

    /** Splits "Java, SQL; Python" into ["Java", "SQL", "Python"], trimming and removing duplicates. */
    public static List<String> toList(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        Arrays.stream(csv.split("[,;\\n]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(s -> {
                    if (seen.add(normalise(s))) {
                        result.add(s);
                    }
                });
        return result;
    }

    public static MatchResult match(String studentSkills, String jobRequirements) {
        List<String> requirements = toList(jobRequirements);
        List<String> skills = toList(studentSkills).stream().map(SkillMatcher::normalise).toList();

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String requirement : requirements) {
            String req = normalise(requirement);
            boolean hit = skills.stream().anyMatch(skill -> containsPhrase(req, skill) || containsPhrase(skill, req));
            (hit ? matched : missing).add(requirement);
        }
        int score = requirements.isEmpty() ? 0 : Math.round(matched.size() * 100f / requirements.size());
        return new MatchResult(score, matched, missing);
    }

    /** Lowercase, and treat any character other than letters, digits, '+' and '#' as a word separator. */
    static String normalise(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+#]+", " ").trim();
    }

    private static boolean containsPhrase(String haystack, String needle) {
        if (needle.isEmpty()) {
            return false;
        }
        return (" " + haystack + " ").contains(" " + needle + " ");
    }
}
