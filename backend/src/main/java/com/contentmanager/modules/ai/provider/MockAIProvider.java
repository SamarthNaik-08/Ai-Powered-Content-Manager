package com.contentmanager.modules.ai.provider;

import com.contentmanager.modules.ai.dto.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("mockAIProvider")
public class MockAIProvider implements AIProvider {

    @Override
    public String getProviderName() {
        return "MOCK";
    }

    @Override
    public GenerateContentResponse generate(GenerateContentRequest request) {
        String topic = request.getTopic() != null ? request.getTopic().trim() : "Modern Content Strategy";
        String tone = request.getTone() != null ? request.getTone() : "Professional";
        String type = request.getContentType() != null ? request.getContentType() : "ARTICLE";

        String title = String.format("The Ultimate Guide to %s: Practical Strategies and Insights", topic);
        String excerpt = String.format("Explore comprehensive strategies and actionable techniques for mastering %s with a %s approach.", topic, tone.toLowerCase());

        String body = String.format("""
                # %s

                In today's fast-evolving landscape, understanding **%s** is essential for staying competitive and delivering measurable impact.

                ## 1. Why %s Matters Now
                As modern organizations scale, the ability to effectively navigate %s determines long-term success. Adopting a %s tone enables clear communication across diverse stakeholders.

                - **Strategic Focus**: Prioritize high-impact initiatives that align with your core goals.
                - **Operational Excellence**: Automate repetitive workflows to unlock creative potential.
                - **Data-Driven Execution**: Continuously monitor performance indicators to refine approaches.

                ## 2. Key Implementation Steps
                Executing a successful plan requires discipline and structured milestones:
                1. Audit your current baseline and define target objectives.
                2. Assemble multidisciplinary teams empowered to innovate.
                3. Deploy modern tooling that enhances agility and collaboration.

                ## 3. Measuring Long-Term Impact
                Consistent evaluation guarantees sustained value delivery. Track engagement metrics, qualitative feedback, and return on investment over scheduled intervals.

                ## Conclusion
                Embracing %s is not merely an option—it is a cornerstone for forward-thinking professionals.
                """, title, topic, topic, topic, tone.toLowerCase(), topic);

        List<String> tags = Arrays.asList(
                topic.toLowerCase().replace(" ", "-"),
                "strategy",
                type.toLowerCase().replace("_", "-"),
                "best-practices"
        );

        return GenerateContentResponse.builder()
                .title(title)
                .content(body.trim())
                .excerpt(excerpt)
                .suggestedTags(tags)
                .build();
    }

    @Override
    public RewriteContentResponse rewrite(RewriteContentRequest request) {
        String original = request.getContent() != null ? request.getContent() : "";
        String tone = request.getTone() != null ? request.getTone() : "refined and polished";
        String style = request.getStyle() != null ? request.getStyle() : "concise";

        // Create an enhanced rewrite of the text
        StringBuilder rewritten = new StringBuilder();
        rewritten.append(original.trim());
        rewritten.append("\n\n*(Rewritten with a ").append(tone).append(" tone in a ").append(style).append(" style for maximum clarity and engagement)*");

        return RewriteContentResponse.builder()
                .rewrittenContent(rewritten.toString())
                .build();
    }

    @Override
    public SummarizeContentResponse summarize(SummarizeContentRequest request) {
        String content = request.getContent() != null ? request.getContent() : "";
        int wordCount = content.split("\\s+").length;

        String summary = String.format("This document spans approximately %d words, outlining strategic principles, key implementation milestones, and measurable success factors.", wordCount);
        List<String> keyPoints = Arrays.asList(
                "Core thesis establishes the vital significance of the subject matter.",
                "Identifies primary operational bottlenecks and actionable countermeasures.",
                "Emphasizes continuous optimization, alignment, and collaborative execution.",
                "Recommends automated monitoring and structured stakeholder governance."
        );

        return SummarizeContentResponse.builder()
                .summary(summary)
                .keyPoints(keyPoints)
                .build();
    }

