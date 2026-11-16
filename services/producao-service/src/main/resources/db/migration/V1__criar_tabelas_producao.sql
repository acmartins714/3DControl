CREATE TABLE ordem_producao (
    id                  UUID PRIMARY KEY,
    pedido_id           UUID        NOT NULL,
    codigo_pedido       VARCHAR(30),
    prazo_entrega       DATE        NOT NULL,
    insumos_disponiveis BOOLEAN     NOT NULL,
    status              VARCHAR(15) NOT NULL,
    responsavel         VARCHAR(100),
    retrabalhos         INTEGER     NOT NULL DEFAULT 0,
    observacoes         VARCHAR(2000),
    criada_em           TIMESTAMP   NOT NULL,
    iniciada_em         TIMESTAMP,
    concluida_em        TIMESTAMP
);

CREATE INDEX idx_ordem_pedido ON ordem_producao (pedido_id);
CREATE INDEX idx_ordem_status ON ordem_producao (status);
