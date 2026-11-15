# Listify - Backend 2

API REST feita com Java 17, Spring Boot, Spring Data JPA, MySQL 8 e Flyway.

## Abrir no VS Code
1. Extraia a pasta `backend2`.
2. No VS Code, abra **Arquivo > Abrir Pasta** e selecione `backend2`.
3. Tenha Java 17 (ou superior compatível) e a extensão **Extension Pack for Java** instalados.
4. Abra `src/main/resources/application.properties` e troque `COLOQUE_SUA_SENHA_AQUI` pela senha do seu MySQL.
5. No MySQL, execute uma vez:

```sql
CREATE DATABASE listify CHARACTER SET utf8mb4;
```

6. No terminal integrado do VS Code, dentro da pasta `backend2`, execute:

```bash
mvn spring-boot:run
```

Na primeira inicialização, o Flyway cria as tabelas automaticamente. A API ficará em `http://localhost:8080`.

> Se o comando `mvn` não for reconhecido, instale o Maven e adicione-o ao PATH. Não crie as tabelas manualmente.

## Endpoints

| Método | Caminho | Ação |
|---|---|---|
| GET | `/api/listas` | Listar todas as listas |
| GET | `/api/listas/{id}` | Buscar uma lista e seus itens |
| POST | `/api/listas` | Criar lista |
| PUT | `/api/listas/{id}` | Editar nome da lista |
| DELETE | `/api/listas/{id}` | Excluir lista |
| POST | `/api/listas/{id}/duplicar` | Duplicar lista; todos os itens começam como não comprados |
| POST | `/api/listas/{id}/concluir` | Concluir lista e registrar no histórico |
| GET | `/api/listas/historico` | Consultar histórico de listas concluídas |
| POST | `/api/listas/{id}/itens` | Adicionar item |
| PUT | `/api/listas/{id}/itens/{itemId}` | Editar item ou marcar comprado |
| DELETE | `/api/listas/{id}/itens/{itemId}` | Excluir item |

## Exemplos de JSON

Criar lista (`POST /api/listas`):
```json
{"nome":"Compras do mês"}
```

Adicionar item (`POST /api/listas/1/itens`):
```json
{"nome":"Arroz","quantidade":"5 kg","comprado":false}
```

Marcar item como comprado (`PUT /api/listas/1/itens/1`):
```json
{"nome":"Arroz","quantidade":"5 kg","comprado":true}
```
