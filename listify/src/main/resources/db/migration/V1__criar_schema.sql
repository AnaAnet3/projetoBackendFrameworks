-- Listify - Migration V1: criação do schema (MySQL 8.0.16 ou superior)

CREATE TABLE categorias (
    id    BIGINT      NOT NULL AUTO_INCREMENT,
    nome  VARCHAR(60) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_categorias_nome UNIQUE (nome)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE produtos (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    nome            VARCHAR(100) NOT NULL,
    categoria_id    BIGINT       NOT NULL,
    unidade_padrao  VARCHAR(10)  NOT NULL DEFAULT 'un',
    PRIMARY KEY (id),
    CONSTRAINT fk_produtos_categoria FOREIGN KEY (categoria_id)
        REFERENCES categorias (id),
    CONSTRAINT ck_produtos_unidade
        CHECK (unidade_padrao IN ('kg', 'g', 'L', 'un', 'pacote')),
    CONSTRAINT uq_produtos_nome UNIQUE (nome)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE listas (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    nome       VARCHAR(255) NOT NULL,
    criada_em  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status     VARCHAR(15)  NOT NULL DEFAULT 'ATIVA',
    PRIMARY KEY (id),
    CONSTRAINT ck_listas_status CHECK (status IN ('ATIVA', 'CONCLUIDA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE itens_lista (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    lista_id        BIGINT        NOT NULL,
    produto_id      BIGINT        NOT NULL,
    quantidade      DECIMAL(10,3) NOT NULL,
    unidade         VARCHAR(10)   NOT NULL,
    preco_estimado  DECIMAL(10,2) NULL,
    preco_pago      DECIMAL(10,2) NULL,
    comprado        BOOLEAN       NOT NULL DEFAULT FALSE,
    comprado_em     DATETIME      NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_itens_lista FOREIGN KEY (lista_id)
        REFERENCES listas (id) ON DELETE CASCADE,
    CONSTRAINT fk_itens_produto FOREIGN KEY (produto_id)
        REFERENCES produtos (id),
    CONSTRAINT ck_itens_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_itens_unidade CHECK (unidade IN ('kg', 'g', 'L', 'un', 'pacote')),
    CONSTRAINT ck_itens_preco_estimado CHECK (preco_estimado IS NULL OR preco_estimado >= 0),
    CONSTRAINT ck_itens_preco_pago CHECK (preco_pago IS NULL OR preco_pago >= 0),
    CONSTRAINT uq_item_por_lista UNIQUE (lista_id, produto_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_itens_produto ON itens_lista (produto_id);

-- View para GET /listas/{id}/total (estimado x gasto)
CREATE VIEW vw_total_lista AS
SELECT
    lista_id,
    COALESCE(SUM(quantidade * preco_estimado), 0) AS total_estimado,
    COALESCE(SUM(CASE WHEN comprado THEN quantidade * preco_pago END), 0) AS total_gasto
FROM itens_lista
GROUP BY lista_id;

-- Último preço pago por produto (referência para próximas estimativas)
CREATE VIEW vw_ultimo_preco_produto AS
SELECT produto_id, preco_pago AS ultimo_preco
FROM (
    SELECT
        i.produto_id,
        i.preco_pago,
        ROW_NUMBER() OVER (
            PARTITION BY i.produto_id
            ORDER BY (i.comprado_em IS NULL), i.comprado_em DESC, i.id DESC
        ) AS rn
    FROM itens_lista i
    WHERE i.comprado = TRUE AND i.preco_pago IS NOT NULL
) ranqueado
WHERE rn = 1;
