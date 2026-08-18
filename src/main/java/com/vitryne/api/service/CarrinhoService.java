package com.vitryne.api.service;

import com.vitryne.api.dto.AdicionarItemRequestDTO;
import com.vitryne.api.dto.AtualizarItemRequestDTO;
import com.vitryne.api.dto.CarrinhoResponseDTO;
import com.vitryne.api.dto.ItemCarrinhoResponseDTO;
import com.vitryne.api.entity.Carrinho;
import com.vitryne.api.entity.Estoque;
import com.vitryne.api.entity.ItemCarrinho;
import com.vitryne.api.exception.*;
import com.vitryne.api.repository.CarrinhoRepository;
import com.vitryne.api.repository.EstoqueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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
        Estoque estoque = buscarEstoquePorId(request.estoqueId());
        if (!estoque.estaDisponivel()) {
            log.warn("Estoque indisponível. tamanho do estoque: [{}]", estoque.getQuantidade());
            throw new EstoqueIndisponivelException(estoque.getTamanho());
        }
        Carrinho carrinho = obterOuCriar(usuarioId);
        ItemCarrinho existente = buscarItemPorEstoqueId(carrinho, request.estoqueId());
        Integer quantidadeFinal = (existente != null)
                ? existente.getQuantidade() + request.quantidade()
                : request.quantidade();
        if (quantidadeFinal > estoque.getQuantidade()) {
            logErroEstoqueDisponivel(estoque, quantidadeFinal);
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
            log.info("Item adicionado no carrinho com sucesso.");
        }
        return persistir(carrinho);
    }

    @Transactional
    public CarrinhoResponseDTO atualizarQuantidadeItem(Long usuarioId, Long itemId, AtualizarItemRequestDTO request) {
        Integer quantidade = request.quantidade();
        validarQuantidade(quantidade);
        Carrinho carrinho = buscarCarrinhoPorUsuarioId(usuarioId);
        ItemCarrinho item = buscarItemPorId(carrinho, itemId);
        Estoque estoque = buscarEstoquePorId(item.getEstoqueId());
        if(quantidade > estoque.getQuantidade()){
            logErroEstoqueDisponivel(estoque, quantidade);
            throw new QuantidadeIndisponivelException(estoque.getTamanho(), estoque.getQuantidade(), quantidade);
        }
        item.setQuantidade(quantidade);
        log.info("Quantidade atualizada com sucesso [{}]", quantidade);
        return persistir(carrinho);
    }

    @Transactional
    public CarrinhoResponseDTO removerItem(Long usuarioId, Long itemId) {
        Carrinho carrinho = buscarCarrinhoPorUsuarioId(usuarioId);
        ItemCarrinho item = buscarItemPorId(carrinho, itemId);
        carrinho.getItens().remove(item);
        log.info("Item removido com sucesso: [{}]", item.getId());
        return persistir(carrinho);
    }

    @Transactional
    public CarrinhoResponseDTO limpar(Long usuarioId) {
        Carrinho carrinho = buscarCarrinhoPorUsuarioId(usuarioId);
        carrinho.getItens().clear();
        log.info("Carrinho limpo com sucesso: [{}]", carrinho.getId());
        return persistir(carrinho);
    }


    private CarrinhoResponseDTO persistir(Carrinho carrinho) {
        log.info("Iniciando persistência do carrinho: [{}]", carrinho.getId());
        try{
            carrinho.setPrevisaoValorTotal(calcularTotal(carrinho));
            carrinho.setAtualizadoEm(LocalDateTime.now());
            Carrinho carrinhoSalvo = carrinhoRepository.save(carrinho);
            log.info("Carrinho persistido com sucesso: [{}]", carrinhoSalvo.getId());
            return toResponseDTO(carrinhoSalvo);
        } catch (Exception e) {
            log.error("Erro ao persistir carrinho: [{}]",
                carrinho.getId(), e
            );
            throw e;
        }

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
        log.info("Obtendo carrinho do usuário: [{}]", usuarioId);
        return carrinhoRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    log.info("Carrinho inexistente para o usuário: [{}], criando um novo.", usuarioId);
                    Carrinho carrinho = Carrinho.builder()
                            .usuarioId(usuarioId)
                            .previsaoValorTotal(0.0)
                            .atualizadoEm(LocalDateTime.now())
                            .build();
                    Carrinho carrinhoSalvo = carrinhoRepository.save(carrinho);
                    log.info("Carrinho criado. Usuário: [{}], Carrinho: [{}]",
                            usuarioId, carrinho.getId()
                    );
                    return carrinhoSalvo;
                });
    }


    private ItemCarrinho buscarItemPorId(Carrinho carrinho, Long itemId) {
        log.info("Buscando item por id: [{}]", itemId);
        ItemCarrinho itemCarrinho = carrinho.getItens().stream()
                .filter(i -> i.getId() != null && i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Erro ao buscar item por id: [{}], Carrinho: [{}]", itemId, carrinho.getId());
                    return new ItemCarrinhoNaoEncontradoException(itemId);
                });
        return itemCarrinho;
    }

    private ItemCarrinho buscarItemPorEstoqueId(Carrinho carrinho, Long estoqueId) {
        log.info("Buscando item no carrinho pelo estoqueId: [{}]", estoqueId);
        ItemCarrinho itemCarrinho = carrinho.getItens().stream()
                .filter(i -> i.getEstoqueId().equals(estoqueId))
                .findFirst()
                .orElse(null);
        if(itemCarrinho == null){
            log.warn("Item não econtrado no carrinho para o estoqueId: [{}]", estoqueId);
        }
        return itemCarrinho;
    }

    private void validarQuantidade(Integer quantidade) {
        log.info("Validando quantidade de produtos: [{}]", quantidade);
        if (quantidade == null || quantidade <= 0) {
            log.warn("Quantidade de produtos inválida: [{}]", quantidade);
            throw new QuantidadeInvalidaException(quantidade);
        }
        log.info("Quantidade de produtos correta: [{}]", quantidade);
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

    private Estoque buscarEstoquePorId(Long estoqueId) {
        log.info("Buscando estoque por id: [{}]", estoqueId);
        Estoque estoque = estoqueRepository.findById(estoqueId).orElseThrow(() -> {
            log.warn("Erro ao buscar estoque por id: [{}]", estoqueId);
            return new EstoqueNaoEncontradoException(estoqueId);
        });
        log.info("Estoque econtrado com sucesso. id: [{}]", estoqueId);
        return estoque;
    }

    private Carrinho buscarCarrinhoPorUsuarioId(Long usuarioId) {
        log.info("Buscando carrinho por usuário: [{}]", usuarioId);
        Carrinho carrinho = carrinhoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> {
                    log.warn("Erro ao buscar carrinho por usuário: [{}]", usuarioId);
                    return new CarrinhoNaoEncontradoException(usuarioId);
                });
        log.info("Carrinho econtado com sucesso. Usuário: [{}], Carrinho: [{}]", usuarioId, carrinho.getId());
        return carrinho;
    }

    private static void logErroEstoqueDisponivel(Estoque estoque, Integer quantidadeFinal) {
        log.warn("Quantidade solicitada [{}] excede o estoque disponível [{}] para o tamanho [{}]",
                estoque.getTamanho(), estoque.getQuantidade(), quantidadeFinal
        );
    }
}
