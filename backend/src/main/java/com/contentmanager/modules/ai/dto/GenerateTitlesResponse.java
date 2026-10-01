package com.contentmanager.modules.ai.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateTitlesResponse {

    @Builder.Default
    private List<String> titles = new ArrayList<>();
}
