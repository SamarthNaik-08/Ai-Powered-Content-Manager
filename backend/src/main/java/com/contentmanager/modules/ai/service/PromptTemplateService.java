package com.contentmanager.modules.ai.service;

import com.contentmanager.modules.ai.dto.*;
import org.springframework.stereotype.Service;


@Service
public class PromptTemplateService {

    public static final String VERSION = "1.0.0";

    // =========================================================================
    // SYSTEM PROMPTS (Strict JSON instruction)
    // =========================================================================

    public String getSystemPromptForGeneration() {
        return """
            You are an expert AI content creator and digital marketing specialist.
            Always output ONLY valid JSON without any markdown formatting or wrapper codeblocks.
            Schema:
            {
              "title": "A compelling, high-converting title",
              "content": "Full markdown-formatted body with appropriate H2/H3 headings, paragraphs, and lists",
              "excerpt": "A concise 2-sentence summary/meta excerpt",
              "suggestedTags": ["tag1", "tag2", "tag3"]
            }
            """;
    }

    public String getSystemPromptForRewrite() {
        return """
            You are an expert copy editor and content strategist.
            Always output ONLY valid JSON without any markdown formatting or wrapper codeblocks.
            Schema:
            {
              "rewrittenContent": "The rewritten, refined content preserving core message while applying requested tone and style"
            }
            """;
    }

    public String getSystemPromptForSummarize() {
        return """
            You are an expert executive summarizer and information architect.
            Always output ONLY valid JSON without any markdown formatting or wrapper codeblocks.
            Schema:
            {
              "summary": "Clear, concise high-level overview of the content",
              "keyPoints": ["Key takeaway 1", "Key takeaway 2", "Key takeaway 3"]
            }
            """;
    }

    public String getSystemPromptForTitles() {
        return """
            You are a viral headline writer and SEO copywriter.
            Always output ONLY valid JSON without any markdown formatting or wrapper codeblocks.
            Schema:
            {
              "titles": ["Title suggestion 1", "Title suggestion 2", "Title suggestion 3", "Title suggestion 4", "Title suggestion 5"]
            }
            """;
    }

    public String getSystemPromptForSeo() {
        return """
            You are a technical SEO director and search strategist.
            Always output ONLY valid JSON without any markdown formatting or wrapper codeblocks.
            Schema:
            {
              "seoTitle": "High-impact SEO title under 60 characters",
              "metaDescription": "Engaging meta description between 140-160 characters",
              "keywords": ["keyword 1", "keyword 2", "keyword 3", "keyword 4"],
              "slugSuggestion": "url-friendly-slug-with-hyphens"
            }
            """;
    }

    public String getSystemPromptForAnalyze() {
        return """
            You are a senior editorial quality analyst and SEO auditor.
            Always output ONLY valid JSON without any markdown formatting or wrapper codeblocks.
            Schema:
            {
              "readabilityScore": 85,
              "readabilityGrade": "Grade 8 - Very Good (Accessible to general audience)",
              "structureScore": 88,
              "structureAnalysis": "Clear logical flow with balanced heading hierarchy and paragraph lengths",
              "seoCompletenessScore": 80,
              "seoCompleteness": "Key topics covered, strong introductory hook, headings contain target keywords",
              "toneConsistency": "Consistent professional and engaging tone maintained throughout",
              "keywordUsage": { "ai": 5, "content": 7, "management": 4 },
              "potentialRepetition": ["e.g. repeated phrasing in section 2"],
              "missingSections": ["e.g. Actionable Next Steps / FAQ section"],
              "recommendations": ["Recommendation 1", "Recommendation 2"]
            }
            """;
    }

    // =========================================================================
    // USER PROMPT RENDERERS
    // =========================================================================

    public String renderGenerationPrompt(GenerateContentRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate high-quality content on the following topic:\n");
        sb.append("Topic: ").append(request.getTopic()).append("\n");
        if (request.getContentType() != null) {
            sb.append("Content Type: ").append(request.getContentType()).append("\n");
        }
        if (request.getTone() != null) {
            sb.append("Tone: ").append(request.getTone()).append("\n");
        }
        if (request.getLength() != null) {
            sb.append("Length: ").append(request.getLength()).append("\n");
        }
        if (request.getAudience() != null) {
            sb.append("Target Audience: ").append(request.getAudience()).append("\n");
        }
        if (request.getInstructions() != null && !request.getInstructions().isBlank()) {
            sb.append("Special Instructions: ").append(request.getInstructions()).append("\n");
        }
        return sb.toString();
    }

    public String renderRewritePrompt(RewriteContentRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Rewrite the following text according to these guidelines:\n");
        if (request.getTone() != null) {
            sb.append("Desired Tone: ").append(request.getTone()).append("\n");
        }
        if (request.getStyle() != null) {
            sb.append("Desired Style: ").append(request.getStyle()).append("\n");
        }
        if (request.getInstructions() != null && !request.getInstructions().isBlank()) {
            sb.append("Instructions: ").append(request.getInstructions()).append("\n");
        }
        sb.append("\nOriginal Content:\n").append(request.getContent());
        return sb.toString();
    }

    public String renderSummarizePrompt(SummarizeContentRequest request) {
        return "Summarize the following content and extract the most important key points:\n\n" + request.getContent();
    }

    public String renderTitlesPrompt(GenerateTitlesRequest request) {
        int count = request.getCount() != null && request.getCount() > 0 ? request.getCount() : 5;
        return "Generate exactly " + count + " distinct, engaging, SEO-optimized title options for this content or topic:\n\n"
                + request.getTopicOrContent();
    }

    public String renderSeoPrompt(GenerateSeoRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate comprehensive SEO metadata for the following article:\n");
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            sb.append("Title: ").append(request.getTitle()).append("\n");
        }
        if (request.getExcerpt() != null && !request.getExcerpt().isBlank()) {
            sb.append("Excerpt: ").append(request.getExcerpt()).append("\n");
        }
        sb.append("Content:\n").append(request.getContent());
        return sb.toString();
    }

    public String renderAnalyzePrompt(AnalyzeContentRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Perform a thorough editorial and SEO quality audit on the following content:\n");
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            sb.append("Title: ").append(request.getTitle()).append("\n");
        }
        sb.append("Content:\n").append(request.getContent());
        return sb.toString();
    }
}
