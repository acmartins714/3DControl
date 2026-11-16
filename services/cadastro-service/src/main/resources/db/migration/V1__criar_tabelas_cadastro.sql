CREATE TABLE cliente (
    id                          UUID PRIMARY KEY,
    nome                        VARCHAR(150) NOT NULL,
    documento                   VARCHAR(20) UNIQUE,
    telefone                    VARCHAR(20),
    email                       VARCHAR(150),
    canal_preferencial          VARCHAR(20)  NOT NULL,
    preferencias_personalizacao TEXT,
    ativo                       BOOLEAN      NOT NULL DEFAULT TRUE,
    cadastrado_em               TIMESTAMP    NOT NULL
);

CREATE TABLE insumo (
    id             UUID PRIMARY KEY,
    nome           VARCHAR(120)   NOT NULL,
    descricao      TEXT,
    unidade_medida VARCHAR(10)    NOT NULL,
    custo_unitario NUMERIC(12, 4) NOT NULL,
    uso_regular    BOOLEAN        NOT NULL DEFAULT TRUE,
    ativo          BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE TABLE produto (
    id                           UUID PRIMARY KEY,
    nome                         VARCHAR(150)   NOT NULL,
    descricao                    TEXT,
    categoria                    VARCHAR(80)    NOT NULL,
    especificacoes_tecnicas      TEXT,
    tempo_medio_fabricacao_horas INTEGER        NOT NULL,
    preco_base                   NUMERIC(12, 2) NOT NULL,
    ativo                        BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE TABLE produto_composicao (
    produto_id UUID           NOT NULL REFERENCES produto (id) ON DELETE CASCADE,
    insumo_id  UUID           NOT NULL REFERENCES insumo (id),
    quantidade NUMERIC(12, 4) NOT NULL,
    PRIMARY KEY (produto_id, insumo_id)
);

CREATE INDEX idx_cliente_nome ON cliente (nome);
CREATE INDEX idx_produto_categoria ON produto (categoria);
