package com.vitryne.api.service;

import com.vitryne.api.dto.ProdutoResponseDTO;
import com.vitryne.api.dto.TamanhoDisponivelDTO;
import com.vitryne.api.entity.Produto;
import com.vitryne.api.exception.ProdutoNaoEncontradoException;
import com.vitryne.api.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public List<ProdutoResponseDTO> listarProdutos(){
        return produtoRepository.findAll().stream().map(this::toResponseDTO).toList();
    };

    public ProdutoResponseDTO buscarPorId(Long id){
        Produto produto = buscarProduto(id);
        return toResponseDTO(produto);
    }

    public Boolean verificarDisponibilidade(Long produtoId, String tamanho){
        Produto produto = buscarProduto(produtoId);
        return produto.verificarDisponibilidade(tamanho);
    }

    private ProdutoResponseDTO toResponseDTO(Produto produto){
        List<TamanhoDisponivelDTO> tamanhos = produto.getEstoques().stream()
                .map(estoque -> new TamanhoDisponivelDTO(
                        estoque.getId(),
                        estoque.getTamanho(),
                        estoque.getQuantidade(),
                        produto.verificarDisponibilidade(estoque.getTamanho())
                ))
                .toList();

        return  ProdutoResponseDTO.builder().id(produto.getId()).avaliacao(produto.getAvaliacao()).cor(produto.getCor()).preco(produto.getPreco()).nome(produto.getNome())
                .tipo(produto.getTipo()).descricao(produto.getDescricao()).precoPromocional(produto.getPrecoPromocional())
                .precoFinal(produto.calcularPrecoFinal()).status(produto.getStatus()).fotosUrls(produto.getFotosUrls()).tamanhosDisponiveis(tamanhos).build();
    }

    private Produto buscarProduto(Long id){
        return produtoRepository.findById(id).orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    //Metodos de manipulacao que serao implementados quando o Lojista for modelado
}
