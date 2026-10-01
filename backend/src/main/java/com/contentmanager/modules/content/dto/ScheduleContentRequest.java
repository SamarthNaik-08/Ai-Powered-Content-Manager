package com.contentmanager.modules.content.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleContentRequest {

    @NotNull(message = "scheduledAt cannot be null")
    @Future(message = "scheduledAt must be a date/time in the future")
    private Instant scheduledAt;
}
