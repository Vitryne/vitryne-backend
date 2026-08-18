package com.vitryne.api.repository;

import com.vitryne.api.entity.Carrinho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CarrinhoRepository extends JpaRepository<Carrinho, Long> {

    @Query(nativeQuery = true, value = """
        select * from carrinho_compras c where c.usuario_id = :usuarioId
    """)
    Optional<Carrinho> findByUsuarioId(@Param("usuarioId") Long usuarioId);
}
