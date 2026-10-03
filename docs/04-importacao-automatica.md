# Importação automática de extrato

## Objetivo

O usuário faz upload do extrato bancário (OFX ou CSV) e o sistema cria as transações automaticamente, tentando categorizá-las sozinho.

## Fluxo

1. Upload do extrato (arquivo OFX ou CSV)
2. `ImportacaoService` identifica o formato e delega pro parser certo
3. O parser extrai uma lista de transações brutas (data, descrição, valor)
4. `CategorizacaoService` compara a descrição de cada transação com as `REGRA_CATEGORIZACAO` salvas do usuário
   - **Regra encontrada** → transação criada já com a categoria associada
   - **Sem correspondência** → transação criada sem categoria, fica pendente de revisão manual
5. Usuário revisa as transações pendentes; toda correção manual vira uma nova `REGRA_CATEGORIZACAO`, melhorando a próxima importação

## Parsers — padrão Strategy

```java
public interface ExtratoParser {
    boolean suporta(String nomeArquivo);
    List<TransacaoBruta> parse(InputStream arquivo);
}
```

- `OfxParser implements ExtratoParser` — OFX é um formato semi-estruturado padronizado que os bancos oferecem pra softwares de contabilidade. Existe lib pronta em Java (`ofx4j`) pra facilitar o parsing.
- `CsvParser implements ExtratoParser` — cada banco exporta CSV num layout diferente. Começar suportando o layout do Nubank (data, título, valor) — é simples, previsível e comum entre o público jovem. Suporte a outros bancos (ou mapeamento manual de colunas) fica como incremento futuro.

`ImportacaoService` decide qual parser usar (pela extensão do arquivo ou farejando o conteúdo), chama o parser certo e repassa o resultado pro `CategorizacaoService`.

## Status

Implementado, com alguns ajustes em relação ao desenho acima:

- `ExtratoParser` ganhou `formato()`, usado pra preencher `IMPORTACAO.formato`.
- `OfxParser` lê os blocos `<STMTTRN>` diretamente, sem a ofx4j (aceita OFX 1.x SGML e 2.x XML, UTF-8 ou Windows-1252).
- `CsvParser` identifica as colunas pelo cabeçalho: fatura do cartão Nubank (`date,title,amount`, sinal invertido)
  e extrato da conta Nubank (`Data,Valor,Identificador,Descrição`); aceita `,` ou `;` e valores `1.234,56`.
- Palavras-chave são comparadas sem acento e sem diferenciar maiúsculas; se várias batem, vence a mais longa.
  Regras cuja categoria não combina com o sinal do valor são ignoradas.
- Ao categorizar manualmente com "lembrar", a regra nova também é aplicada às outras transações pendentes.
- `DELETE /importacoes/{id}` desfaz uma importação (apaga as transações que ela criou).

Extratos de exemplo em `docs/exemplos/`.
