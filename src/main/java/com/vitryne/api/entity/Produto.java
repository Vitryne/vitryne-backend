package com.vitryne.api.entity;

import com.vitryne.api.exception.EstoqueInsuficienteException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome do produto é uma informação obrigatória")
    private String nome;

    private String descricao;

    @NotNull(message = "O preco do produto é uma informação obrigatória")
    @Column(precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "preco_promocional", precision = 10, scale = 2)
    private BigDecimal precoPromocional;

    @NotBlank(message = "O tipo do produto é uma informação obrigatória")
    private String tipo;// camiseta/calça/vestido/acessorio/etc.

    private String cor;

    private Double avaliacao;

    @NotBlank(message = "O status do produto é uma informação obrigatória")
    private String status;// ATIVO ou INATIVO

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "fotos_urls", columnDefinition = "text[]")
    private List<String> fotosUrls;

    @Builder.Default
    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Estoque> estoques = new ArrayList<>();

    //relacionar produto com uma Loja futuramente


    public BigDecimal calcularPrecoFinal(){
        return (precoPromocional != null) ? precoPromocional : preco;
    }

    public void aplicarDesconto(Double percentual){
        if(percentual == null || percentual <= 0 || percentual >= 100){
            throw new IllegalArgumentException("Percentual de desconto inválido");
        }

        this.precoPromocional = this.preco * (1 - percentual / 100);
    }

    public void removerDesconto(){
        this.precoPromocional = null;
    }

    public Boolean verificarDisponibilidade(String tamanho){
        return buscarEstoquePorTamanho(tamanho).map(Estoque::estaDisponivel).orElse(false);
    }

    public void reporEstoque(String tamanho, Integer qtd) {
        Estoque estoque = buscarEstoquePorTamanho(tamanho)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tamanho não cadastrado para este produto: " + tamanho));

        estoque.aumentarEstoque(qtd);
    }

    public void darBaixaEstoque(String tamanho, Integer qtd) {
        if (qtd == null || qtd <= 0) {
            throw new IllegalArgumentException("Quantidade para baixa inválida: " + qtd);
        }

        Estoque estoque = buscarEstoquePorTamanho(tamanho)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tamanho não cadastrado para este produto: " + tamanho));

        if (estoque.getQuantidade() < qtd) {
            throw new EstoqueInsuficienteException(tamanho, estoque.getQuantidade(), qtd);
        }

        estoque.diminuirEstoque(qtd);
    }

    public void cadastrarTamanho(String tamanho, Integer quantidadeInicial) {
        if (buscarEstoquePorTamanho(tamanho).isPresent()) {
            throw new IllegalArgumentException("Tamanho já cadastrado: " + tamanho);
        }
        if (quantidadeInicial == null || quantidadeInicial < 0) {
            throw new IllegalArgumentException("Quantidade inválida: " + quantidadeInicial);
        }

        Estoque novo = Estoque.builder()
                .produto(this)
                .tamanho(tamanho)
                .quantidade(quantidadeInicial)
                .build();

        this.estoques.add(novo);
    }

    private java.util.Optional<Estoque> buscarEstoquePorTamanho(String tamanho){
        return estoques.stream().filter(e -> e.getTamanho().equals(tamanho)).findFirst();
    }

}
