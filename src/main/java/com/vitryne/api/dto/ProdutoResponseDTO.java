package com.vitryne.api.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record ProdutoResponseDTO(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        BigDecimal precoPromocional,
        BigDecimal precoFinal,
        String tipo,
        String cor,
        Double avaliacao,
        String status,
        List<String> fotosUrls,
        List<TamanhoDisponivelDTO> tamanhosDisponiveis
) {
}
