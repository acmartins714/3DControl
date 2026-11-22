# Diagrama de Classes UML — 3D Control

Entidades de cada microsserviço e, em seguida, os componentes Spring de cada serviço com as
**anotações** que cada classe usa (indicadas no estereótipo `«…»`).
As setas tracejadas entre serviços representam **referências apenas por identificador** (UUID): cada serviço
tem o próprio banco e não há chave estrangeira entre bancos.

> Fonte PlantUML equivalente (2 diagramas: entidades e camadas do `pedido-service` com anotações):
> [`diagrama-classes.puml`](diagrama-classes.puml)
> (abra no plugin PlantUML do IntelliJ/VS Code ou em https://www.plantuml.com/plantuml para exportar PNG/SVG).

## Entidades (todos os serviços)

As entidades são classes JPA (`@Entity`; as partes que não têm tabela própria são `@Embeddable`) e também
guardam as **regras de negócio**: o estado só muda pelos métodos mostrados abaixo, que validam cada operação.

```mermaid
classDiagram
    direction LR

    namespace cadastro_service {
        class Cliente {
            <<@Entity>>
            -UUID id
            -String nome
            -String documento
            -String telefone
            -String email
            -CanalAtendimento canalPreferencial
            -String preferenciasPersonalizacao
            -boolean ativo
            +atualizar() void
            +inativar() void
        }
        class Produto {
            <<@Entity>>
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
        class ItemComposicao {
            <<@Embeddable>>
            -UUID insumoId
            -BigDecimal quantidade
        }
        class Insumo {
            <<@Entity>>
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
            <<@Entity>>
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
            <<@Embeddable>>
            -UUID produtoId
            -String nomeProduto
            -String categoria
            -int quantidade
            -BigDecimal precoUnitario
            -BigDecimal custoUnitarioEstimado
            -String personalizacao
            +subtotal() BigDecimal
        }
        class HistoricoStatus {
            <<@Embeddable>>
            -StatusPedido status
            -LocalDateTime dataHora
            -String observacao
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
            <<@Entity>>
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
            <<@Entity>>
            -UUID pedidoId
            -UUID insumoId
            -BigDecimal quantidade
            -Status status
            +consumir() void
            +cancelar() void
        }
        class MovimentacaoEstoque {
            <<@Entity>>
            -UUID insumoId
            -Tipo tipo
            -BigDecimal quantidade
            -UUID pedidoId
            -LocalDateTime dataHora
        }
        class SolicitacaoCompra {
            <<@Entity>>
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
            <<@Entity>>
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
            <<@Entity>>
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
            <<@Embeddable>>
            -String categoria
            -BigDecimal valorVenda
            -BigDecimal custoEstimado
        }
        class Pagamento {
            <<@Embeddable>>
            -BigDecimal valor
            -TipoPagamento tipo
            -FormaPagamento forma
            -LocalDateTime pagoEm
        }
    }

    Produto "1" *-- "0..*" ItemComposicao : ficha tecnica
    ItemComposicao ..> Insumo : insumoId
    Pedido "1" *-- "1..*" ItemPedido
    Pedido "1" *-- "1..*" HistoricoStatus : rastreio
    Pedido --> StatusPedido
    ItemEstoque "1" -- "0..*" Reserva
    ItemEstoque "1" -- "0..*" MovimentacaoEstoque
    OrdemProducao ..> Prioridade
    ContaReceber "1" *-- "1..*" ItemConta
    ContaReceber "1" *-- "0..*" Pagamento

    Pedido ..> Cliente : clienteId
    ItemPedido ..> Produto : produtoId
    ItemEstoque ..> Insumo : insumoId
    Reserva ..> Pedido : pedidoId
    SolicitacaoCompra ..> Pedido : pedidoId
    OrdemProducao ..> Pedido : pedidoId
    ContaReceber ..> Pedido : pedidoId
```

## pedido-service — camadas e anotações Spring

Cada classe mostra no estereótipo `«…»` as anotações que leva no código. O fluxo é sempre
**Controller → Service → Repository / Client**, e a entidade `Pedido` guarda as regras.

```mermaid
classDiagram
    direction TB

    class PedidoServiceApplication {
        <<@SpringBootApplication>>
        +main(String[] args)$
    }

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

    class PedidoService {
        <<@Service @Transactional>>
        +registrar(NovoPedidoRequest) PedidoResponse
        +alterar(UUID id, AlteracaoRequest) PedidoResponse
        +reprocessarInsumos(UUID id) PedidoResponse
        +iniciarProducao(UUID id) PedidoResponse
        +finalizarProducao(UUID id) PedidoResponse
        +despachar(UUID id) PedidoResponse
        +confirmarEntrega(UUID id) PedidoResponse
        +cancelar(UUID id, String motivo) PedidoResponse
        +buscar(UUID id) PedidoResponse
        +listar(StatusPedido s, UUID clienteId) List~PedidoResponse~
        -compensar(UUID pedidoId) void
    }
    class RastreioService {
        <<@Service @Transactional(readOnly = true)>>
        +rastrear(String codigo) Rastreio
    }

    class PedidoRepository {
        <<interface — Spring Data>>
        extends JpaRepository~Pedido, UUID~
        +findByCodigo(String codigo) Optional~Pedido~
        +findByStatusOrderByPrazoEntrega(StatusPedido s) List~Pedido~
    }

    class Pedido {
        <<@Entity @Table pedido>>
        @Id UUID id
        @Enumerated StatusPedido status
        @Version Long versao
        @ElementCollection List~ItemPedido~ itens
        @ElementCollection List~HistoricoStatus~ historico
    }
    class ItemPedido {
        <<@Embeddable>>
    }
    class HistoricoStatus {
        <<@Embeddable>>
    }

    class CadastroClient {
        <<@Component — RestClient>>
    }
    class EstoqueClient {
        <<@Component — RestClient>>
    }
    class ProducaoClient {
        <<@Component — RestClient>>
    }
    class FinanceiroClient {
        <<@Component — RestClient>>
    }

    PedidoController --> PedidoService
    RastreioController --> RastreioService
    PedidoService --> PedidoRepository
    PedidoService --> CadastroClient
    PedidoService --> EstoqueClient
    PedidoService --> ProducaoClient
    PedidoService --> FinanceiroClient
    RastreioService --> PedidoRepository
    PedidoService ..> Pedido : cria e altera
    PedidoRepository ..> Pedido
    Pedido *-- ItemPedido
    Pedido *-- HistoricoStatus
```

## Demais serviços — componentes Spring

Todos seguem o mesmo padrão: `@RestController` → `@Service @Transactional` → `JpaRepository` →
`@Entity`. Cada serviço também tem um `@SpringBootApplication` e um `GlobalExceptionHandler`
(`@RestControllerAdvice`).

### cadastro-service

```mermaid
classDiagram
    direction LR
    class ClienteController { <<@RestController /clientes>> }
    class InsumoController { <<@RestController /insumos>> }
    class ProdutoController { <<@RestController /produtos>> }
    class ClienteService { <<@Service @Transactional>> }
    class InsumoService { <<@Service @Transactional>> }
    class ProdutoService { <<@Service @Transactional>> }
    class ClienteRepository { <<JpaRepository>> }
    class InsumoRepository { <<JpaRepository>> }
    class ProdutoRepository { <<JpaRepository>> }
    class Cliente { <<@Entity @Table cliente>> }
    class Insumo { <<@Entity @Table insumo>> }
    class Produto { <<@Entity @Table produto>> }
    class ItemComposicao { <<@Embeddable>> }

    ClienteController --> ClienteService
    InsumoController --> InsumoService
    ProdutoController --> ProdutoService
    ClienteService --> ClienteRepository
    InsumoService --> InsumoRepository
    ProdutoService --> ProdutoRepository
    ProdutoService --> InsumoRepository
    ClienteRepository ..> Cliente
    InsumoRepository ..> Insumo
    ProdutoRepository ..> Produto
    Produto *-- ItemComposicao : @ElementCollection
```

### estoque-service

```mermaid
classDiagram
    direction LR
    class EstoqueController { <<@RestController /estoque>> }
    class SolicitacaoCompraController { <<@RestController /solicitacoes-compra>> }
    class EstoqueService { <<@Service @Transactional>> }
    class ReservaService { <<@Service @Transactional>> }
    class SolicitacaoCompraService { <<@Service @Transactional>> }
    class ItemEstoqueRepository { <<JpaRepository>> }
    class ReservaRepository { <<JpaRepository>> }
    class MovimentacaoEstoqueRepository { <<JpaRepository>> }
    class SolicitacaoCompraRepository { <<JpaRepository>> }
    class ItemEstoque { <<@Entity @Table item_estoque — @Version>> }
    class Reserva { <<@Entity @Table reserva>> }
    class MovimentacaoEstoque { <<@Entity @Table movimentacao_estoque>> }
    class SolicitacaoCompra { <<@Entity @Table solicitacao_compra>> }

    EstoqueController --> EstoqueService
    EstoqueController --> ReservaService
    SolicitacaoCompraController --> SolicitacaoCompraService
    SolicitacaoCompraService --> EstoqueService
    SolicitacaoCompraService --> SolicitacaoCompraRepository
    EstoqueService --> ItemEstoqueRepository
    EstoqueService --> MovimentacaoEstoqueRepository
    ReservaService --> ItemEstoqueRepository
    ReservaService --> ReservaRepository
    ReservaService --> MovimentacaoEstoqueRepository
    ReservaService --> SolicitacaoCompraRepository
    ItemEstoqueRepository ..> ItemEstoque
    ReservaRepository ..> Reserva
    MovimentacaoEstoqueRepository ..> MovimentacaoEstoque
    SolicitacaoCompraRepository ..> SolicitacaoCompra
```

### producao-service

```mermaid
classDiagram
    direction LR
    class OrdemProducaoController { <<@RestController /producao>> }
    class OrdemProducaoService { <<@Service @Transactional>> }
    class OrdemProducaoRepository { <<JpaRepository>> }
    class PedidoClient { <<@Component — RestClient>> }
    class OrdemProducao { <<@Entity @Table ordem_producao>> }

    OrdemProducaoController --> OrdemProducaoService
    OrdemProducaoService --> OrdemProducaoRepository
    OrdemProducaoService --> PedidoClient
    OrdemProducaoRepository ..> OrdemProducao
```

### financeiro-service

```mermaid
classDiagram
    direction LR
    class ContaReceberController { <<@RestController /financeiro/contas>> }
    class LucratividadeController { <<@RestController /financeiro/lucratividade>> }
    class ContaReceberService { <<@Service @Transactional>> }
    class LucratividadeService { <<@Service @Transactional(readOnly = true)>> }
    class ContaReceberRepository { <<JpaRepository>> }
    class ContaReceber { <<@Entity @Table conta_receber>> }
    class ItemConta { <<@Embeddable>> }
    class Pagamento { <<@Embeddable>> }

    ContaReceberController --> ContaReceberService
    LucratividadeController --> LucratividadeService
    ContaReceberService --> ContaReceberRepository
    LucratividadeService --> ContaReceberRepository
    ContaReceberRepository ..> ContaReceber
    ContaReceber *-- ItemConta : @ElementCollection
    ContaReceber *-- Pagamento : @ElementCollection
```

### Resumo das anotações por tipo de classe

| Tipo de classe | Anotações usadas | Exemplos |
|---|---|---|
| raiz do serviço | `@SpringBootApplication` | `PedidoServiceApplication` |
| Controller | `@RestController`, `@RequestMapping`, `@GetMapping`/`@PostMapping`/…, `@Valid`, `@RequestBody`, `@PathVariable` | `PedidoController` |
| Tratamento de erros | `@RestControllerAdvice`, `@ExceptionHandler` | `GlobalExceptionHandler` |
| Service | `@Service`, `@Transactional`, `@Transactional(readOnly = true)` | `PedidoService`, `ReservaService` |
| Repository | nenhuma (interface que estende `JpaRepository`) | `PedidoRepository` |
| Entidade | `@Entity`, `@Table`, `@Id`, `@Column`, `@Enumerated`, `@Version`, `@ElementCollection`, `@CollectionTable`, `@Embeddable` | `Pedido`, `ItemPedido` |
| Cliente HTTP | `@Component`, `@Value` | `EstoqueClient` |
| DTOs | `@NotNull`, `@NotBlank`, `@Positive`, `@DecimalMin`… (Bean Validation) | `PedidoDtos` |

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
