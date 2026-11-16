# Diagrama de Classes UML — 3D Control

Modelo de domínio de cada microsserviço (camada `domain`) e, em seguida, os componentes Spring de cada
serviço com as **anotações** que cada classe usa (indicadas no estereótipo `«…»`).
As setas tracejadas entre serviços representam **referências apenas por identificador** (UUID): cada serviço
tem o próprio banco e não há chave estrangeira entre bancos.

> Fonte PlantUML equivalente (2 diagramas: domínio e camadas do `pedido-service` com anotações):
> [`diagrama-classes.puml`](diagrama-classes.puml)
> (abra no plugin PlantUML do IntelliJ/VS Code ou em https://www.plantuml.com/plantuml para exportar PNG/SVG).

## Visão geral do domínio (todos os contextos)

Classes de domínio: Java puro, **sem anotações** de Spring ou JPA.

```mermaid
classDiagram
    direction LR

    namespace cadastro_service {
        class Cliente {
            -UUID id
            -String nome
            -String documento
            -String telefone
            -String email
            -CanalAtendimento canalPreferencial
            -String preferenciasPersonalizacao
            -boolean ativo
            +novo()$ Cliente
            +atualizar() void
            +inativar() void
        }
        class Produto {
            -UUID id
            -String nome
            -String categoria
            -String especificacoesTecnicas
            -int tempoMedioFabricacaoHoras
            -BigDecimal precoBase
            -boolean ativo
            +calcularCustoInsumos(Map custos) BigDecimal
            +atualizar() void
        }
        class ComposicaoProduto {
            <<record>>
            +UUID insumoId
            +BigDecimal quantidade
        }
        class Insumo {
            -UUID id
            -String nome
            -UnidadeMedida unidadeMedida
            -BigDecimal custoUnitario
            -boolean usoRegular
            -boolean ativo
        }
    }

    namespace pedido_service {
        class Pedido {
            <<aggregate root>>
            -UUID id
            -String codigo
            -UUID clienteId
            -CanalAtendimento canalOrigem
            -String atendenteResponsavel
            -LocalDate prazoEntrega
            -ModalidadeEntrega modalidadeEntrega
            -CondicaoPagamento condicaoPagamento
            -StatusPedido status
            -Long versao
            +registrar()$ Pedido
            +garantirAlteravel() void
            +alterar() void
            +atualizarDisponibilidadeInsumos(boolean ok) void
            +iniciarProducao() void
            +finalizarProducao() void
            +despachar() void
            +confirmarEntrega(boolean quitado, BigDecimal saldo) void
            +cancelar(String motivo) void
            +valorTotal() BigDecimal
        }
        class ItemPedido {
            <<record>>
            +UUID produtoId
            +String nomeProduto
            +String categoria
            +int quantidade
            +BigDecimal precoUnitario
            +BigDecimal custoUnitarioEstimado
            +String personalizacao
            +subtotal() BigDecimal
        }
        class HistoricoStatus {
            <<record>>
            +StatusPedido status
            +LocalDateTime dataHora
            +String observacao
        }
        class StatusPedido {
            <<enumeration>>
            EM_TRIAGEM
            AGUARDANDO_INSUMO
            EM_PRODUCAO
            FINALIZADO
            EM_ROTA_ENTREGA
            ENTREGUE
            CANCELADO
            +permiteAlteracao() boolean
            +podeTransitarPara(StatusPedido d) boolean
        }
    }

    namespace estoque_service {
        class ItemEstoque {
            <<aggregate root>>
            -UUID insumoId
            -BigDecimal quantidadeDisponivel
            -BigDecimal quantidadeReservada
            -BigDecimal estoqueMinimo
            -Long versao
            +registrarEntrada(BigDecimal q) void
            +saldoLivre() BigDecimal
            +reservar(BigDecimal q) void
            +consumirReserva(BigDecimal q) void
            +liberarReserva(BigDecimal q) void
            +abaixoDoMinimo() boolean
        }
        class Reserva {
            -UUID pedidoId
            -UUID insumoId
            -BigDecimal quantidade
            -Status status
            +consumir() void
            +cancelar() void
        }
        class MovimentacaoEstoque {
            <<record>>
            +UUID insumoId
            +Tipo tipo
            +BigDecimal quantidade
            +UUID pedidoId
            +LocalDateTime dataHora
        }
        class SolicitacaoCompra {
            -UUID insumoId
            -BigDecimal quantidade
            -UUID pedidoId
            -Status status
            +registrarRecebimento() void
            +cancelar() void
        }
    }

    namespace producao_service {
        class OrdemProducao {
            <<aggregate root>>
            -UUID pedidoId
            -String codigoPedido
            -LocalDate prazoEntrega
            -boolean insumosDisponiveis
            -StatusOrdem status
            -String responsavel
            -int retrabalhos
            +replanejar(LocalDate prazo, boolean insumos) void
            +iniciar(String responsavel) void
            +registrarRetrabalho(String motivo) void
            +concluir() void
            +prioridade(LocalDate hoje) Prioridade
        }
        class FilaProducao {
            <<domain service>>
            +ordenar(List ordens, LocalDate hoje)$ List
        }
        class Prioridade {
            <<enumeration>>
            URGENTE
            ALTA
            NORMAL
            BAIXA
        }
    }

    namespace financeiro_service {
        class ContaReceber {
            <<aggregate root>>
            -UUID pedidoId
            -UUID clienteId
            -StatusConta status
            +registrarPagamento(BigDecimal v, FormaPagamento f) Pagamento
            +atualizarItens(List itens) void
            +liberadaParaEntrega() boolean
            +saldoRemanescente() BigDecimal
            +cancelar() void
        }
        class ItemConta {
            <<record>>
            +String categoria
            +BigDecimal valorVenda
            +BigDecimal custoEstimado
        }
        class Pagamento {
            <<record>>
            +BigDecimal valor
            +TipoPagamento tipo
            +FormaPagamento forma
            +LocalDateTime pagoEm
        }
        class AnaliseLucratividade {
            <<domain service>>
            +porCategoria(List contas)$ List
        }
    }

    Produto "1" *-- "0..*" ComposicaoProduto : ficha tecnica
    ComposicaoProduto ..> Insumo : insumoId
    Pedido "1" *-- "1..*" ItemPedido
    Pedido "1" *-- "1..*" HistoricoStatus : rastreio
    Pedido --> StatusPedido
    ItemEstoque "1" -- "0..*" Reserva
    ItemEstoque "1" -- "0..*" MovimentacaoEstoque
    OrdemProducao ..> Prioridade
    FilaProducao ..> OrdemProducao
    ContaReceber "1" *-- "1..*" ItemConta
    ContaReceber "1" *-- "0..*" Pagamento
    AnaliseLucratividade ..> ContaReceber

    Pedido ..> Cliente : clienteId
    ItemPedido ..> Produto : produtoId
    ItemEstoque ..> Insumo : insumoId
    Reserva ..> Pedido : pedidoId
    SolicitacaoCompra ..> Pedido : pedidoId
    OrdemProducao ..> Pedido : pedidoId
    ContaReceber ..> Pedido : pedidoId
```

## pedido-service — camadas e anotações Spring

Cada classe mostra no estereótipo `«…»` as anotações que leva no código. As classes de domínio
(`Pedido`, `ItemPedido`…) **não têm anotações**: são Java puro. Os casos de uso dependem apenas das
**portas** (interfaces), e os adaptadores da infraestrutura as implementam.

```mermaid
classDiagram
    direction TB

    class PedidoServiceApplication {
        <<@SpringBootApplication>>
        +main(String[] args)$
    }

    %% ---------- infrastructure.web ----------
    class PedidoController {
        <<@RestController @RequestMapping /pedidos>>
        +registrar(@Valid @RequestBody NovoPedidoRequest) PedidoResponse
        +alterar(@PathVariable UUID, @Valid @RequestBody AlteracaoRequest) PedidoResponse
        +confirmarEntrega(@PathVariable UUID) PedidoResponse
        +cancelar(@PathVariable UUID, @RequestBody CancelamentoRequest) PedidoResponse
    }
    class RastreioController {
        <<@RestController @RequestMapping /rastreio>>
        +rastrear(@PathVariable String codigo) Rastreio
    }
    class GlobalExceptionHandler {
        <<@RestControllerAdvice>>
        +naoEncontrado() ProblemDetail 404
        +regraDeNegocio() ProblemDetail 422
        +concorrencia() ProblemDetail 409
        +integracao() ProblemDetail 503
    }

    %% ---------- application ----------
    class RegistrarPedidoUseCase {
        <<@Service @Transactional>>
        +executar(NovoPedido cmd) Pedido
    }
    class AlterarPedidoUseCase {
        <<@Service @Transactional>>
        +executar(UUID id, AlteracaoPedido cmd) Pedido
    }
    class MovimentarPedidoUseCase {
        <<@Service @Transactional>>
        +reprocessarInsumos(UUID id) Pedido
        +iniciarProducao(UUID id) Pedido
        +finalizarProducao(UUID id) Pedido
        +despachar(UUID id) Pedido
        +confirmarEntrega(UUID id) Pedido
        +cancelar(UUID id, String motivo) Pedido
    }
    class ConsultarPedidosUseCase {
        <<@Service @Transactional(readOnly = true)>>
        +buscar(UUID id) Pedido
        +listar(StatusPedido s, UUID clienteId) List~Pedido~
    }
    class RastrearPedidoUseCase {
        <<@Service @Transactional(readOnly = true)>>
        +rastrear(String codigo) Rastreio
    }
    class MontadorItensPedido {
        <<@Component>>
        +montar(List~ItemCommand~ itens) ItensResolvidos
        +necessidades(List~ItemPedido~ itens) List~InsumoNecessario~
    }

    %% ---------- portas (interfaces da aplicação/domínio) ----------
    class PedidoRepository {
        <<interface>>
        +salvar(Pedido p) Pedido
        +buscarPorId(UUID id) Optional~Pedido~
        +buscarPorCodigo(String codigo) Optional~Pedido~
    }
    class CadastroGateway {
        <<interface>>
    }
    class EstoqueGateway {
        <<interface>>
    }
    class ProducaoGateway {
        <<interface>>
    }
    class FinanceiroGateway {
        <<interface>>
    }

    %% ---------- domain (sem anotações) ----------
    class Pedido {
        <<domain — sem anotações>>
    }

    %% ---------- infrastructure.persistence ----------
    class PedidoRepositoryAdapter {
        <<@Component>>
    }
    class PedidoJpaRepository {
        <<interface — Spring Data>>
        extends JpaRepository~PedidoJpa, UUID~
        +findByCodigo(String codigo) Optional~PedidoJpa~
    }
    class PedidoJpa {
        <<@Entity @Table pedido>>
        @Id UUID id
        @Enumerated StatusPedido status
        @Version Long versao
        @ElementCollection List~ItemPedidoEmbeddable~ itens
        @ElementCollection List~HistoricoEmbeddable~ historico
        +from(Pedido p)$ PedidoJpa
        +toDomain() Pedido
    }
    class ItemPedidoEmbeddable {
        <<@Embeddable>>
    }
    class HistoricoEmbeddable {
        <<@Embeddable>>
    }

    %% ---------- infrastructure.client ----------
    class CadastroHttpGateway {
        <<@Component — RestClient>>
    }
    class EstoqueHttpGateway {
        <<@Component — RestClient>>
    }
    class ProducaoHttpGateway {
        <<@Component — RestClient>>
    }
    class FinanceiroHttpGateway {
        <<@Component — RestClient>>
    }

    PedidoController --> RegistrarPedidoUseCase
    PedidoController --> AlterarPedidoUseCase
    PedidoController --> MovimentarPedidoUseCase
    PedidoController --> ConsultarPedidosUseCase
    RastreioController --> RastrearPedidoUseCase

    RegistrarPedidoUseCase --> MontadorItensPedido
    AlterarPedidoUseCase --> MontadorItensPedido
    RegistrarPedidoUseCase --> PedidoRepository
    RegistrarPedidoUseCase --> CadastroGateway
    RegistrarPedidoUseCase --> EstoqueGateway
    RegistrarPedidoUseCase --> ProducaoGateway
    RegistrarPedidoUseCase --> FinanceiroGateway
    MovimentarPedidoUseCase --> PedidoRepository
    RastrearPedidoUseCase --> PedidoRepository
    MontadorItensPedido --> CadastroGateway
    RegistrarPedidoUseCase ..> Pedido : cria
    PedidoRepository ..> Pedido

    PedidoRepositoryAdapter ..|> PedidoRepository
    PedidoRepositoryAdapter --> PedidoJpaRepository
    PedidoJpaRepository ..> PedidoJpa
    PedidoJpa *-- ItemPedidoEmbeddable
    PedidoJpa *-- HistoricoEmbeddable
    PedidoJpa ..> Pedido : converte
    CadastroHttpGateway ..|> CadastroGateway
    EstoqueHttpGateway ..|> EstoqueGateway
    ProducaoHttpGateway ..|> ProducaoGateway
    FinanceiroHttpGateway ..|> FinanceiroGateway
```

## Demais serviços — componentes Spring

Todos seguem o mesmo padrão: `@RestController` → `@Service @Transactional` → porta (`interface`) ←
adaptador `@Component` → `JpaRepository` → `@Entity`. Cada serviço também tem um
`@SpringBootApplication` e um `GlobalExceptionHandler` (`@RestControllerAdvice`).

### cadastro-service

```mermaid
classDiagram
    direction LR
    class ClienteController { <<@RestController /clientes>> }
    class InsumoController { <<@RestController /insumos>> }
    class ProdutoController { <<@RestController /produtos>> }
    class GerenciarClientesUseCase { <<@Service @Transactional>> }
    class GerenciarInsumosUseCase { <<@Service @Transactional>> }
    class GerenciarProdutosUseCase { <<@Service @Transactional>> }
    class ClienteRepository { <<interface>> }
    class InsumoRepository { <<interface>> }
    class ProdutoRepository { <<interface>> }
    class ClienteRepositoryAdapter { <<@Component>> }
    class InsumoRepositoryAdapter { <<@Component>> }
    class ProdutoRepositoryAdapter { <<@Component>> }
    class ClienteJpaEntity { <<@Entity @Table cliente>> }
    class InsumoJpaEntity { <<@Entity @Table insumo>> }
    class ProdutoJpaEntity { <<@Entity @Table produto>> }
    class ComposicaoEmbeddable { <<@Embeddable>> }

    ClienteController --> GerenciarClientesUseCase
    InsumoController --> GerenciarInsumosUseCase
    ProdutoController --> GerenciarProdutosUseCase
    GerenciarClientesUseCase --> ClienteRepository
    GerenciarInsumosUseCase --> InsumoRepository
    GerenciarProdutosUseCase --> ProdutoRepository
    GerenciarProdutosUseCase --> InsumoRepository
    ClienteRepositoryAdapter ..|> ClienteRepository
    InsumoRepositoryAdapter ..|> InsumoRepository
    ProdutoRepositoryAdapter ..|> ProdutoRepository
    ClienteRepositoryAdapter ..> ClienteJpaEntity
    InsumoRepositoryAdapter ..> InsumoJpaEntity
    ProdutoRepositoryAdapter ..> ProdutoJpaEntity
    ProdutoJpaEntity *-- ComposicaoEmbeddable : @ElementCollection
```

### estoque-service

```mermaid
classDiagram
    direction LR
    class EstoqueController { <<@RestController /estoque>> }
    class SolicitacaoCompraController { <<@RestController /solicitacoes-compra>> }
    class GerenciarEstoqueUseCase { <<@Service @Transactional>> }
    class ReservarInsumosUseCase { <<@Service @Transactional>> }
    class GerenciarSolicitacoesCompraUseCase { <<@Service @Transactional>> }
    class ItemEstoqueRepository { <<interface>> }
    class ReservaRepository { <<interface>> }
    class MovimentacaoRepository { <<interface>> }
    class SolicitacaoCompraRepository { <<interface>> }
    class ItemEstoqueRepositoryAdapter { <<@Component>> }
    class ReservaRepositoryAdapter { <<@Component>> }
    class MovimentacaoRepositoryAdapter { <<@Component>> }
    class SolicitacaoCompraRepositoryAdapter { <<@Component>> }
    class ItemEstoqueJpa { <<@Entity @Table item_estoque — @Version>> }
    class ReservaJpa { <<@Entity @Table reserva>> }
    class MovimentacaoJpa { <<@Entity @Table movimentacao_estoque>> }
    class SolicitacaoCompraJpa { <<@Entity @Table solicitacao_compra>> }

    EstoqueController --> GerenciarEstoqueUseCase
    EstoqueController --> ReservarInsumosUseCase
    SolicitacaoCompraController --> GerenciarSolicitacoesCompraUseCase
    GerenciarSolicitacoesCompraUseCase --> GerenciarEstoqueUseCase
    GerenciarEstoqueUseCase --> ItemEstoqueRepository
    GerenciarEstoqueUseCase --> MovimentacaoRepository
    ReservarInsumosUseCase --> ItemEstoqueRepository
    ReservarInsumosUseCase --> ReservaRepository
    ReservarInsumosUseCase --> MovimentacaoRepository
    ReservarInsumosUseCase --> SolicitacaoCompraRepository
    GerenciarSolicitacoesCompraUseCase --> SolicitacaoCompraRepository
    ItemEstoqueRepositoryAdapter ..|> ItemEstoqueRepository
    ReservaRepositoryAdapter ..|> ReservaRepository
    MovimentacaoRepositoryAdapter ..|> MovimentacaoRepository
    SolicitacaoCompraRepositoryAdapter ..|> SolicitacaoCompraRepository
    ItemEstoqueRepositoryAdapter ..> ItemEstoqueJpa
    ReservaRepositoryAdapter ..> ReservaJpa
    MovimentacaoRepositoryAdapter ..> MovimentacaoJpa
    SolicitacaoCompraRepositoryAdapter ..> SolicitacaoCompraJpa
```

### producao-service

```mermaid
classDiagram
    direction LR
    class OrdemProducaoController { <<@RestController /producao>> }
    class PlanejarProducaoUseCase { <<@Service @Transactional>> }
    class ExecutarProducaoUseCase { <<@Service @Transactional>> }
    class OrdemProducaoConfig { <<@Configuration — @Bean Clock>> }
    class OrdemProducaoRepository { <<interface>> }
    class PedidoGateway { <<interface>> }
    class OrdemProducaoRepositoryAdapter { <<@Component>> }
    class PedidoHttpGateway { <<@Component — RestClient>> }
    class OrdemProducaoJpa { <<@Entity @Table ordem_producao>> }

    OrdemProducaoController --> PlanejarProducaoUseCase
    OrdemProducaoController --> ExecutarProducaoUseCase
    PlanejarProducaoUseCase --> OrdemProducaoRepository
    PlanejarProducaoUseCase ..> OrdemProducaoConfig : Clock
    ExecutarProducaoUseCase --> OrdemProducaoRepository
    ExecutarProducaoUseCase --> PedidoGateway
    OrdemProducaoRepositoryAdapter ..|> OrdemProducaoRepository
    OrdemProducaoRepositoryAdapter ..> OrdemProducaoJpa
    PedidoHttpGateway ..|> PedidoGateway
```

### financeiro-service

```mermaid
classDiagram
    direction LR
    class ContaReceberController { <<@RestController /financeiro/contas>> }
    class LucratividadeController { <<@RestController /financeiro/lucratividade>> }
    class GerenciarContasReceberUseCase { <<@Service @Transactional>> }
    class AnalisarLucratividadeUseCase { <<@Service @Transactional(readOnly = true)>> }
    class ContaReceberRepository { <<interface>> }
    class ContaReceberRepositoryAdapter { <<@Component>> }
    class ContaReceberJpa { <<@Entity @Table conta_receber>> }
    class ItemContaEmbeddable { <<@Embeddable>> }
    class PagamentoEmbeddable { <<@Embeddable>> }

    ContaReceberController --> GerenciarContasReceberUseCase
    LucratividadeController --> AnalisarLucratividadeUseCase
    GerenciarContasReceberUseCase --> ContaReceberRepository
    AnalisarLucratividadeUseCase --> ContaReceberRepository
    ContaReceberRepositoryAdapter ..|> ContaReceberRepository
    ContaReceberRepositoryAdapter ..> ContaReceberJpa
    ContaReceberJpa *-- ItemContaEmbeddable : @ElementCollection
    ContaReceberJpa *-- PagamentoEmbeddable : @ElementCollection
```

### Resumo das anotações por camada

| Camada | Anotações usadas | Exemplos |
|---|---|---|
| raiz do serviço | `@SpringBootApplication` | `PedidoServiceApplication` |
| `infrastructure.web` | `@RestController`, `@RequestMapping`, `@GetMapping`/`@PostMapping`/…, `@Valid`, `@RequestBody`, `@PathVariable`, `@RestControllerAdvice`, `@ExceptionHandler` | `PedidoController`, `GlobalExceptionHandler` |
| `application` | `@Service`, `@Transactional`, `@Transactional(readOnly = true)`, `@Component` | `RegistrarPedidoUseCase`, `MontadorItensPedido` |
| `infrastructure.persistence` | `@Component`, `@Entity`, `@Table`, `@Id`, `@Column`, `@Enumerated`, `@Version`, `@ElementCollection`, `@CollectionTable`, `@Embeddable` | `PedidoJpa`, `PedidoRepositoryAdapter` |
| `infrastructure.client` | `@Component`, `@Value` | `EstoqueHttpGateway` |
| `infrastructure.config` | `@Configuration`, `@Bean` | `OrdemProducaoConfig` (relógio) |
| `domain` | **nenhuma** (Java puro) | `Pedido`, `ContaReceber`, `ItemEstoque` |

## Máquina de estados do Pedido

```mermaid
stateDiagram-v2
    [*] --> EM_TRIAGEM : registrar (insumos reservados)
    EM_TRIAGEM --> AGUARDANDO_INSUMO : falta insumo
    AGUARDANDO_INSUMO --> EM_TRIAGEM : insumo recebido / reprocessar
    EM_TRIAGEM --> EM_PRODUCAO : producao-service inicia a ordem
    EM_PRODUCAO --> FINALIZADO : producao-service conclui
    FINALIZADO --> EM_ROTA_ENTREGA : despachar (ENTREGA)
    FINALIZADO --> ENTREGUE : retirada com pagamento quitado
    EM_ROTA_ENTREGA --> ENTREGUE : confirmar entrega com pagamento quitado
    EM_TRIAGEM --> CANCELADO
    AGUARDANDO_INSUMO --> CANCELADO
    ENTREGUE --> [*]
    CANCELADO --> [*]

    note right of EM_TRIAGEM
        Alteracoes permitidas
        (Regra de Ouro)
    end note
    note right of EM_PRODUCAO
        Pedido bloqueado
        para alteracoes
    end note
```
