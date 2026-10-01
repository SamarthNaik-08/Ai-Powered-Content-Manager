package com.contentmanager.modules.ai.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewriteContentResponse {

    private String rewrittenContent;
}
