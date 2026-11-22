CREATE TABLE pedido (
    id                    UUID PRIMARY KEY,
    codigo                VARCHAR(30)  NOT NULL UNIQUE,
    cliente_id            UUID         NOT NULL,
    canal_origem          VARCHAR(20)  NOT NULL,
    atendente_responsavel VARCHAR(100),
    prazo_entrega         DATE         NOT NULL,
    modalidade_entrega    VARCHAR(20)  NOT NULL,
    condicao_pagamento    VARCHAR(40)  NOT NULL,
    observacoes           VARCHAR(2000),
    status                VARCHAR(20)  NOT NULL,
    criado_em             TIMESTAMP    NOT NULL,
    atualizado_em         TIMESTAMP    NOT NULL,
    versao                BIGINT
);

CREATE TABLE pedido_item (
    pedido_id               UUID           NOT NULL REFERENCES pedido (id) ON DELETE CASCADE,
    produto_id              UUID           NOT NULL,
    nome_produto            VARCHAR(150),
    categoria               VARCHAR(80),
    quantidade              INTEGER        NOT NULL,
    preco_unitario          NUMERIC(12, 2) NOT NULL,
    custo_unitario_estimado NUMERIC(12, 2) NOT NULL,
    personalizacao          VARCHAR(2000)
);

CREATE TABLE pedido_historico (
    pedido_id  UUID        NOT NULL REFERENCES pedido (id) ON DELETE CASCADE,
    status     VARCHAR(20) NOT NULL,
    data_hora  TIMESTAMP   NOT NULL,
    observacao VARCHAR(500)
);

CREATE INDEX idx_pedido_status ON pedido (status);
CREATE INDEX idx_pedido_cliente ON pedido (cliente_id);
CREATE INDEX idx_pedido_item_pedido ON pedido_item (pedido_id);
CREATE INDEX idx_pedido_historico_pedido ON pedido_historico (pedido_id);
