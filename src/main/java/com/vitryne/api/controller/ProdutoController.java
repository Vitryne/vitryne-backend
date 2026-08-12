package com.vitryne.api.controller;

import com.vitryne.api.dto.ProdutoResponseDTO;
import com.vitryne.api.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/produto")
public class ProdutoController {

    private final ProdutoService produtoService;

    @GetMapping
    public ResponseEntity<List<ProdutoResponseDTO>> listarProdutos(){
        log.info("Recebida requisição para listar todos os produtos");

        List<ProdutoResponseDTO> produtos = produtoService.listarProdutos();

        log.info("Retornando {} produtos encontrados", produtos.size());
        return ResponseEntity.ok(produtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(@PathVariable Long id){
        log.info("Recebida requisição para buscar produto com ID: {}", id);

        ProdutoResponseDTO produto = produtoService.buscarPorId(id);

        log.info("Produto com ID: {} retornado com sucesso", id);
        return ResponseEntity.ok(produto);
    }
}