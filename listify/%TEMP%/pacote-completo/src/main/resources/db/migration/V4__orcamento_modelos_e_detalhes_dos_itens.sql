-- Listify - Migration V4: orçamento por lista, modelos de lista
-- e marca/observação/prioridade nos itens.

-- 1) Listas: limite de gasto (opcional) e flag de modelo ("Compra do mês")
ALTER TABLE listas
    ADD COLUMN limite DECIMAL(10,2) NULL,
    ADD COLUMN modelo BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT ck_listas_limite CHECK (limite IS NULL OR limite > 0);

-- 2) Itens: marca, observação e prioridade
ALTER TABLE itens_lista
    ADD COLUMN marca       VARCHAR(60)  NULL,
    ADD COLUMN observacao  VARCHAR(255) NULL,
    ADD COLUMN prioridade  VARCHAR(10)  NOT NULL DEFAULT 'NORMAL',
    ADD CONSTRAINT ck_itens_prioridade CHECK (prioridade IN ('NORMAL', 'IMPORTANTE', 'ESSENCIAL'));
