# API de Tarefas (Spring Boot + H2)

Rodar: `mvn spring-boot:run` — Testes: `mvn test` — Console H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:testdb`, user `sa`, senha vazia)

| Método | Endpoint | Sucesso | Erro |
|---|---|---|---|
| POST | /api/tarefas | 201 | 400 |
| GET | /api/tarefas | 200 | - |
| GET | /api/tarefas/{id} | 200 | 404 |
| PUT | /api/tarefas/{id} | 200 | 400 / 404 |
| DELETE | /api/tarefas/{id} | 204 | 400 / 404 |

Exemplo: `{"titulo":"Estudar Mockito","descricao":"opcional","status":"PENDENTE"}`
Para concluir (PUT): `{"titulo":"...","status":"CONCLUIDA","dataConclusao":"2026-09-28T10:00:00"}`
