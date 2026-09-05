package com.vitryne.api.service;

import com.vitryne.api.dto.AdicionarItemRequestDTO;
import com.vitryne.api.dto.CarrinhoResponseDTO;
import com.vitryne.api.dto.ItemCarrinhoResponseDTO;
import com.vitryne.api.entity.Carrinho;
import com.vitryne.api.entity.Estoque;
import com.vitryne.api.entity.ItemCarrinho;
import com.vitryne.api.entity.Produto;
import com.vitryne.api.exception.*;
import com.vitryne.api.repository.CarrinhoRepository;
import com.vitryne.api.repository.EstoqueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarrinhoServiceTest {

    @Mock
    private CarrinhoRepository carrinhoRepository;

    @Mock
    private EstoqueRepository estoqueRepository;

    @InjectMocks
    private CarrinhoService carrinhoService;

    private Long usuarioId;
    private Produto produtoPadrao;
    private Estoque estoqueDisponivel;
    private Estoque estoqueSemSaldo;
    private Carrinho carrinhoPadrao;
    private ItemCarrinho itemPadrao;

    @BeforeEach
    void setUp() {
        usuarioId = 1L;

        produtoPadrao = Produto.builder()
                .id(100L)
                .nome("Tênis Esportivo")
                .preco(200.0)
                .precoPromocional(null)
                .fotosUrls(List.of("https://vitryne.com/tenis.jpg"))
                .estoques(new ArrayList<>())
                .build();

        estoqueDisponivel = Estoque.builder()
                .id(10L)
                .produto(produtoPadrao)
                .tamanho("41")
                .quantidade(10)
                .build();

        estoqueSemSaldo = Estoque.builder()
                .id(20L)
                .produto(produtoPadrao)
                .tamanho("42")
                .quantidade(0)
                .build();

        produtoPadrao.getEstoques().add(estoqueDisponivel);
        produtoPadrao.getEstoques().add(estoqueSemSaldo);

        itemPadrao = ItemCarrinho.builder()
                .id(1L)
                .estoqueId(10L)
                .quantidade(2)
                .precoUnitario(200.0)
                .build();

        carrinhoPadrao = Carrinho.builder()
                .id(1L)
                .usuarioId(usuarioId)
                .previsaoValorTotal(400.0)
                .atualizadoEm(LocalDateTime.now())
                .itens(new ArrayList<>(List.of(itemPadrao)))
                .build();

        itemPadrao.setCarrinho(carrinhoPadrao);
    }

    @Nested
    @DisplayName("Testes de Consulta de Carrinho")
    class BuscarCarrinhoTests {

        @Test
        @DisplayName("Deve buscar carrinho do usuário com sucesso e calcular subtotais e detalhes do produto")
        void deveBuscarCarrinhoPorUsuarioComSucesso() {
            when(carrinhoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(carrinhoPadrao));
            when(estoqueRepository.findAllById(List.of(10L))).thenReturn(List.of(estoqueDisponivel));

            CarrinhoResponseDTO response = carrinhoService.buscarPorUsuario(usuarioId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.usuarioId()).isEqualTo(usuarioId);
            assertThat(response.previsaoValorTotal()).isEqualTo(400.0);
            assertThat(response.itens()).hasSize(1);

            ItemCarrinhoResponseDTO itemDTO = response.itens().get(0);
            assertThat(itemDTO.id()).isEqualTo(1L);
            assertThat(itemDTO.estoqueId()).isEqualTo(10L);
            assertThat(itemDTO.quantidade()).isEqualTo(2);
            assertThat(itemDTO.precoUnitario()).isEqualTo(200.0);
            assertThat(itemDTO.subtotal()).isEqualTo(400.0);
            assertThat(itemDTO.tamanho()).isEqualTo("41");
            assertThat(itemDTO.produtoId()).isEqualTo(100L);
            assertThat(itemDTO.nomeProduto()).isEqualTo("Tênis Esportivo");
            assertThat(itemDTO.fotoUrl()).isEqualTo("https://vitryne.com/tenis.jpg");

            verify(carrinhoRepository, times(1)).findByUsuarioId(usuarioId);
            verify(estoqueRepository, times(1)).findAllById(List.of(10L));
        }

        @Test
        @DisplayName("Deve lançar CarrinhoNaoEncontradoException quando não existir carrinho para o usuário")
        void deveLancarExcecaoAoBuscarCarrinhoInexistente() {
            when(carrinhoRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> carrinhoService.buscarPorUsuario(99L))
                    .isInstanceOf(CarrinhoNaoEncontradoException.class)
                    .hasMessageContaining("99");

            verify(carrinhoRepository, times(1)).findByUsuarioId(99L);
        }
    }

    @Nested
    @DisplayName("Testes de Adição de Item ao Carrinho")
    class AdicionarItemTests {

        @Test
        @DisplayName("Deve adicionar novo item em carrinho existente e atualizar previsão total")
        void deveAdicionarItemEmCarrinhoExistenteComSucesso() {
            AdicionarItemRequestDTO request = new AdicionarItemRequestDTO(10L, 3);

            Carrinho carrinhoVazio = Carrinho.builder()
                    .id(2L)
                    .usuarioId(usuarioId)
                    .previsaoValorTotal(0.0)
                    .atualizadoEm(LocalDateTime.now())
                    .itens(new ArrayList<>())
                    .build();

            when(estoqueRepository.findById(10L)).thenReturn(Optional.of(estoqueDisponivel));
            when(carrinhoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(carrinhoVazio));
            when(carrinhoRepository.save(any(Carrinho.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(estoqueRepository.findAllById(List.of(10L))).thenReturn(List.of(estoqueDisponivel));

            CarrinhoResponseDTO response = carrinhoService.adicionarItem(usuarioId, request);

            assertThat(response).isNotNull();
            assertThat(response.itens()).hasSize(1);
            assertThat(response.itens().get(0).quantidade()).isEqualTo(3);
            assertThat(response.itens().get(0).subtotal()).isEqualTo(600.0);
            assertThat(response.previsaoValorTotal()).isEqualTo(600.0);

            verify(carrinhoRepository, times(1)).save(carrinhoVazio);
        }

        @Test
        @DisplayName("Deve criar um novo carrinho automaticamente e adicionar o item quando usuário não possui carrinho")
        void deveCriarNovoCarrinhoEAdicionarItemQuandoNaoExistir() {
            AdicionarItemRequestDTO request = new AdicionarItemRequestDTO(10L, 2);

            when(estoqueRepository.findById(10L)).thenReturn(Optional.of(estoqueDisponivel));
            when(carrinhoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());
            when(carrinhoRepository.save(any(Carrinho.class))).thenAnswer(invocation -> {
                Carrinho c = invocation.getArgument(0);
                if (c.getId() == null) c.setId(10L);
                return c;
            });
            when(estoqueRepository.findAllById(List.of(10L))).thenReturn(List.of(estoqueDisponivel));

            CarrinhoResponseDTO response = carrinhoService.adicionarItem(usuarioId, request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.itens()).hasSize(1);
            assertThat(response.previsaoValorTotal()).isEqualTo(400.0);

            verify(carrinhoRepository, times(2)).save(any(Carrinho.class));
        }

        @Test
        @DisplayName("Deve incrementar a quantidade de item já existente no carrinho e atualizar totais")
        void deveIncrementarQuantidadeDeItemJaExistenteNoCarrinho() {
            AdicionarItemRequestDTO request = new AdicionarItemRequestDTO(10L, 3);

            when(estoqueRepository.findById(10L)).thenReturn(Optional.of(estoqueDisponivel));
            when(carrinhoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(carrinhoPadrao));
            when(carrinhoRepository.save(any(Carrinho.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(estoqueRepository.findAllById(List.of(10L))).thenReturn(List.of(estoqueDisponivel));

            CarrinhoResponseDTO response = carrinhoService.adicionarItem(usuarioId, request);

            assertThat(response).isNotNull();
            assertThat(response.itens()).hasSize(1);
            assertThat(response.itens().get(0).quantidade()).isEqualTo(5); // 2 existente + 3 novo
            assertThat(response.itens().get(0).subtotal()).isEqualTo(1000.0);
            assertThat(response.previsaoValorTotal()).isEqualTo(1000.0);

            verify(carrinhoRepository, times(1)).save(carrinhoPadrao);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(ints = {0, -1, -5})
        @DisplayName("Deve lançar IllegalArgumentException quando a quantidade for nula ou menor/igual a zero")
        void deveLancarExcecaoAoAdicionarComQuantidadeInvalida(Integer quantidadeInvalida) {
            AdicionarItemRequestDTO request = new AdicionarItemRequestDTO(10L, quantidadeInvalida);

            assertThatThrownBy(() -> carrinhoService.adicionarItem(usuarioId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantidade inválida");

            verifyNoInteractions(estoqueRepository);
            verifyNoInteractions(carrinhoRepository);
        }

        @Test
        @DisplayName("Deve lançar EstoqueNaoEncontradoException quando estoque não for localizado")
        void deveLancarExcecaoAoAdicionarItemComEstoqueInexistente() {
            AdicionarItemRequestDTO request = new AdicionarItemRequestDTO(999L, 1);
            when(estoqueRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> carrinhoService.adicionarItem(usuarioId, request))
                    .isInstanceOf(EstoqueNaoEncontradoException.class)
                    .hasMessageContaining("999");

            verify(estoqueRepository, times(1)).findById(999L);
        }

        @Test
        @DisplayName("Deve lançar EstoqueIndisponivelException quando estoque estiver zerado")
        void deveLancarExcecaoAoAdicionarItemQuandoEstoqueEstiverZerado() {
            AdicionarItemRequestDTO request = new AdicionarItemRequestDTO(20L, 1);
            when(estoqueRepository.findById(20L)).thenReturn(Optional.of(estoqueSemSaldo));

            assertThatThrownBy(() -> carrinhoService.adicionarItem(usuarioId, request))
                    .isInstanceOf(EstoqueIndisponivelException.class)
                    .hasMessageContaining("42");

            verify(estoqueRepository, times(1)).findById(20L);
        }

        @Test
        @DisplayName("Deve lançar QuantidadeIndisponivelException quando quantidade solicitada for maior que o estoque total")
        void deveLancarExcecaoAoAdicionarQuantidadeAcimaDoEstoque() {
            AdicionarItemRequestDTO request = new AdicionarItemRequestDTO(10L, 15); // Estoque disponível é 10

            Carrinho carrinhoVazio = Carrinho.builder()
                    .id(1L)
                    .usuarioId(usuarioId)
                    .itens(new ArrayList<>())
                    .build();

            when(estoqueRepository.findById(10L)).thenReturn(Optional.of(estoqueDisponivel));
            when(carrinhoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(carrinhoVazio));

            assertThatThrownBy(() -> carrinhoService.adicionarItem(usuarioId, request))
                    .isInstanceOf(QuantidadeIndisponivelException.class)
                    .hasMessageContaining("41");

            verify(estoqueRepository, times(1)).findById(10L);
        }

        @Test
        @DisplayName("Deve lançar QuantidadeIndisponivelException quando a soma com o item já existente ultrapassa o estoque")
        void deveLancarExcecaoQuandoSomaDeQuantidadeExistenteUltrapassaEstoque() {
            // Carrinho já possui 2 itens do estoque 10 (disponível = 10). Tentar adicionar mais 9 itens resulta em 11 > 10.
            AdicionarItemRequestDTO request = new AdicionarItemRequestDTO(10L, 9);

            when(estoqueRepository.findById(10L)).thenReturn(Optional.of(estoqueDisponivel));
            when(carrinhoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(carrinhoPadrao));

            assertThatThrownBy(() -> carrinhoService.adicionarItem(usuarioId, request))
                    .isInstanceOf(QuantidadeIndisponivelException.class)
                    .hasMessageContaining("41");

            verify(estoqueRepository, times(1)).findById(10L);
        }
    }

    @Nested
    @DisplayName("Testes de Atualização de Quantidade de Item")
    class AtualizarQuantidadeTests {

        @Test
        @DisplayName("Deve atualizar quantidade do item com sucesso e recalcular o total do carrinho")
        void deveAtualizarQuantidadeDeItemComSucesso() {
            when(carrinhoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(carrinhoPadrao));
            when(estoqueRepository.findById(10L)).thenReturn(Optional.of(estoqueDisponivel));
            when(carrinhoRepository.save(any(Carrinho.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(estoqueRepository.findAllById(List.of(10L))).thenReturn(List.of(estoqueDisponivel));

            CarrinhoResponseDTO response = carrinhoService.atualizarQuantidadeItem(usuarioId, 1L, 4);

            assertThat(response).isNotNull();
            assertThat(response.itens().get(0).quantidade()).isEqualTo(4);
            assertThat(response.itens().get(0).subtotal()).isEqualTo(800.0);
            assertThat(response.previsaoValorTotal()).isEqualTo(800.0);

            verify(carrinhoRepository, times(1)).save(carrinhoPadrao);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(ints = {0, -1, -10})
        @DisplayName("Deve lançar IllegalArgumentException ao atualizar com quantidade nula ou negativa/zero")
        void deveLancarExcecaoAoAtualizarComQuantidadeInvalida(Integer quantidadeInvalida) {
            assertThatThrownBy(() -> carrinhoService.atualizarQuantidadeItem(usuarioId, 1L, quantidadeInvalida))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantidade inválida");

            verifyNoInteractions(carrinhoRepository);
            verifyNoInteractions(estoqueRepository);
        }

        @Test
        @DisplayName("Deve lançar CarrinhoNaoEncontradoException ao atualizar item de carrinho inexistente")
        void deveLancarExcecaoAoAtualizarItemEmCarrinhoInexistente() {
            when(carrinhoRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> carrinhoService.atualizarQuantidadeItem(99L, 1L, 3))
                    .isInstanceOf(CarrinhoNaoEncontradoException.class)
                    .hasMessageContaining("99");

            verify(carrinhoRepository, times(1)).findByUsuarioId(99L);
        }

        @Test
        @DisplayName("Deve lançar ItemCarrinhoNaoEncontradoException quando item não existir no carrinho")
        void deveLancarExcecaoAoAtualizarItemInexistenteNoCarrinho() {
            when(carrinhoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(carrinhoPadrao));

            assertThatThrownBy(() -> carrinhoService.atualizarQuantidadeItem(usuarioId, 999L, 3))
                    .isInstanceOf(ItemCarrinhoNaoEncontradoException.class)
                    .hasMessageContaining("999");

            verify(carrinhoRepository, times(1)).findByUsuarioId(usuarioId);
        }

        @Test
        @DisplayName("Deve lançar EstoqueNaoEncontradoException quando estoque vinculado ao item não for encontrado")
        void deveLancarExcecaoAoAtualizarItemComEstoqueInexistente() {
            when(carrinhoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(carrinhoPadrao));
            when(estoqueRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> carrinhoService.atualizarQuantidadeItem(usuarioId, 1L, 3))
                    .isInstanceOf(EstoqueNaoEncontradoException.class)
                    .hasMessageContaining("10");

            verify(estoqueRepository, times(1)).findById(10L);
        }

        @Test
        @DisplayName("Deve lançar QuantidadeIndisponivelException quando nova quantidade superar o estoque")
        void deveLancarExcecaoAoAtualizarQuantidadeSuperiorAoEstoque() {
            when(carrinhoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(carrinhoPadrao));
            when(estoqueRepository.findById(10L)).thenReturn(Optional.of(estoqueDisponivel)); // max 10

            assertThatThrownBy(() -> carrinhoService.atualizarQuantidadeItem(usuarioId, 1L, 11))
                    .isInstanceOf(QuantidadeIndisponivelException.class)
                    .hasMessageContaining("41");

            verify(estoqueRepository, times(1)).findById(10L);
        }
    }

    @Nested
    @DisplayName("Testes de Remoção de Item do Carrinho")
    class RemoverItemTests {

        @Test
        @DisplayName("Deve remover item com sucesso e recalcular total do carrinho")
        void deveRemoverItemComSucesso() {
            when(carrinhoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(carrinhoPadrao));
            when(carrinhoRepository.save(any(Carrinho.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(estoqueRepository.findAllById(List.of())).thenReturn(List.of());

            CarrinhoResponseDTO response = carrinhoService.removerItem(usuarioId, 1L);

            assertThat(response).isNotNull();
            assertThat(response.itens()).isEmpty();
            assertThat(response.previsaoValorTotal()).isEqualTo(0.0);

            verify(carrinhoRepository, times(1)).save(carrinhoPadrao);
        }

        @Test
        @DisplayName("Deve lançar CarrinhoNaoEncontradoException ao tentar remover item de carrinho inexistente")
        void deveLancarExcecaoAoRemoverDeCarrinhoInexistente() {
            when(carrinhoRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> carrinhoService.removerItem(99L, 1L))
                    .isInstanceOf(CarrinhoNaoEncontradoException.class)
                    .hasMessageContaining("99");

            verify(carrinhoRepository, times(1)).findByUsuarioId(99L);
        }

        @Test
        @DisplayName("Deve lançar ItemCarrinhoNaoEncontradoException ao remover item inexistente no carrinho")
        void deveLancarExcecaoAoRemoverItemInexistente() {
            when(carrinhoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(carrinhoPadrao));

            assertThatThrownBy(() -> carrinhoService.removerItem(usuarioId, 999L))
                    .isInstanceOf(ItemCarrinhoNaoEncontradoException.class)
                    .hasMessageContaining("999");

            verify(carrinhoRepository, times(1)).findByUsuarioId(usuarioId);
        }
    }

    @Nested
    @DisplayName("Testes de Limpeza de Carrinho")
    class LimparCarrinhoTests {

        @Test
        @DisplayName("Deve limpar todos os itens do carrinho e zerar a previsão de valor total")
        void deveLimparCarrinhoComSucesso() {
            when(carrinhoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(carrinhoPadrao));
            when(carrinhoRepository.save(any(Carrinho.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(estoqueRepository.findAllById(List.of())).thenReturn(List.of());

            CarrinhoResponseDTO response = carrinhoService.limpar(usuarioId);

            assertThat(response).isNotNull();
            assertThat(response.itens()).isEmpty();
            assertThat(response.previsaoValorTotal()).isEqualTo(0.0);

            verify(carrinhoRepository, times(1)).save(carrinhoPadrao);
        }

        @Test
        @DisplayName("Deve lançar CarrinhoNaoEncontradoException ao limpar carrinho inexistente")
        void deveLancarExcecaoAoLimparCarrinhoInexistente() {
            when(carrinhoRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> carrinhoService.limpar(99L))
                    .isInstanceOf(CarrinhoNaoEncontradoException.class)
                    .hasMessageContaining("99");

            verify(carrinhoRepository, times(1)).findByUsuarioId(99L);
        }
    }

    @Nested
    @DisplayName("Testes de Validação de Cálculo de Subtotal e Valor Total")
    class CalculoTotaisTests {

        @Test
        @DisplayName("Deve calcular subtotal e total com múltiplos itens e valores unitários distintos")
        void deveCalcularSubtotalETotalComMultiplosItensCorretamente() {
            Estoque outroEstoque = Estoque.builder()
                    .id(30L)
                    .produto(Produto.builder()
                            .id(200L)
                            .nome("Meia")
                            .preco(25.0)
                            .fotosUrls(List.of("https://vitryne.com/meia.jpg"))
                            .build())
                    .tamanho("UN")
                    .quantidade(50)
                    .build();

            ItemCarrinho item2 = ItemCarrinho.builder()
                    .id(2L)
                    .carrinho(carrinhoPadrao)
                    .estoqueId(30L)
                    .quantidade(4)
                    .precoUnitario(25.0)
                    .build();

            carrinhoPadrao.getItens().add(item2);

            when(carrinhoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(carrinhoPadrao));
            when(estoqueRepository.findAllById(List.of(10L, 30L))).thenReturn(List.of(estoqueDisponivel, outroEstoque));

            CarrinhoResponseDTO response = carrinhoService.buscarPorUsuario(usuarioId);

            assertThat(response.itens()).hasSize(2);
            // Item 1: 2 * 200.0 = 400.0
            assertThat(response.itens().get(0).subtotal()).isEqualTo(400.0);
            // Item 2: 4 * 25.0 = 100.0
            assertThat(response.itens().get(1).subtotal()).isEqualTo(100.0);
        }
    }
}
