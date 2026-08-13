package com.vitryne.api.dto;

public record TamanhoDisponivelDTO(
        Long estoqueId,
        String tamanho,
        Integer quantidade,
        Boolean disponivel
) {
}
