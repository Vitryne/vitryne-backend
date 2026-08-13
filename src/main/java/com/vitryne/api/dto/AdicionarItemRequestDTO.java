package com.vitryne.api.dto;

import jakarta.validation.constraints.NotNull;

public record AdicionarItemRequestDTO(
        @NotNull
        Long estoqueId,
        @NotNull
        Integer quantidade
) {
}
