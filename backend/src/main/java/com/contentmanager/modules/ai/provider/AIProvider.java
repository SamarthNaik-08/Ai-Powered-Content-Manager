package com.contentmanager.modules.ai.provider;

import com.contentmanager.modules.ai.dto.*;

public interface AIProvider {

    String getProviderName();

    GenerateContentResponse generate(GenerateContentRequest request);

    RewriteContentResponse rewrite(RewriteContentRequest request);

    SummarizeContentResponse summarize(SummarizeContentRequest request);

    GenerateTitlesResponse generateTitles(GenerateTitlesRequest request);

    GenerateSeoResponse generateSEO(GenerateSeoRequest request);

    AnalyzeContentResponse analyze(AnalyzeContentRequest request);
}
