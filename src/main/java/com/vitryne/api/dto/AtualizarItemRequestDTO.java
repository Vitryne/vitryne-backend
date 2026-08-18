package com.vitryne.api.dto;

import jakarta.validation.constraints.NotNull;

public record AtualizarItemRequestDTO(
        @NotNull
        Integer quantidade
) {
}
