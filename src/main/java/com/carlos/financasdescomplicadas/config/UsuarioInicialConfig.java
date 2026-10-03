package com.carlos.financasdescomplicadas.config;

import com.carlos.financasdescomplicadas.model.Usuario;
import com.carlos.financasdescomplicadas.repository.UsuarioRepository;
import com.carlos.financasdescomplicadas.service.CategoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Não existe cadastro público (ver docs/05-seguranca-autenticacao.md). Pra não precisar
 * gerar hash BCrypt na mão, a conta pode ser criada na subida da aplicação a partir das
 * propriedades app.usuario-inicial.* (ou das variáveis USUARIO_INICIAL_*).
 * Se o email já existir, nada é feito.
 */
@Configuration
public class UsuarioInicialConfig {

    private static final Logger log = LoggerFactory.getLogger(UsuarioInicialConfig.class);

    @Bean
    public ApplicationRunner criarUsuarioInicial(
            @Value("${app.usuario-inicial.nome:}") String nome,
            @Value("${app.usuario-inicial.email:}") String email,
            @Value("${app.usuario-inicial.senha:}") String senha,
            UsuarioRepository usuarioRepository,
            CategoriaService categoriaService,
            PasswordEncoder passwordEncoder,
            TransactionTemplate transactionTemplate) {

        return args -> {
            if (email.isBlank() || senha.isBlank()) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> {
                if (usuarioRepository.existsByEmail(email)) {
                    return;
                }
                String nomeUsuario = nome.isBlank() ? email : nome;
                Usuario usuario = usuarioRepository.save(
                        new Usuario(nomeUsuario, email, passwordEncoder.encode(senha)));
                categoriaService.criarCategoriasPadrao(usuario);
                log.info("Usuário inicial criado: {}", email);
            });
        };
    }
}
