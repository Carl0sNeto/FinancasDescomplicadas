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

Extratos de exemplo em `docs/exemplos/` (OFX, CSV e PDF).

## PDF

Para bancos que só exportam PDF, o `PdfParser` (Apache PDFBox) extrai o texto do documento e
procura transações linha a linha. Como cada banco monta o PDF de um jeito, a leitura é por heurística:

- **Linha de transação**: começa com data (`03/09/2026`, `03/09/26`, `03/09`, `03 SET`, `03/SET`) e tem
  um valor brasileiro (`1.234,56`). O texto entre os dois é a descrição. Linhas sem data herdam a data
  da anterior (vários extratos só mostram a data na primeira transação do dia).
- **Saldo**: se houver dois valores na linha, o segundo é o saldo e é ignorado. Linhas de saldo, total,
  limite, vencimento etc. são puladas.
- **Sinal**, nesta ordem:
  1. indicador explícito (`-50,00`, `50,00-`, `50,00 D` = débito; `+50,00`, `50,00 C` = crédito);
  2. fatura de cartão (texto com "fatura" e "vencimento", sem "extrato"): compras vêm sem sinal,
     então o sinal é invertido;
  3. extrato sem indicador: crédito se a descrição tiver "recebido", "salário", "depósito", "estorno"...;
     senão, débito.
- **Ano**: datas sem ano usam o ano da data completa mais recente do documento; meses posteriores ao
  dela são do ano anterior (compra de dezembro numa fatura de janeiro).
- **PDF protegido**: o upload aceita o campo `senha` (muitos bancos usam parte do CPF). A senha não é guardada.
- **Limites**: PDF escaneado (só imagem) não tem texto pra ler; layouts muito diferentes podem não ser
  reconhecidos — a mensagem de erro sugere exportar em OFX/CSV. Conferir a importação e, se precisar,
  desfazê-la.

### Ajuste de esquema

O Hibernate fixa os valores aceitos na coluna `importacao.formato` (restrição CHECK no PostgreSQL, tipo
ENUM no H2) e o `ddl-auto=update` não atualiza isso quando o enum ganha `PDF`. O `AjusteEsquemaBanco`
corrige bancos antigos na inicialização (remove a restrição desatualizada / converte o ENUM em texto).
