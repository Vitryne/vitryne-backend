package com.vitryne.api.service;

import com.vitryne.api.dto.ProdutoResponseDTO;
import com.vitryne.api.dto.TamanhoDisponivelDTO;
import com.vitryne.api.entity.Estoque;
import com.vitryne.api.entity.Produto;
import com.vitryne.api.exception.ProdutoNaoEncontradoException;
import com.vitryne.api.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private ProdutoService produtoService;

    private Produto produtoPadrao;
    private Estoque estoqueM;
    private Estoque estoqueG;

    @BeforeEach
    void setUp() {
        estoqueM = Estoque.builder()
                .id(10L)
                .tamanho("M")
                .quantidade(5)
                .build();

        estoqueG = Estoque.builder()
                .id(11L)
                .tamanho("G")
                .quantidade(0)
                .build();

        List<Estoque> estoques = new ArrayList<>(List.of(estoqueM, estoqueG));

        produtoPadrao = Produto.builder()
                .id(1L)
                .nome("Camiseta Básica")
                .descricao("Camiseta 100% algodão")
                .preco(100.0)
                .precoPromocional(null)
                .tipo("Camiseta")
                .cor("Azul")
                .avaliacao(4.5)
                .status("ATIVO")
                .fotosUrls(List.of("https://vitryne.com/fotos/camiseta-azul.jpg"))
                .estoques(estoques)
                .build();

        estoqueM.setProduto(produtoPadrao);
        estoqueG.setProduto(produtoPadrao);
    }

    @Nested
    @DisplayName("Testes de Listagem de Produtos")
    class ListarProdutosTests {

        @Test
        @DisplayName("Deve retornar lista de ProdutoResponseDTO quando existirem produtos")
        void deveListarProdutosComSucesso() {
            when(produtoRepository.findAll()).thenReturn(List.of(produtoPadrao));

            List<ProdutoResponseDTO> resultado = produtoService.listarProdutos();

            assertThat(resultado).isNotNull().hasSize(1);
            ProdutoResponseDTO dto = resultado.get(0);
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.nome()).isEqualTo("Camiseta Básica");
            assertThat(dto.preco()).isEqualTo(100.0);
            assertThat(dto.precoFinal()).isEqualTo(100.0);
            assertThat(dto.tamanhosDisponiveis()).hasSize(2);

            verify(produtoRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não existirem produtos cadastrados")
        void deveRetornarListaVaziaQuandoNaoExistiremProdutos() {
            when(produtoRepository.findAll()).thenReturn(List.of());

            List<ProdutoResponseDTO> resultado = produtoService.listarProdutos();

            assertThat(resultado).isNotNull().isEmpty();
            verify(produtoRepository, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("Testes de Busca de Produto por ID")
    class BuscarPorIdTests {

        @Test
        @DisplayName("Deve buscar produto por ID com sucesso e mapear todos os campos corretamente")
        void deveBuscarProdutoPorIdComSucesso() {
            when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoPadrao));

            ProdutoResponseDTO resultado = produtoService.buscarPorId(1L);

            assertThat(resultado).isNotNull();
            assertThat(resultado.id()).isEqualTo(1L);
            assertThat(resultado.nome()).isEqualTo("Camiseta Básica");
            assertThat(resultado.descricao()).isEqualTo("Camiseta 100% algodão");
            assertThat(resultado.tipo()).isEqualTo("Camiseta");
            assertThat(resultado.cor()).isEqualTo("Azul");
            assertThat(resultado.avaliacao()).isEqualTo(4.5);
            assertThat(resultado.status()).isEqualTo("ATIVO");
            assertThat(resultado.fotosUrls()).containsExactly("https://vitryne.com/fotos/camiseta-azul.jpg");
            assertThat(resultado.tamanhosDisponiveis()).hasSize(2);

            TamanhoDisponivelDTO tamanhoM = resultado.tamanhosDisponiveis().get(0);
            assertThat(tamanhoM.estoqueId()).isEqualTo(10L);
            assertThat(tamanhoM.tamanho()).isEqualTo("M");
            assertThat(tamanhoM.quantidade()).isEqualTo(5);
            assertThat(tamanhoM.disponivel()).isTrue();

            TamanhoDisponivelDTO tamanhoG = resultado.tamanhosDisponiveis().get(1);
            assertThat(tamanhoG.estoqueId()).isEqualTo(11L);
            assertThat(tamanhoG.tamanho()).isEqualTo("G");
            assertThat(tamanhoG.quantidade()).isEqualTo(0);
            assertThat(tamanhoG.disponivel()).isFalse();

            verify(produtoRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Deve calcular preço final igual ao preço original quando não há preço promocional")
        void deveCalcularPrecoFinalSemPrecoPromocional() {
            produtoPadrao.setPreco(150.0);
            produtoPadrao.setPrecoPromocional(null);

            when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoPadrao));

            ProdutoResponseDTO resultado = produtoService.buscarPorId(1L);

            assertThat(resultado.preco()).isEqualTo(150.0);
            assertThat(resultado.precoPromocional()).isNull();
            assertThat(resultado.precoFinal()).isEqualTo(150.0);
        }

        @Test
        @DisplayName("Deve calcular preço final igual ao preço promocional quando este estiver preenchido")
        void deveCalcularPrecoFinalComPrecoPromocional() {
            produtoPadrao.setPreco(150.0);
            produtoPadrao.setPrecoPromocional(120.0);

            when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoPadrao));

            ProdutoResponseDTO resultado = produtoService.buscarPorId(1L);

            assertThat(resultado.preco()).isEqualTo(150.0);
            assertThat(resultado.precoPromocional()).isEqualTo(120.0);
            assertThat(resultado.precoFinal()).isEqualTo(120.0);
        }

        @Test
        @DisplayName("Deve lançar ProdutoNaoEncontradoException quando produto não existir")
        void deveLancarExcecaoAoBuscarProdutoInexistente() {
            when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> produtoService.buscarPorId(99L))
                    .isInstanceOf(ProdutoNaoEncontradoException.class)
                    .hasMessageContaining("99");

            verify(produtoRepository, times(1)).findById(99L);
        }
    }

    @Nested
    @DisplayName("Testes de Verificação de Disponibilidade por Tamanho")
    class VerificarDisponibilidadeTests {

        @Test
        @DisplayName("Deve retornar true quando tamanho existe e possui quantidade maior que zero")
        void deveRetornarTrueQuandoTamanhoExisteETemEstoque() {
            when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoPadrao));

            Boolean disponivel = produtoService.verificarDisponibilidade(1L, "M");

            assertThat(disponivel).isTrue();
            verify(produtoRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Deve retornar false quando tamanho existe mas quantidade é zero")
        void deveRetornarFalseQuandoTamanhoExisteMasQuantidadeEhZero() {
            when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoPadrao));

            Boolean disponivel = produtoService.verificarDisponibilidade(1L, "G");

            assertThat(disponivel).isFalse();
            verify(produtoRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Deve retornar false quando tamanho não está cadastrado no produto")
        void deveRetornarFalseQuandoTamanhoNaoCadastrado() {
            when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoPadrao));

            Boolean disponivel = produtoService.verificarDisponibilidade(1L, "GG");

            assertThat(disponivel).isFalse();
            verify(produtoRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Deve lançar ProdutoNaoEncontradoException ao verificar disponibilidade de produto inexistente")
        void deveLancarExcecaoAoVerificarDisponibilidadeDeProdutoInexistente() {
            when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> produtoService.verificarDisponibilidade(99L, "M"))
                    .isInstanceOf(ProdutoNaoEncontradoException.class)
                    .hasMessageContaining("99");

            verify(produtoRepository, times(1)).findById(99L);
        }
    }
}
