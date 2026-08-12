package com.vitryne.api.service;

import com.vitryne.api.dto.AdicionarItemRequestDTO;
import com.vitryne.api.dto.CarrinhoResponseDTO;
import com.vitryne.api.dto.ItemCarrinhoResponseDTO;
import com.vitryne.api.entity.Carrinho;
import com.vitryne.api.entity.Estoque;
import com.vitryne.api.entity.ItemCarrinho;
import com.vitryne.api.exception.*;
import com.vitryne.api.repository.CarrinhoRepository;
import com.vitryne.api.repository.EstoqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CarrinhoService {

    private final CarrinhoRepository carrinhoRepository;
    private final EstoqueRepository estoqueRepository;

    @Transactional(readOnly = true)
    public CarrinhoResponseDTO buscarPorUsuario(Long usuarioId) {
        return toResponseDTO(buscarCarrinhoPorUsuarioId(usuarioId));
    }

    @Transactional
    public CarrinhoResponseDTO adicionarItem(Long usuarioId, AdicionarItemRequestDTO request) {
        validarQuantidade(request.quantidade());
        Estoque estoque = buscarEstoquePorId(usuarioId);
        if (!estoque.estaDisponivel()) {
            throw new EstoqueIndisponivelException(estoque.getTamanho());
        }
        Carrinho carrinho = obterOuCriar(usuarioId);
        ItemCarrinho existente = buscarItemPorEstoqueId(carrinho, request.estoqueId());
        Integer quantidadeFinal = (existente != null)
                ? existente.getQuantidade() + request.quantidade()
                : request.quantidade();
        if (quantidadeFinal > estoque.getQuantidade()) {
            throw new QuantidadeIndisponivelException(
                    estoque.getTamanho(), estoque.getQuantidade(), quantidadeFinal);
        }
        Double precoUnitario = estoque.getProduto().calcularPrecoFinal();
        if (existente != null) {
            existente.setQuantidade(quantidadeFinal);
            existente.setPrecoUnitario(precoUnitario);
        } else {
            ItemCarrinho novo = ItemCarrinho.builder()
                    .carrinho(carrinho)
                    .estoqueId(request.estoqueId())
                    .quantidade(request.quantidade())
                    .precoUnitario(precoUnitario)
                    .build();
            carrinho.getItens().add(novo);
        }
        return persistir(carrinho);
    }

    @Transactional
    public CarrinhoResponseDTO atualizarQuantidadeItem(Long usuarioId, Long itemId, Integer quantidade) {
        validarQuantidade(quantidade);
        Carrinho carrinho = buscarCarrinhoPorUsuarioId(usuarioId);
        ItemCarrinho item = buscarItemPorId(carrinho, itemId);
        Estoque estoque = buscarEstoquePorId(item.getEstoqueId());
        if(quantidade > estoque.getQuantidade()){
            throw new QuantidadeIndisponivelException(estoque.getTamanho(), estoque.getQuantidade(), quantidade);
        }
        item.setQuantidade(quantidade);
        return persistir(carrinho);
    }

    @Transactional
    public CarrinhoResponseDTO removerItem(Long usuarioId, Long itemId) {
        Carrinho carrinho = buscarCarrinhoPorUsuarioId(usuarioId);
        ItemCarrinho item = buscarItemPorId(carrinho, itemId);
        carrinho.getItens().remove(item);
        return persistir(carrinho);
    }

    @Transactional
    public CarrinhoResponseDTO limpar(Long usuarioId) {
        Carrinho carrinho = buscarCarrinhoPorUsuarioId(usuarioId);
        carrinho.getItens().clear();
        return persistir(carrinho);
    }


    private CarrinhoResponseDTO persistir(Carrinho carrinho) {
        carrinho.setPrevisaoValorTotal(calcularTotal(carrinho));
        carrinho.setAtualizadoEm(LocalDateTime.now());
        return toResponseDTO(carrinhoRepository.save(carrinho));
    }

    private Double calcularTotal(Carrinho carrinho) {
        return carrinho.getItens().stream()
                .mapToDouble(this::calcularSubtotal)
                .sum();
    }

    private Double calcularSubtotal(ItemCarrinho item) {
        if (item.getPrecoUnitario() == null || item.getQuantidade() == null) {
            return 0.0;
        }
        return item.getPrecoUnitario() * item.getQuantidade();
    }

    private Carrinho obterOuCriar(Long usuarioId) {
        return carrinhoRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> carrinhoRepository.save(
                        Carrinho.builder()
                                .usuarioId(usuarioId)
                                .previsaoValorTotal(0.0)
                                .atualizadoEm(LocalDateTime.now())
                                .build()
                ));
    }


    private ItemCarrinho buscarItemPorId(Carrinho carrinho, Long itemId) {
        return carrinho.getItens().stream()
                .filter(i -> i.getId() != null && i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ItemCarrinhoNaoEncontradoException(itemId));
    }

    private ItemCarrinho buscarItemPorEstoqueId(Carrinho carrinho, Long estoqueId) {
        return carrinho.getItens().stream()
                .filter(i -> i.getEstoqueId().equals(estoqueId))
                .findFirst()
                .orElse(null);
    }

    private void validarQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new QuantidadeInvalidaException(quantidade);
        }
    }

    private CarrinhoResponseDTO toResponseDTO(Carrinho carrinho) {
        List<ItemCarrinho> itensCarrinho = carrinho.getItens();
        List<Long> estoqueIds = itensCarrinho.stream()
                .map(ItemCarrinho::getEstoqueId)
                .toList();
        Map<Long, Estoque> estoquesPorId = estoqueRepository.findAllById(estoqueIds).stream()
                .collect(Collectors.toMap(Estoque::getId, e -> e));
        List<ItemCarrinhoResponseDTO> itens = itensCarrinho.stream()
                .map(item -> toItemResponseDTO(item, estoquesPorId.get(item.getEstoqueId())))
                .toList();
        return CarrinhoResponseDTO.builder()
                .id(carrinho.getId())
                .usuarioId(carrinho.getUsuarioId())
                .previsaoValorTotal(carrinho.getPrevisaoValorTotal())
                .atualizadoEm(carrinho.getAtualizadoEm())
                .itens(itens)
                .build();
    }

    private ItemCarrinhoResponseDTO toItemResponseDTO(ItemCarrinho item, Estoque estoque) {
        ItemCarrinhoResponseDTO.ItemCarrinhoResponseDTOBuilder builder = ItemCarrinhoResponseDTO.builder()
                .id(item.getId())
                .estoqueId(item.getEstoqueId())
                .quantidade(item.getQuantidade())
                .precoUnitario(item.getPrecoUnitario())
                .subtotal(calcularSubtotal(item));
        if (estoque != null) {
            builder.tamanho(estoque.getTamanho())
                    .produtoId(estoque.getProduto().getId())
                    .nomeProduto(estoque.getProduto().getNome())
                    .fotoUrl(estoque.getProduto().getFotosUrls().isEmpty()
                            ? null
                            : estoque.getProduto().getFotosUrls().get(0));
        }
        return builder.build();
    }

    private Estoque buscarEstoquePorId(Long usuarioId) {
        return estoqueRepository.findById(usuarioId).orElseThrow(() -> new EstoqueNaoEncontradoException(usuarioId));
    }

    private Carrinho buscarCarrinhoPorUsuarioId(Long usuarioId) {
        return carrinhoRepository.findByUsuarioId(usuarioId).orElseThrow(() -> new CarrinhoNaoEncontradoException(usuarioId));
    }
}
