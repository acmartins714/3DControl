# 3D Control — Sistema de Controle de Vendas

Backend do **3D Control**, sistema que centraliza a operação de uma pequena empresa de produtos
personalizados sob encomenda: pedidos vindos de qualquer canal (WhatsApp, Instagram, Facebook, telefone,
site e balcão) passam a seguir **um fluxo único e rastreável**, integrando vendas, estoque de insumos,
produção e financeiro.

> **UNIESP — Sistemas para Internet · Tecnologia para Back-end Avançado · 2026.2**
> Professor: Jonas Bernadino de Lima
> Grupo: Alexandre Chaves Martins · Hrasam Hussein Gomes Monteiro · Luana Silva · Victor Melquiades Leite

---

## Sumário

1. [Problema e escopo da primeira versão](#1-problema-e-escopo-da-primeira-versão)
2. [Arquitetura escolhida e justificativa](#2-arquitetura-escolhida-e-justificativa)
3. [Microsserviços](#3-microsserviços)
4. [Organização do código (Package by Feature + Arquitetura Limpa)](#4-organização-do-código-package-by-feature--arquitetura-limpa)
5. [Fluxos principais](#5-fluxos-principais)
6. [Diagrama de classes UML](#6-diagrama-de-classes-uml)
7. [Tecnologias](#7-tecnologias)
8. [Como executar](#8-como-executar)
9. [Endpoints](#9-endpoints)
10. [Limitações conhecidas e próximos passos](#10-limitações-conhecidas-e-próximos-passos)

---

## 1. Problema e escopo da primeira versão

A proposta comercial identificou cinco causas-raiz de prejuízo. Cada uma tem uma resposta direta no sistema:

| Problema diagnosticado | Resposta no 3D Control | Onde está no código |
|---|---|---|
| Dados espalhados em celulares, cadernos e planilhas | Cadastros únicos de clientes, produtos e insumos; todo pedido registrado num só lugar, com o canal de origem e o atendente | `cadastro-service`, `pedido-service` |
| Alterações do cliente não chegam à produção | **Regra de Ouro**: o pedido pode ser alterado até entrar em produção e é bloqueado depois disso. A produção nunca guarda uma cópia das especificações: consulta sempre o pedido | `Pedido.garantirAlteravel()`, `StatusPedido.permiteAlteracao()` |
| Produção parada por falta de insumo / compras em excesso | Cada pedido reserva os insumos da ficha técnica do produto. Se faltar algo, nada é reservado, o pedido fica *Aguardando insumo* e a administração recebe uma **solicitação de compra** antes do início da fabricação | `ReservarInsumosUseCase`, `SolicitacaoCompra` |
| Prazos prometidos sem visibilidade / ninguém sabe o status | Fila de produção ordenada por disponibilidade de insumo e proximidade do prazo; **rastreio** por código com a linha do tempo completa | `FilaProducao`, `RastrearPedidoUseCase` |
| Entrega sem conferir o pagamento / sem visão de lucro | Conta a receber por pedido (sinal, parcelas, quitação). **A entrega é bloqueada enquanto houver saldo.** Lucratividade calculada por categoria de produto | `ContaReceber`, `Pedido.confirmarEntrega()`, `AnaliseLucratividade` |

**Fora da primeira versão** (planejado, ver [§10](#10-limitações-conhecidas-e-próximos-passos)): integração
automatizada com a API do WhatsApp (o endpoint de rastreio já está pronto para ela), o front-end, a
autenticação de funcionários e o cálculo de capacidade produtiva por dia.

---

## 2. Arquitetura escolhida e justificativa

O projeto combina três decisões complementares:

```
 Microsserviços          → como o SISTEMA é dividido     (um serviço por área de negócio)
 Package by Feature      → como cada SERVIÇO é dividido  (um pacote por funcionalidade)
 Arquitetura Limpa       → como cada FEATURE é dividida  (domain / application / infrastructure)
```

### 2.1 Microsserviços

O sistema é dividido em **cinco serviços de negócio + um API Gateway**. Cada serviço corresponde a um
*bounded context* (contexto delimitado, no vocabulário do DDD) e cada contexto, a um dos módulos da
proposta comercial.

**Por que microsserviços neste caso:**

- **O problema é de integração entre áreas.** A empresa perde dinheiro na passagem de informação entre
  atendimento, produção, estoque e financeiro. Separar o sistema exatamente nessas fronteiras deixa as
  integrações **explícitas e contratuais** (APIs REST versionáveis), em vez de acoplamentos escondidos
  dentro de um monólito.
- **Fonte única de verdade por assunto.** Cada serviço é **dono exclusivo dos seus dados**
  (*database per service*): só o `pedido-service` altera pedidos, só o `financeiro-service` decide se um
  pedido está quitado. Isso elimina a causa do problema original: várias cópias divergentes da mesma
  informação.
- **Evolução e implantação independentes.** O módulo financeiro (relatórios de lucratividade) e o canal
  automatizado (site e WhatsApp) tendem a mudar em ritmos diferentes. Cada serviço pode ser alterado,
  testado e publicado sem redeploy dos demais.
- **Escala seletiva.** Em datas comemorativas o volume de pedidos e consultas de rastreio cresce muito
  mais que o de lançamentos financeiros. Só os serviços sob pressão precisam de mais instâncias.
- **Isolamento de falhas.** Uma instabilidade no relatório financeiro não impede a produção de consultar
  a fila.
- **Objetivo pedagógico da disciplina** (Back-end Avançado): exercitar comunicação entre serviços,
  gateway, consistência distribuída e conteinerização.

**Cuidados para manter a solução simples** (premissa do proprietário: orçamento de pequena empresa):

- Granularidade **grossa**: cinco serviços, um por módulo da proposta, e não um por entidade.
- **Um único PostgreSQL** com **um banco por serviço** (`cadastro_db`, `pedido_db`, ...): mantém o
  isolamento lógico sem custo de várias instâncias. Se for preciso, um banco pode migrar para outro
  servidor sem mudar código.
- Comunicação **síncrona via REST** (`RestClient`), sem broker de mensagens nesta versão. Menos
  infraestrutura para operar; a evolução para eventos está prevista (§10).
- Tudo sobe com **um comando** (`docker compose up`).

**Comunicação e consistência.** O `pedido-service` atua como **orquestrador** do fluxo do pedido. Ao
registrar um pedido, ele valida o cliente e os produtos, reserva insumos, inclui a ordem na fila e abre
a conta a receber. Se uma etapa falhar depois de efeitos em outros serviços, ele executa
**compensações** (libera a reserva, cancela a ordem e a conta), no padrão *Saga orquestrada*.
Referências entre serviços são feitas **apenas por identificador (UUID)**; não há chave estrangeira entre
bancos.

**API Gateway.** O `api-gateway` (Spring Cloud Gateway) é o ponto único de entrada (`:8080/api/**`) para
o front-end, o site e o bot do WhatsApp. Os clientes não precisam conhecer o endereço de cada serviço.
Autenticação, *rate limiting* e CORS ficam centralizados num só lugar quando forem adicionados.

### 2.2 Package by Feature

Dentro de cada serviço, o código é agrupado **por funcionalidade de negócio** (`cliente`, `produto`,
`insumo`, `pedido`, `rastreio`, `estoque`, `solicitacaocompra`, `ordemproducao`, `contareceber`,
`lucratividade`), e **não** por tipo técnico (`controllers/`, `services/`, `repositories/`).

**Por quê:**

- **Alta coesão:** tudo o que diz respeito a "pedido" está em `pedido/`. Para entender ou alterar uma
  regra, abre-se um único pacote.
- **Baixo acoplamento e encapsulamento:** classes de infraestrutura são *package-private* (adaptadores
  JPA, controllers, DTOs). Outra feature só enxerga o que é deliberadamente público (domínio e casos de
  uso).
- **O código "grita" o negócio** (*Screaming Architecture*): a estrutura de pastas comunica o que o
  sistema faz.
- **Caminho natural para extração:** se uma feature crescer, ela já é uma unidade isolada e pode virar um
  novo microsserviço.

### 2.3 Arquitetura Limpa (Clean Architecture)

Cada feature é dividida em três camadas, com a **regra da dependência** apontando sempre para dentro:

```
 infrastructure  ──►  application  ──►  domain
 (@RestController,    (@Service,         (entidades, regras,
  @Entity, HTTP)       @Transactional,    exceções — Java puro)
                       portas)
```

| Camada | Conteúdo | Depende de |
|---|---|---|
| `domain` | Entidades e agregados (`Pedido`, `ContaReceber`, `ItemEstoque`, ...), objetos de valor (`record`s), serviços de domínio (`FilaProducao`, `AnaliseLucratividade`), exceções de negócio e **portas de persistência** (`PedidoRepository`) | nada (Java puro) |
| `application` | **Casos de uso** `@Service` + `@Transactional` (`RegistrarPedidoUseCase`, `ReservarInsumosUseCase`, ...), comandos de entrada e **portas de saída** para outros serviços (`EstoqueGateway`, `FinanceiroGateway`, ...) | `domain` (+ anotações do Spring) |
| `infrastructure` | Adaptadores: `web` (`@RestController` e DTOs), `persistence` (`@Entity` e Spring Data), `client` (`RestClient` para outros serviços) | `application`, `domain`, frameworks |

**Por quê:**

- **As regras de negócio críticas ficam protegidas e testáveis.** A Regra de Ouro, a validação de
  quitação na entrega e a reserva de insumos estão em classes Java puras, testadas com JUnit **sem subir
  Spring nem banco** (`mvn test` roda em segundos).
- **Domínio independente de framework e de banco.** As entidades e regras não conhecem JPA, HTTP nem
  Spring.
- **Aplicação pragmática.** Os casos de uso usam `@Service` e `@Transactional`, como é idiomático no
  Spring, o que dispensa configuração manual de beans. Cada caso de uso é uma unidade de trabalho
  atômica e as consultas usam `@Transactional(readOnly = true)`. Essa é a única concessão ao framework
  fora da infraestrutura: os casos de uso continuam dependendo apenas de **interfaces** (portas), nunca
  de JPA ou de HTTP.
- **Inversão de dependência (o "D" do SOLID) nas integrações.** O `RegistrarPedidoUseCase` depende da
  interface `EstoqueGateway`, e não do HTTP. Trocar REST por mensageria no futuro significa escrever um
  novo adaptador, sem tocar em nenhuma regra de negócio.
- **Entidade de domínio ≠ entidade JPA.** O mapeamento fica no adaptador (`PedidoJpa.from()` /
  `toDomain()`), evitando que anotações e restrições do ORM "vazem" para as regras.

### 2.4 Decisões complementares

| Decisão | Motivo |
|---|---|
| **PostgreSQL** + **Flyway** | Banco relacional robusto e gratuito. O Flyway versiona o esquema (`db/migration/V1__*.sql`), e o Hibernate apenas **valida** (`ddl-auto: validate`) |
| **UUID** gerado no domínio | A identidade nasce no domínio, sem depender do banco; IDs não colidem entre serviços |
| **Controle otimista** (`@Version`) em `Pedido` e `ItemEstoque` | Impede a corrida "atendente altera enquanto a produção inicia" e duas reservas simultâneas do mesmo saldo (HTTP 409) |
| **Timeouts nos clientes HTTP** (`spring.http.clients`: conexão 2 s, leitura 10 s) | Quando um serviço cai, quem o chama falha rápido (503) em vez de ficar preso no timeout do sistema operacional |
| **Problem Details (RFC 9457)** | Erros padronizados: 404 (não encontrado), 422 (regra de negócio), 409 (concorrência), 503 (serviço dependente indisponível) |
| **Sem módulo "common" compartilhado** | Microsserviços não compartilham código de domínio. As poucas classes utilitárias (`DomainException`, handler) são duplicadas de propósito para manter a autonomia de cada serviço |
| **Snapshot de preço/custo no item do pedido** | Mudanças futuras no catálogo não alteram pedidos já fechados nem distorcem a análise de lucro |

---

## 3. Microsserviços

| Serviço | Porta | Banco | Features | Responsabilidade |
|---|---|---|---|---|
| `api-gateway` | 8080 | — | — | Ponto único de entrada `/api/**` |
| `cadastro-service` | 8081 | `cadastro_db` | `cliente`, `produto`, `insumo` | Dados mestre e ficha técnica (insumos por produto) |
| `pedido-service` | 8082 | `pedido_db` | `pedido`, `rastreio` | Ciclo de vida do pedido, Regra de Ouro, orquestração, rastreio |
| `estoque-service` | 8083 | `estoque_db` | `estoque`, `solicitacaocompra` | Saldos, reservas por pedido, movimentações, demandas de compra |
| `producao-service` | 8084 | `producao_db` | `ordemproducao` | Fila priorizada, início/conclusão, retrabalho |
| `financeiro-service` | 8085 | `financeiro_db` | `contareceber`, `lucratividade` | Sinal/parcelas/quitação, liberação da entrega, margem por categoria |

```mermaid
flowchart LR
    canais["Atendimento · Site · WhatsApp"] --> gw["api-gateway :8080"]
    gw --> cad["cadastro-service"]
    gw --> ped["pedido-service"]
    gw --> est["estoque-service"]
    gw --> pro["producao-service"]
    gw --> fin["financeiro-service"]
    ped -- "valida cliente/produto" --> cad
    ped -- "reserva/consome insumos" --> est
    ped -- "planeja ordem" --> pro
    ped -- "abre conta / consulta quitação" --> fin
    pro -- "início/conclusão" --> ped
    cad --- db1[(cadastro_db)]
    ped --- db2[(pedido_db)]
    est --- db3[(estoque_db)]
    pro --- db4[(producao_db)]
    fin --- db5[(financeiro_db)]
```

---

## 4. Organização do código (Package by Feature + Arquitetura Limpa)

```
3DControl/
├── pom.xml                         # POM agregador (Spring Boot 4.1, Java 21)
├── docker-compose.yml              # PostgreSQL + 5 serviços + gateway
├── Dockerfile                      # imagem parametrizada por módulo
├── infra/postgres/init/            # cria um banco por serviço
├── docs/uml/                       # diagrama de classes (Mermaid + PlantUML)
├── api-gateway/
└── services/
    ├── cadastro-service/
    ├── estoque-service/
    ├── financeiro-service/
    ├── producao-service/
    └── pedido-service/
        └── src/main/java/br/edu/uniesp/threedcontrol/pedidos/
            ├── PedidoServiceApplication.java
            ├── pedido/                         ◄── FEATURE
            │   ├── domain/                     ◄── regras (Java puro)
            │   │   ├── Pedido.java             (raiz do agregado)
            │   │   ├── ItemPedido.java, HistoricoStatus.java
            │   │   ├── StatusPedido.java       (máquina de estados)
            │   │   ├── PedidoNaoAlteravelException.java
            │   │   └── PedidoRepository.java   (porta)
            │   ├── application/                ◄── casos de uso (@Service)
            │   │   ├── RegistrarPedidoUseCase.java
            │   │   ├── AlterarPedidoUseCase.java
            │   │   ├── MovimentarPedidoUseCase.java
            │   │   └── port/                   (CadastroGateway, EstoqueGateway, ...)
            │   └── infrastructure/             ◄── adaptadores
            │       ├── web/                    (PedidoController + DTOs)
            │       ├── persistence/            (PedidoJpa + Spring Data)
            │       └── client/                 (RestClient p/ outros serviços)
            ├── rastreio/                       ◄── FEATURE
            └── shared/                         (exceções base, tratamento de erros HTTP)
```

---

## 5. Fluxos principais

### Registro de pedido (qualquer canal)

```mermaid
sequenceDiagram
    actor A as Atendente / Site
    participant P as pedido-service
    participant C as cadastro-service
    participant E as estoque-service
    participant O as producao-service
    participant F as financeiro-service
    A->>P: POST /pedidos
    P->>C: GET /clientes/{id}, GET /produtos/{id}
    C-->>P: cliente, preço, custo, ficha técnica
    P->>E: POST /estoque/reservas (insumos × quantidade)
    alt saldo suficiente
        E-->>P: reservado = true → EM_TRIAGEM
    else falta insumo
        E-->>P: reservado = false + faltantes (abre SolicitacaoCompra) → AGUARDANDO_INSUMO
    end
    P->>O: POST /producao/ordens (entra na fila)
    P->>F: POST /financeiro/contas (conta a receber)
    P-->>A: 201 Created (código 3DC-AAMMDD-XXXX)
```

### Produção → entrega

1. A equipe fabril consulta `GET /producao/fila` (insumos disponíveis primeiro; depois urgência do prazo).
2. `POST /producao/ordens/{id}/inicio` → o pedido vai para **EM_PRODUCAO** (bloqueado para alterações) e os
   insumos reservados recebem baixa definitiva no estoque.
3. `POST /producao/ordens/{id}/conclusao` → pedido **FINALIZADO**.
4. `POST /pedidos/{id}/despacho` (se for entrega) → **EM_ROTA_ENTREGA**.
5. `POST /pedidos/{id}/entrega` → o pedido-service consulta o financeiro; **só conclui se a conta estiver
   QUITADA**. Caso contrário, responde 422 informando o saldo remanescente.

---

## 6. Diagrama de classes UML

O diagrama completo, renderizado diretamente pelo GitHub, está em
**[docs/uml/diagrama-classes.md](docs/uml/diagrama-classes.md)**. Ele contém o modelo de domínio de
todos os serviços, as camadas da Arquitetura Limpa no `pedido-service` e a máquina de estados do pedido.
A fonte PlantUML equivalente está em [docs/uml/diagrama-classes.puml](docs/uml/diagrama-classes.puml).

Visão resumida das principais classes de domínio e suas relações:

```mermaid
classDiagram
    direction LR
    class Cliente
    class Produto
    class Insumo
    class Pedido {
        <<aggregate root>>
        +alterar()
        +iniciarProducao()
        +confirmarEntrega(quitado, saldo)
    }
    class ItemPedido
    class ItemEstoque {
        <<aggregate root>>
        +reservar(q)
        +consumirReserva(q)
    }
    class SolicitacaoCompra
    class OrdemProducao {
        <<aggregate root>>
        +iniciar(responsavel)
        +concluir()
    }
    class ContaReceber {
        <<aggregate root>>
        +registrarPagamento(valor, forma)
        +liberadaParaEntrega()
    }
    class Pagamento
    Produto "1" *-- "0..*" Insumo : composição
    Pedido "1" *-- "1..*" ItemPedido
    Pedido ..> Cliente : clienteId
    ItemPedido ..> Produto : produtoId
    ItemEstoque ..> Insumo : insumoId
    SolicitacaoCompra ..> Pedido : pedidoId
    OrdemProducao ..> Pedido : pedidoId
    ContaReceber ..> Pedido : pedidoId
    ContaReceber "1" *-- "0..*" Pagamento
```

---

## 7. Tecnologias

- **Java 21** (LTS) · **Spring Boot 4.1** (Web MVC, Data JPA, Validation, RestClient, Flyway)
- **Spring Cloud Gateway (Server Web MVC)**, no `api-gateway` (Spring Boot 4.0 + Spring Cloud 2025.1, a
  linha homologada do Spring Cloud)
- **PostgreSQL 17** · **Flyway** · **Hibernate 7**
- **springdoc-openapi 3** (Swagger UI em cada serviço)
- **JUnit 5 + AssertJ** (testes de domínio) · **Maven** (wrapper incluso) · **Docker Compose**

---

## 8. Como executar

### Opção A: tudo em contêineres

Pré-requisito: Docker Desktop.

```bash
docker compose up --build
```

> Se já existir um PostgreSQL local usando a porta 5432, mude a porta publicada no host:
> `POSTGRES_HOST_PORT=5433 docker compose up --build`

- Gateway: <http://localhost:8080/api/...>
- Swagger de cada serviço: `http://localhost:808X/swagger-ui.html` (X = 1 a 5)

### Opção B: serviços pela IDE / Maven

1. Suba somente o banco: `docker compose up -d postgres`
2. Rode cada serviço (ordem indiferente):

```bash
./mvnw -pl services/cadastro-service spring-boot:run
```

(repita para `pedido-service`, `estoque-service`, `producao-service`, `financeiro-service` e `api-gateway`).

As conexões aceitam variáveis de ambiente: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` e
`*_URL` para os serviços integrados (veja `application.yaml` de cada módulo).

### Testes

```bash
./mvnw test
```

Os testes cobrem as regras de domínio (Regra de Ouro, transições de status, bloqueio de entrega sem
quitação, reserva de insumos, priorização da fila, pagamentos e lucratividade) **sem depender de banco
ou de Spring**.

### Testes ponta a ponta (com o ambiente Docker no ar)

Scripts em [`scripts/e2e/`](scripts/e2e). Usam só a biblioteca padrão do Python 3, chamam a API pelo
gateway e podem ser executados várias vezes, porque cada execução cria dados próprios.

**Fluxo completo do pedido** (35 verificações): falta de insumo → solicitação de compra → reprocessamento
→ alteração antes da produção → Regra de Ouro → baixa de estoque → entrega bloqueada até a quitação →
rastreio → lucratividade → erros padronizados.

```bash
python scripts/e2e/fluxo_completo.py
```

**Compensação da saga:** com o `financeiro-service` fora do ar, o registro do pedido deve responder 503
sem gravar o pedido, devolvendo a reserva de estoque e cancelando a ordem de produção.

```bash
docker compose stop financeiro-service
python scripts/e2e/compensacao.py
docker compose start financeiro-service
```

Para apontar para outro endereço, defina `API_BASE_URL` (padrão: `http://localhost:8080/api`).

---

## 9. Endpoints

Todos acessíveis pelo gateway com o prefixo `/api` (ex.: `GET http://localhost:8080/api/pedidos`).

| Serviço | Método e caminho | Descrição |
|---|---|---|
| cadastro | `POST/GET /clientes`, `GET/PUT/DELETE /clientes/{id}` | CRUD de clientes (DELETE = inativar) |
| cadastro | `POST/GET /insumos`, `GET/PUT/DELETE /insumos/{id}` | CRUD de insumos |
| cadastro | `POST/GET /produtos`, `GET/PUT/DELETE /produtos/{id}` | Catálogo + ficha técnica + custo estimado |
| pedido | `POST /pedidos` · `GET /pedidos?status=&clienteId=` · `GET /pedidos/{id}` | Registro e consulta |
| pedido | `PUT /pedidos/{id}` | Alteração (somente antes da produção) |
| pedido | `POST /pedidos/{id}/reprocessamento-insumos` | Nova tentativa de reserva |
| pedido | `POST /pedidos/{id}/despacho` · `/entrega` · `/cancelamento` | Ciclo de vida |
| pedido | `GET /rastreio/{codigo}` | Rastreio público (atendimento, site e WhatsApp) |
| estoque | `POST/GET /estoque` · `GET/PUT /estoque/{insumoId}` · `GET /estoque/alertas` | Saldos e alertas de mínimo |
| estoque | `POST /estoque/{insumoId}/entradas` · `GET /estoque/{insumoId}/movimentacoes` | Entradas e auditoria |
| estoque | `GET /solicitacoes-compra?status=` · `POST /solicitacoes-compra/{id}/recebimento` | Demandas de compra |
| produção | `GET /producao/fila` · `POST /producao/ordens/{id}/inicio` · `/conclusao` · `/retrabalhos` | Chão de fábrica |
| financeiro | `GET /financeiro/contas?status=` · `GET /financeiro/contas/pedido/{pedidoId}` | Contas a receber |
| financeiro | `POST /financeiro/contas/pedido/{pedidoId}/pagamentos` | Sinal / parcela / quitação |
| financeiro | `GET /financeiro/lucratividade?inicio=&fim=` | Margem por categoria de produto |

Endpoints de **integração interna** (chamados entre serviços): `POST /estoque/reservas`,
`POST /estoque/reservas/{pedidoId}/consumo`, `DELETE /estoque/reservas/{pedidoId}`, `POST /producao/ordens`,
`DELETE /producao/ordens/pedido/{pedidoId}`, `POST /pedidos/{id}/inicio-producao`,
`POST /pedidos/{id}/conclusao-producao`, `POST /financeiro/contas`, `PUT /financeiro/contas/pedido/{id}/itens`,
`GET /financeiro/contas/pedido/{id}/situacao`.

---

## 10. Limitações conhecidas e próximos passos

- **Mensageria assíncrona** (RabbitMQ/Kafka) com *outbox pattern* para substituir as chamadas síncronas
  entre serviços. Hoje, uma falha de rede entre a reserva e a gravação do pedido é tratada por
  compensação *best-effort*.
- **Segurança:** autenticação/autorização de funcionários (Spring Security + OAuth2/JWT) no gateway e
  restrição dos endpoints de integração interna.
- **Integração WhatsApp Business API:** um bot consumindo `GET /api/rastreio/{codigo}`.
- **Capacidade produtiva:** usar `tempoMedioFabricacaoHoras` × fila para sugerir prazos realistas no
  momento da venda.
- **Observabilidade:** Actuator, *tracing* distribuído (Micrometer + OpenTelemetry) e *circuit breaker*
  (Resilience4j) nos clientes HTTP.
- **Testes de integração** com Testcontainers (PostgreSQL) para os adaptadores de persistência.
- **Erros do gateway:** quando o serviço de destino está fora do ar, o `api-gateway` responde 500 (falha
  rápida, mas com o código genérico). O ideal é 503, com um filtro de *circuit breaker* e *fallback*.
