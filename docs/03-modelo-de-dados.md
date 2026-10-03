# Modelo de dados

## Entidades

### USUARIO
| Campo | Tipo |
|---|---|
| id | uuid (PK) |
| nome | string |
| email | string |
| senha_hash | string |
| criado_em | timestamp |

### CATEGORIA
| Campo | Tipo |
|---|---|
| id | uuid (PK) |
| usuario_id | uuid (FK) |
| nome | string |
| tipo | string (receita / despesa) |

### TRANSACAO
| Campo | Tipo |
|---|---|
| id | uuid (PK) |
| usuario_id | uuid (FK) |
| categoria_id | uuid (FK) |
| importacao_id | uuid (FK, opcional) |
| descricao | string |
| valor | decimal |
| data | date |
| criado_em | timestamp |

### META
| Campo | Tipo |
|---|---|
| id | uuid (PK) |
| usuario_id | uuid (FK) |
| titulo | string |
| valor_alvo | decimal |
| valor_atual | decimal |
| data_limite | date |

### IMPORTACAO
| Campo | Tipo |
|---|---|
| id | uuid (PK) |
| usuario_id | uuid (FK) |
| nome_arquivo | string |
| formato | string (OFX / CSV) |
| importado_em | timestamp |
| total_transacoes | int |

### REGRA_CATEGORIZACAO
| Campo | Tipo |
|---|---|
| id | uuid (PK) |
| usuario_id | uuid (FK) |
| categoria_id | uuid (FK) |
| palavra_chave | string |

## Relacionamentos

- USUARIO 1—N TRANSACAO, CATEGORIA, META, IMPORTACAO, REGRA_CATEGORIZACAO
- CATEGORIA 1—N TRANSACAO, REGRA_CATEGORIZACAO
- IMPORTACAO 1—N TRANSACAO

## Decisões de modelagem

- **uuid como chave primária** em vez de inteiro sequencial — evita expor quantos registros existem e facilita sincronização entre ambientes.
- **CATEGORIA pertence ao usuário** (não é uma tabela global fixa) — cada pessoa cria suas próprias categorias.
- **TRANSACAO não tem campo `tipo` próprio** — o tipo (receita/despesa) é inferido a partir do sinal do `valor` (positivo = receita, negativo = despesa). Isso evita inconsistência entre um campo `tipo` e o valor real, e combina bem com a importação automática, já que extratos bancários já trazem o valor com sinal. `CATEGORIA.tipo` continua existindo pra evitar que uma categoria de despesa seja usada numa receita.
- **META separada de TRANSACAO** — permite acompanhar progresso (`valor_atual` vs `valor_alvo`) sem misturar com o histórico de lançamentos.
- **IMPORTACAO e REGRA_CATEGORIZACAO** — suportam a importação automática de extratos (ver `docs/04-importacao-automatica.md`). Cada transação importada guarda a referência de qual importação a gerou; regras de categorização guardam associações palavra-chave → categoria, criadas conforme o usuário corrige categorizações.
