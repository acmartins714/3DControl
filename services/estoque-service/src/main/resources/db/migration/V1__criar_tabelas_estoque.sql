CREATE TABLE item_estoque (
    id                    UUID PRIMARY KEY,
    insumo_id             UUID           NOT NULL UNIQUE,
    nome_insumo           VARCHAR(120),
    unidade_medida        VARCHAR(10),
    quantidade_disponivel NUMERIC(14, 4) NOT NULL DEFAULT 0,
    quantidade_reservada  NUMERIC(14, 4) NOT NULL DEFAULT 0,
    estoque_minimo        NUMERIC(14, 4) NOT NULL DEFAULT 0,
    versao                BIGINT
);

CREATE TABLE reserva (
    id         UUID PRIMARY KEY,
    pedido_id  UUID           NOT NULL,
    insumo_id  UUID           NOT NULL,
    quantidade NUMERIC(14, 4) NOT NULL,
    status     VARCHAR(15)    NOT NULL,
    criada_em  TIMESTAMP      NOT NULL
);

CREATE TABLE movimentacao_estoque (
    id         UUID PRIMARY KEY,
    insumo_id  UUID           NOT NULL,
    tipo       VARCHAR(20)    NOT NULL,
    quantidade NUMERIC(14, 4) NOT NULL,
    pedido_id  UUID,
    observacao VARCHAR(255),
    data_hora  TIMESTAMP      NOT NULL
);

CREATE TABLE solicitacao_compra (
    id            UUID PRIMARY KEY,
    insumo_id     UUID           NOT NULL,
    nome_insumo   VARCHAR(120),
    quantidade    NUMERIC(14, 4) NOT NULL,
    pedido_id     UUID,
    status        VARCHAR(15)    NOT NULL,
    solicitada_em TIMESTAMP      NOT NULL,
    atualizada_em TIMESTAMP      NOT NULL
);

CREATE INDEX idx_reserva_pedido ON reserva (pedido_id, status);
CREATE INDEX idx_movimentacao_insumo ON movimentacao_estoque (insumo_id, data_hora);
CREATE INDEX idx_solicitacao_status ON solicitacao_compra (status);
