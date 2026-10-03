# Estrutura de pastas do backend

```
financasdescomplicadas/  (backend na raiz; frontend em frontend/)
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/carlos/financasdescomplicadas/
│   │   │   ├── FinancasdescomplicadasApplication.java
│   │   │   │
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java       [feito]
│   │   │   │   └── CorsConfig.java           [feito]
│   │   │   │
│   │   │   ├── security/
│   │   │   │   ├── JwtTokenProvider.java     [feito]
│   │   │   │   ├── JwtAuthFilter.java        [feito]
│   │   │   │   └── UserDetailsServiceImpl.java [feito]
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java       [feito]
│   │   │   │   ├── TransacaoController.java  [feito]
│   │   │   │   ├── CategoriaController.java  [feito]
│   │   │   │   ├── MetaController.java       [feito]
│   │   │   │   └── ImportacaoController.java [feito]
│   │   │   │
│   │   │   ├── service/
│   │   │   │   ├── TransacaoService.java     [feito]
│   │   │   │   ├── CategoriaService.java     [feito]
│   │   │   │   ├── MetaService.java          [feito]
│   │   │   │   ├── ImportacaoService.java    [feito]
│   │   │   │   └── CategorizacaoService.java [feito]
│   │   │   │
│   │   │   ├── parser/
│   │   │   │   ├── ExtratoParser.java        [feito] (interface)
│   │   │   │   ├── OfxParser.java            [feito]
│   │   │   │   └── CsvParser.java            [feito]
│   │   │   │
│   │   │   ├── repository/
│   │   │   │   ├── UsuarioRepository.java              [feito]
│   │   │   │   ├── TransacaoRepository.java            [feito]
│   │   │   │   ├── CategoriaRepository.java             [feito]
│   │   │   │   ├── MetaRepository.java                  [feito]
│   │   │   │   ├── ImportacaoRepository.java             [feito]
│   │   │   │   └── RegraCategorizacaoRepository.java     [feito]
│   │   │   │
│   │   │   ├── model/
│   │   │   │   ├── Usuario.java              [feito]
│   │   │   │   ├── Transacao.java            [feito]
│   │   │   │   ├── Categoria.java            [feito]
│   │   │   │   ├── Meta.java                 [feito]
│   │   │   │   ├── Importacao.java           [feito]
│   │   │   │   └── RegraCategorizacao.java   [feito]
│   │   │   │
│   │   │   ├── dto/
│   │   │   │   ├── LoginRequestDTO.java      [feito]
│   │   │   │   ├── LoginResponseDTO.java     [feito]
│   │   │   │   ├── TransacaoResponseDTO.java [feito]
│   │   │   │   ├── MetaRequestDTO.java       [feito]
│   │   │   │   └── TransacaoBruta.java       [feito] (resultado cru do parser)
│   │   │   │
│   │   │   └── exception/
│   │   │       ├── GlobalExceptionHandler.java        [feito]
│   │   │       └── RecursoNaoEncontradoException.java [feito]
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│       └── java/com/carlos/financasdescomplicadas/
│           ├── service/     (testes unitários com Mockito)
│           └── controller/  (testes de integração)
```
