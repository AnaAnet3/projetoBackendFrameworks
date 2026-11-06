-- Listify - Migration V2: dados iniciais (categorias e produtos base)

INSERT INTO categorias (nome) VALUES
    ('Hortifruti'),
    ('Laticínios'),
    ('Mercearia'),
    ('Carnes'),
    ('Padaria'),
    ('Bebidas'),
    ('Limpeza'),
    ('Higiene');

INSERT INTO produtos (nome, categoria_id, unidade_padrao) VALUES
    ('Banana',           (SELECT id FROM categorias WHERE nome = 'Hortifruti'), 'kg'),
    ('Tomate',           (SELECT id FROM categorias WHERE nome = 'Hortifruti'), 'kg'),
    ('Leite',            (SELECT id FROM categorias WHERE nome = 'Laticínios'), 'L'),
    ('Queijo mussarela', (SELECT id FROM categorias WHERE nome = 'Laticínios'), 'kg'),
    ('Arroz',            (SELECT id FROM categorias WHERE nome = 'Mercearia'),  'kg'),
    ('Feijão',           (SELECT id FROM categorias WHERE nome = 'Mercearia'),  'kg'),
    ('Macarrão',         (SELECT id FROM categorias WHERE nome = 'Mercearia'),  'pacote'),
    ('Frango',           (SELECT id FROM categorias WHERE nome = 'Carnes'),     'kg'),
    ('Pão francês',      (SELECT id FROM categorias WHERE nome = 'Padaria'),    'un'),
    ('Refrigerante',     (SELECT id FROM categorias WHERE nome = 'Bebidas'),    'L'),
    ('Detergente',       (SELECT id FROM categorias WHERE nome = 'Limpeza'),    'un'),
    ('Sabonete',         (SELECT id FROM categorias WHERE nome = 'Higiene'),    'un');
