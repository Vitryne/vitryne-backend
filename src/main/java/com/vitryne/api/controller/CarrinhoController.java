package com.vitryne.api.controller;

import com.vitryne.api.dto.AdicionarItemRequestDTO;
import com.vitryne.api.dto.AtualizarItemRequestDTO;
import com.vitryne.api.dto.CarrinhoResponseDTO;
import com.vitryne.api.service.CarrinhoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/carrinho")
public class CarrinhoController {

    private final CarrinhoService carrinhoService;

    @GetMapping("/{usuarioId}")
    public ResponseEntity<CarrinhoResponseDTO> buscarPorUsuario(@PathVariable Long usuarioId) {
        CarrinhoResponseDTO response = carrinhoService.buscarPorUsuario(usuarioId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{usuarioId}/itens")
    public ResponseEntity<CarrinhoResponseDTO> adicionarItem(@PathVariable Long usuarioId,
                                                             @RequestBody @Valid AdicionarItemRequestDTO request) {
        CarrinhoResponseDTO response = carrinhoService.adicionarItem(usuarioId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{usuarioId}/itens/{itemId}")
    public ResponseEntity<CarrinhoResponseDTO> atualizarQuantidadeItem(@PathVariable Long usuarioId,
                                                                       @PathVariable Long itemId,
                                                                       @RequestBody @Valid AtualizarItemRequestDTO request) {
        CarrinhoResponseDTO response = carrinhoService.atualizarQuantidadeItem(usuarioId, itemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{usuarioId}/itens/{itemId}")
    public ResponseEntity<CarrinhoResponseDTO> removerItem(@PathVariable Long usuarioId,
                                                           @PathVariable Long itemId) {
        CarrinhoResponseDTO response = carrinhoService.removerItem(usuarioId, itemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{usuarioId}/itens")
    public ResponseEntity<CarrinhoResponseDTO> limpar(@PathVariable Long usuarioId) {
        CarrinhoResponseDTO response = carrinhoService.limpar(usuarioId);
        return ResponseEntity.ok(response);
    }
}