package com.guardrail.dto;

import com.guardrail.entity.AuthorType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LikeRequest {
    @NotNull
    private Long authorId;
    @NotNull
    private AuthorType authorType;
}
