const express = require('express');
const pool = require('../db');
const router = express.Router();

const nomeValido = (n) => typeof n === 'string' && n.trim().length > 0 && n.length <= 120;

// Listar listas (?status=ativa | concluida). Sem filtro, retorna todas.
router.get('/', async (req, res) => {
  try {
    const { status } = req.query;
    let sql = 'SELECT * FROM listas';
    const params = [];
    if (status === 'ativa' || status === 'concluida') {
      sql += ' WHERE status = ?';
      params.push(status);
    }
    sql += ' ORDER BY criada_em DESC';
    const [rows] = await pool.query(sql, params);
    res.json(rows);
  } catch (e) {
    res.status(500).json({ erro: e.message });
  }
});

// Histórico: listas concluídas, da mais recente para a mais antiga
router.get('/historico', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT l.*, COUNT(i.id) AS total_itens
       FROM listas l
       LEFT JOIN itens_lista i ON i.lista_id = l.id
       WHERE l.status = 'concluida'
       GROUP BY l.id
       ORDER BY l.concluida_em DESC`
    );
    res.json(rows);
  } catch (e) {
    res.status(500).json({ erro: e.message });
  }
});

// Buscar uma lista
router.get('/:id', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT * FROM listas WHERE id = ?', [req.params.id]);
    if (!rows.length) return res.status(404).json({ erro: 'Lista não encontrada' });
    res.json(rows[0]);
  } catch (e) {
    res.status(500).json({ erro: e.message });
  }
});

// Criar lista
router.post('/', async (req, res) => {
  try {
    const { nome } = req.body;
    if (!nomeValido(nome)) return res.status(400).json({ erro: 'Nome inválido' });
    const [r] = await pool.query('INSERT INTO listas (nome) VALUES (?)', [nome.trim()]);
    const [rows] = await pool.query('SELECT * FROM listas WHERE id = ?', [r.insertId]);
    res.status(201).json(rows[0]);
  } catch (e) {
    res.status(500).json({ erro: e.message });
  }
});

// Editar lista (nome)
router.put('/:id', async (req, res) => {
  try {
    const { nome } = req.body;
    if (!nomeValido(nome)) return res.status(400).json({ erro: 'Nome inválido' });
    const [r] = await pool.query('UPDATE listas SET nome = ? WHERE id = ?', [nome.trim(), req.params.id]);
    if (!r.affectedRows) return res.status(404).json({ erro: 'Lista não encontrada' });
    const [rows] = await pool.query('SELECT * FROM listas WHERE id = ?', [req.params.id]);
    res.json(rows[0]);
  } catch (e) {
    res.status(500).json({ erro: e.message });
  }
});

// Excluir lista (itens saem junto, via ON DELETE CASCADE)
router.delete('/:id', async (req, res) => {
  try {
    const [r] = await pool.query('DELETE FROM listas WHERE id = ?', [req.params.id]);
    if (!r.affectedRows) return res.status(404).json({ erro: 'Lista não encontrada' });
    res.status(204).end();
  } catch (e) {
    res.status(500).json({ erro: e.message });
  }
});

// Duplicar lista: cópia nova e ativa, itens voltam como "não comprados"
router.post('/:id/duplicar', async (req, res) => {
  const conn = await pool.getConnection();
  try {
    const [orig] = await conn.query('SELECT * FROM listas WHERE id = ?', [req.params.id]);
    if (!orig.length) return res.status(404).json({ erro: 'Lista não encontrada' });

    const nome = (req.body && nomeValido(req.body.nome)) ? req.body.nome.trim() : `${orig[0].nome} (cópia)`;

    await conn.beginTransaction();
    const [nova] = await conn.query('INSERT INTO listas (nome) VALUES (?)', [nome]);
    await conn.query(
      `INSERT INTO itens_lista (lista_id, produto_id, quantidade, unidade, preco_estimado, preco_pago, comprado)
       SELECT ?, produto_id, quantidade, unidade, preco_estimado, NULL, 0
       FROM itens_lista WHERE lista_id = ?`,
      [nova.insertId, req.params.id]
    );
    await conn.commit();

    const [rows] = await conn.query('SELECT * FROM listas WHERE id = ?', [nova.insertId]);
    res.status(201).json(rows[0]);
  } catch (e) {
    await conn.rollback();
    res.status(500).json({ erro: e.message });
  } finally {
    conn.release();
  }
});

// Concluir lista: vai para o histórico
router.patch('/:id/concluir', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT status FROM listas WHERE id = ?', [req.params.id]);
    if (!rows.length) return res.status(404).json({ erro: 'Lista não encontrada' });
    if (rows[0].status === 'concluida') return res.status(409).json({ erro: 'Lista já concluída' });

    await pool.query("UPDATE listas SET status = 'concluida', concluida_em = NOW() WHERE id = ?", [req.params.id]);
    const [lista] = await pool.query('SELECT * FROM listas WHERE id = ?', [req.params.id]);
    res.json(lista[0]);
  } catch (e) {
    res.status(500).json({ erro: e.message });
  }
});

module.exports = router;