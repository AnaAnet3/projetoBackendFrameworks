-- Listify - Migration V3: histórico de listas concluídas (vem da backend2)
ALTER TABLE listas ADD COLUMN concluida_em DATETIME NULL;

CREATE TABLE historico_listas (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    lista_id      BIGINT       NOT NULL,
    nome_lista    VARCHAR(255) NOT NULL,
    concluida_em  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_historico_lista FOREIGN KEY (lista_id)
        REFERENCES listas (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
