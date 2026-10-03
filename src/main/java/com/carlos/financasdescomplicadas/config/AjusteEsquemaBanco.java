package com.carlos.financasdescomplicadas.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ajustes de esquema que o ddl-auto=update não faz sozinho.
 *
 * O Hibernate fixa os valores aceitos por colunas de enum: no PostgreSQL com uma restrição
 * CHECK (formato IN ('OFX','CSV')) e no H2 com um tipo ENUM nativo, e não atualiza nenhum
 * dos dois quando o enum ganha um valor novo. Bancos criados antes do formato PDF recusariam
 * importações de PDF; aqui a restrição antiga é removida e o tipo ENUM vira texto.
 *
 * Quando o projeto adotar Flyway (docs/08-proximos-passos.md), isso vira uma migração.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AjusteEsquemaBanco implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AjusteEsquemaBanco.class);

    private static final String RESTRICOES_DA_TABELA = """
            select tc.constraint_name, cc.check_clause
            from information_schema.table_constraints tc
            join information_schema.check_constraints cc
              on cc.constraint_name = tc.constraint_name and cc.constraint_schema = tc.constraint_schema
            where tc.constraint_type = 'CHECK'
              and lower(tc.table_name) = 'importacao'
              and lower(tc.table_schema) = lower(?)
            """;

    private static final String TIPO_DA_COLUNA_FORMATO = """
            select data_type from information_schema.columns
            where lower(table_name) = 'importacao' and lower(column_name) = 'formato'
              and lower(table_schema) = lower(?)
            """;

    private final DataSource dataSource;

    public AjusteEsquemaBanco(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conexao = dataSource.getConnection()) {
            converterColunaEnumEmTexto(conexao);
            for (String restricao : restricoesDeFormatoDesatualizadas(conexao)) {
                try (Statement st = conexao.createStatement()) {
                    st.execute("alter table importacao drop constraint \"" + restricao.replace("\"", "") + "\"");
                }
                log.info("Restrição antiga de importacao.formato removida: {}", restricao);
            }
        } catch (SQLException e) {
            // Não impede a aplicação de subir; só a importação de PDF falharia
            log.warn("Não foi possível verificar as restrições da tabela importacao", e);
        }
    }

    /**
     * No H2 o Hibernate cria a coluna com tipo ENUM nativo (e um PostgreSQL pode ter um tipo
     * enum próprio, "USER-DEFINED"): o tipo fixa os valores aceitos. Converte pra texto,
     * como a coluna já é no PostgreSQL criado pelo Hibernate.
     */
    private static void converterColunaEnumEmTexto(Connection conexao) throws SQLException {
        String tipo = null;
        try (PreparedStatement ps = conexao.prepareStatement(TIPO_DA_COLUNA_FORMATO)) {
            ps.setString(1, conexao.getSchema());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    tipo = rs.getString(1).toUpperCase(Locale.ROOT);
                }
            }
        }
        if ("ENUM".equals(tipo) || "USER-DEFINED".equals(tipo)) {
            try (Statement st = conexao.createStatement()) {
                st.execute("alter table importacao alter column formato set data type varchar(5)");
            }
            log.info("Coluna importacao.formato convertida de {} para varchar", tipo);
        }
    }

    // Restrições que listam os formatos aceitos (citam 'OFX') mas ainda não incluem 'PDF'
    private static List<String> restricoesDeFormatoDesatualizadas(Connection conexao) throws SQLException {
        List<String> nomes = new ArrayList<>();
        try (PreparedStatement ps = conexao.prepareStatement(RESTRICOES_DA_TABELA)) {
            ps.setString(1, conexao.getSchema());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String clausula = rs.getString(2).toUpperCase(Locale.ROOT);
                    if (clausula.contains("'OFX'") && !clausula.contains("'PDF'")) {
                        nomes.add(rs.getString(1));
                    }
                }
            }
        }
        return nomes;
    }
}