    @Override
    public GenerateTitlesResponse generateTitles(GenerateTitlesRequest request) {
        String base = request.getTopicOrContent() != null ? request.getTopicOrContent().trim() : "Content Growth";
        if (base.length() > 50) {
            base = base.substring(0, 50).trim();
        }

        List<String> titles = Arrays.asList(
                "Mastering " + base + ": A Complete Guide for Modern Teams",
                "The Future of " + base + ": Trends, Tactics, and Case Studies",
                "10 Proven Ways to Accelerate Your " + base + " Results",
                "Why " + base + " is the Secret Weapon You Are Missing",
                "How to Build a Scalable Framework for " + base
        );

        return GenerateTitlesResponse.builder()
                .titles(titles)
                .build();
    }

    @Override
    public GenerateSeoResponse generateSEO(GenerateSeoRequest request) {
        String title = request.getTitle() != null && !request.getTitle().isBlank() ? request.getTitle() : "Mastering Digital Content";
        String slug = title.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-");

        String metaDesc = request.getExcerpt() != null && !request.getExcerpt().isBlank()
                ? request.getExcerpt()
                : "Discover actionable insights and advanced techniques in " + title + ". Read the full in-depth guide today.";

        if (metaDesc.length() > 160) {
            metaDesc = metaDesc.substring(0, 157) + "...";
        }

        List<String> keywords = Arrays.asList(
                slug.replace("-", " "),
                "content management",
                "best practices",
                "digital strategy",
                "guide"
        );

        return GenerateSeoResponse.builder()
                .seoTitle(title.length() > 60 ? title.substring(0, 57) + "..." : title)
                .metaDescription(metaDesc)
                .keywords(keywords)
                .slugSuggestion(slug)
                .build();
    }

    @Override
    public AnalyzeContentResponse analyze(AnalyzeContentRequest request) {
        String content = request.getContent() != null ? request.getContent() : "";
        int wordCount = content.split("\\s+").length;

        int readabilityScore = Math.min(95, Math.max(65, 75 + (wordCount % 20)));
        String readabilityGrade = readabilityScore > 85 ? "Grade 8 - Very Good (Accessible)" : "Grade 10 - Good";

        int structureScore = content.contains("#") ? 90 : 65;
        String structureDetails = content.contains("#")
                ? "Excellent heading structure with clear hierarchy."
                : "Content lacks markdown headings (# or ##) to break up large paragraphs.";

        int seoScore = wordCount > 200 ? 88 : 60;
        String seoDetails = wordCount > 200
                ? "Good length for search indexing; target topics are well distributed."
                : "Content length is below 200 words, which may impact search visibility.";

        Map<String, Integer> keywords = new HashMap<>();
        String[] words = content.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        for (String w : words) {
            if (w.length() > 4) {
                keywords.put(w, keywords.getOrDefault(w, 0) + 1);
            }
        }
        // Top 5 keywords
        Map<String, Integer> topKeywords = new LinkedHashMap<>();
        keywords.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .forEach(e -> topKeywords.put(e.getKey(), e.getValue()));

        List<String> repetitions = new ArrayList<>();
        if (words.length > 50 && topKeywords.values().stream().anyMatch(v -> v > 8)) {
            repetitions.add("High frequency of top keywords detected; consider using synonyms.");
        }

        List<String> missing = new ArrayList<>();
        if (!content.toLowerCase().contains("conclusion") && !content.toLowerCase().contains("summary")) {
            missing.add("Conclusion / Key Takeaways section");
        }
        if (!content.toLowerCase().contains("example") && !content.toLowerCase().contains("case")) {
            missing.add("Real-world examples or practical demonstrations");
        }

        List<String> recommendations = new ArrayList<>();
        recommendations.add("Keep introductory hook punchy within the first 100 words.");
        if (!content.contains("##")) {
            recommendations.add("Add subheadings (H2, H3) to improve scannability.");
        }
        recommendations.add("Include a clear call-to-action (CTA) at the end of the article.");

        return AnalyzeContentResponse.builder()
                .readabilityScore(readabilityScore)
                .readabilityGrade(readabilityGrade)
                .structureScore(structureScore)
                .structureAnalysis(structureDetails)
                .seoCompletenessScore(seoScore)
                .seoCompleteness(seoDetails)
                .toneConsistency("Consistent and confident tone throughout the article.")
                .keywordUsage(topKeywords)
                .potentialRepetition(repetitions)
                .missingSections(missing)
                .recommendations(recommendations)
                .build();
    }
}
