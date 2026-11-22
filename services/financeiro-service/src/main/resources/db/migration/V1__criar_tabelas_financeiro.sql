CREATE TABLE conta_receber (
    id            UUID PRIMARY KEY,
    pedido_id     UUID        NOT NULL UNIQUE,
    cliente_id    UUID        NOT NULL,
    status        VARCHAR(20) NOT NULL,
    criada_em     TIMESTAMP   NOT NULL,
    atualizada_em TIMESTAMP   NOT NULL
);

CREATE TABLE conta_receber_item (
    conta_id       UUID           NOT NULL REFERENCES conta_receber (id) ON DELETE CASCADE,
    categoria      VARCHAR(80)    NOT NULL,
    valor_venda    NUMERIC(12, 2) NOT NULL,
    custo_estimado NUMERIC(12, 2) NOT NULL
);

CREATE TABLE pagamento (
    conta_id     UUID           NOT NULL REFERENCES conta_receber (id) ON DELETE CASCADE,
    pagamento_id UUID           NOT NULL,
    valor        NUMERIC(12, 2) NOT NULL,
    tipo         VARCHAR(15)    NOT NULL,
    forma        VARCHAR(20)    NOT NULL,
    pago_em      TIMESTAMP      NOT NULL,
    observacao   VARCHAR(255)
);

CREATE INDEX idx_conta_status ON conta_receber (status);
CREATE INDEX idx_conta_criada_em ON conta_receber (criada_em);
CREATE INDEX idx_conta_item_conta ON conta_receber_item (conta_id);
CREATE INDEX idx_pagamento_conta ON pagamento (conta_id);
