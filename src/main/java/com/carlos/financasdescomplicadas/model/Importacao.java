package com.carlos.financasdescomplicadas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "importacao")
@Getter
@Setter
@NoArgsConstructor
public class Importacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private FormatoExtrato formato;

    @CreationTimestamp
    @Column(name = "importado_em", nullable = false, updatable = false)
    private LocalDateTime importadoEm;

    @Column(name = "total_transacoes", nullable = false)
    private int totalTransacoes;
}
