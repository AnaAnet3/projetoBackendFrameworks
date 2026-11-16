#### projetoBackendFrameworks
# Listify

Aplicação web responsiva para criar e gerenciar listas de compras, feita com
Spring Boot e uma interface em HTML/CSS/JS puro servida pelo próprio backend.

## Funcionalidades
- CRUD de listas e de itens, agrupados por categoria
- Marcar itens como comprados e ver o total (com orçamento)
- Catálogo de produtos e categorias
- Duplicar listas, salvar como modelo e criar a partir de modelo
- Concluir listas e consultar o histórico
- Exportar a lista como texto (e PDF via impressão do navegador)

## Tecnologias
Java 17 · Spring Boot 4.1.2 · Spring Web MVC · Spring Data JPA ·
Bean Validation · Flyway · MySQL · HTML/CSS/JS

## Estrutura
`controller` → `service` → `repository` → `model`, com `dto` para entrada/saída
e `exception` com tratamento global de erros. Migrations em `resources/db/migration`.

## Como executar
1. Tenha Java 17+ e MySQL em execução.
2. Em `listify/src/main/resources/application.properties`, informe seu usuário
   e senha do MySQL (o banco `listify` é criado automaticamente).
3. Na pasta `listify`, rode:
   ```
   ./mvnw spring-boot:run
   ```
4. Acesse http://localhost:8080

## Principais endpoints
| Recurso | Rotas |
|---|---|
| Listas | `GET/POST /listas`, `GET/PUT/DELETE /listas/{id}`, `GET /listas/historico` |
| Ações da lista | `POST /listas/{id}/duplicar`, `/salvar-modelo`, `/usar-modelo`, `/concluir` |
| Itens | `GET/POST /listas/{id}/itens`, `PATCH/DELETE /listas/{id}/itens/{itemId}`, `PATCH .../comprado`, `GET /listas/{id}/total` |
| Catálogo | `GET /produtos`, `GET /categorias` |
| Exportação | `GET /listas/{id}/exportar/texto` |
