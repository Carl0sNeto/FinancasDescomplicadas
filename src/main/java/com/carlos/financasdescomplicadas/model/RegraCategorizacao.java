package com.carlos.financasdescomplicadas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "regra_categorizacao")
@Getter
@Setter
@NoArgsConstructor
public class RegraCategorizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    // Guardada já normalizada (minúscula, sem acento) pra comparação direta
    @Column(name = "palavra_chave", nullable = false, length = 150)
    private String palavraChave;

    public RegraCategorizacao(Usuario usuario, Categoria categoria, String palavraChave) {
        this.usuario = usuario;
        this.categoria = categoria;
        this.palavraChave = palavraChave;
    }
}
