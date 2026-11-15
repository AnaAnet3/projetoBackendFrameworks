const app = document.querySelector('#app');

const UNIDADES = { un: 'un', kg: 'kg', g: 'g', L: 'L', pacote: 'pct' };

let abaAtual = 'ativas';
let modoMercado = false;
let timerAviso;


function escapar(texto) {
  const mapa = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
  return String(texto ?? '').replace(/[&<>"']/g, (c) => mapa[c]);
}

function moeda(valor) {
  return Number(valor || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function formatarData(valor) {
  const texto = String(valor);
  const data = texto.length === 10 ? new Date(texto + 'T12:00:00') : new Date(texto);
  return data.toLocaleDateString('pt-BR');
}

function paraNumero(valor) {
  if (valor === '' || valor == null) return null;
  return Number(String(valor).replace(',', '.'));
}

function percentual(total) {
  return total.totalItens ? (total.itensComprados / total.totalItens) * 100 : 0;
}

function montarOpcoes(pares, selecionado) {
  return pares
    .map(([valor, texto]) => {
      const marcado = String(valor) === String(selecionado) ? ' selected' : '';
      return `<option value="${escapar(valor)}"${marcado}>${escapar(texto)}</option>`;
    })
    .join('');
}

function opcoesUnidade(selecionada) {
  return montarOpcoes(Object.entries(UNIDADES), selecionada);
}


/* ---------- comunicação com a API ---------- */

async function api(caminho, metodo = 'GET', corpo) {
  const opcoes = { method: metodo };
  if (corpo) {
    opcoes.headers = { 'Content-Type': 'application/json' };
    opcoes.body = JSON.stringify(corpo);
  }

  const resposta = await fetch(caminho, opcoes);
  const texto = await resposta.text();

  let json = null;
  try {
    json = texto ? JSON.parse(texto) : null;
  } catch {
    // resposta sem JSON (ex.: 204 ou corpo vazio): segue com null
  }

  if (!resposta.ok) {
    throw new Error(json?.erro || 'Não foi possível falar com o servidor.');
  }
  return json;
}

function mostrarAviso(mensagem, sucesso = false) {
  const aviso = document.querySelector('#toast');
  aviso.style.background = sucesso ? 'var(--ok)' : '';
  aviso.textContent = mensagem;
  aviso.hidden = false;

  clearTimeout(timerAviso);
  timerAviso = setTimeout(() => {
    aviso.hidden = true;
  }, 3500);
}

// Roda a função e mostra o erro como aviso em vez de quebrar a tela
async function comTratamento(funcao) {
  try {
    await funcao();
  } catch (erro) {
    mostrarAviso(erro.message);
  }
}


/* ---------- janela de diálogo ---------- */

function campoDialogo(campo) {
  const controle = campo.select
    ? `<select name="${campo.nome}">${campo.select}</select>`
    : `<input name="${campo.nome}" type="${campo.tipo || 'text'}" step="any" min="0"
        value="${escapar(campo.valor ?? '')}" ${campo.obrigatorio ? 'required' : ''}>`;
  return `<label>${escapar(campo.rotulo)}${controle}</label>`;
}

function abrirDialogo(titulo, campos, aoSalvar) {
  const dialogo = document.querySelector('#dlg');

  dialogo.innerHTML = `
    <form method="dialog">
      <h3>${escapar(titulo)}</h3>
      ${campos.map(campoDialogo).join('')}
      <div class="linha">
        <button class="btn" value="x" formnovalidate>Cancelar</button>
        <button class="btn pri" value="ok">Salvar</button>
      </div>
    </form>`;

  dialogo.onclose = () => {
    const confirmou = dialogo.returnValue === 'ok';
    dialogo.returnValue = '';
    if (!confirmou) return;

    const valores = Object.fromEntries(new FormData(dialogo.querySelector('form')));
    comTratamento(() => aoSalvar(valores));
  };

  dialogo.showModal();
}


/* ---------- tela: minhas listas ---------- */

function botoesDaLista(lista) {
  const renomearEConcluir = `
    <button class="btn" title="Renomear" data-act="renomear" data-id="${lista.id}" data-nome="${escapar(lista.nome)}">✏️</button>
    <button class="btn" title="Concluir" data-act="concluir" data-id="${lista.id}">✅</button>`;

  return `
    <div class="acoes">
      ${lista.status === 'ATIVA' ? renomearEConcluir : ''}
      <button class="btn" title="Duplicar" data-act="duplicar" data-id="${lista.id}">📋</button>
      <button class="btn perigo" title="Excluir" data-act="excluir" data-id="${lista.id}" data-nome="${escapar(lista.nome)}">🗑️</button>
    </div>`;
}

function cartaoDaLista({ lista, total }) {
  const selo = lista.status !== 'ATIVA' ? '<span class="selo">Concluída</span>' : '';

  return `
    <div class="card">
      <a href="#/lista/${lista.id}">
        <div class="linha" style="justify-content:space-between"><h3>${escapar(lista.nome)}</h3>${selo}</div>
        <p class="mudo peq">Criada em ${formatarData(lista.criadaEm)} · ${total.totalItens} itens</p>
        <div class="barra"><i style="width:${percentual(total)}%"></i></div>
        <div class="val">
          <span>Estimado <b>${moeda(total.totalEstimado)}</b></span>
          <span>Gasto <b style="color:var(--pri)">${moeda(total.totalGasto)}</b></span>
        </div>
      </a>
      ${botoesDaLista(lista)}
    </div>`;
}

async function telaHome() {
  const listas = await api('/listas');
  const totais = await Promise.all(listas.map((l) => api(`/listas/${l.id}/total`)));

  const todas = listas.map((lista, i) => ({ lista, total: totais[i] }));
  const ativas = todas.filter((x) => x.lista.status === 'ATIVA');
  const historico = todas.filter((x) => x.lista.status !== 'ATIVA');
  const visiveis = abaAtual === 'ativas' ? ativas : historico;

  const mensagemVazia = abaAtual === 'ativas'
    ? 'Nenhuma lista ativa. Crie uma acima!'
    : 'Nenhuma lista concluída ainda.';

  app.innerHTML = `
    <div class="topo-pag">
      <div>
        <h1>Minhas listas</h1>
        <p class="mudo">Organize o que comprar e acompanhe quanto gastou.</p>
      </div>
      <form class="linha" data-form="criar">
        <input name="nome" placeholder="Nome da nova lista" maxlength="120" required style="width:16rem">
        <button class="btn pri">+ Criar</button>
      </form>
    </div>

    <div class="abas">
      <button data-act="aba" data-aba="ativas" class="${abaAtual === 'ativas' ? 'on' : ''}">Ativas (${ativas.length})</button>
      <button data-act="aba" data-aba="historico" class="${abaAtual === 'historico' ? 'on' : ''}">Histórico (${historico.length})</button>
    </div>

    ${visiveis.length
      ? `<div class="grade">${visiveis.map(cartaoDaLista).join('')}</div>`
      : `<p class="vazio">${mensagemVazia}</p>`}`;
}


/* ---------- tela: detalhe da lista ---------- */

function linhaDoItem(item, listaId, ativa) {
  const unidade = UNIDADES[item.unidade] || escapar(item.unidade);
  const precoPago = item.comprado && item.precoPago != null ? ` · pago ${moeda(item.precoPago)}` : '';
  const subtotal = item.quantidade * (item.precoPago ?? item.precoEstimado ?? 0);

  const botoesEdicao = `
    <div class="edita">
      <button class="btn" data-act="editar" data-id="${listaId}" data-item="${item.id}"
        data-q="${item.quantidade}" data-u="${item.unidade}" data-p="${item.precoEstimado ?? ''}">✏️</button>
      <button class="btn perigo" data-act="remover" data-id="${listaId}" data-item="${item.id}">🗑️</button>
    </div>`;

  return `
    <li class="item ${item.comprado ? 'c' : ''}">
      <button class="chk" ${ativa ? '' : 'disabled'} data-act="toggle" data-id="${listaId}" data-item="${item.id}"
        data-comprado="${item.comprado}" data-estimado="${item.precoEstimado ?? ''}" aria-label="Marcar como comprado">${item.comprado ? '✓' : ''}</button>
      <div class="info">
        <div class="nome">${escapar(item.produto)}</div>
        <div class="mudo peq">${item.quantidade} ${unidade} × ${moeda(item.precoEstimado)}${precoPago}</div>
      </div>
      <b>${moeda(subtotal)}</b>
      ${ativa ? botoesEdicao : ''}
    </li>`;
}

function grupoDeCategoria(grupo, listaId, ativa) {
  // itens já comprados vão para o fim do grupo
  const itens = [...grupo.itens].sort((a, b) => Number(a.comprado) - Number(b.comprado));
  return `
    <h2 class="cat">${escapar(grupo.categoria)}</h2>
    <ul class="itens">${itens.map((item) => linhaDoItem(item, listaId, ativa)).join('')}</ul>`;
}

function painelDeTotais(total) {
  return `
    <div class="tot">
      <h2>Totais</h2>
      <div class="l"><span>Estimado</span><b>${moeda(total.totalEstimado)}</b></div>
      <div class="l"><span>Gasto</span><b>${moeda(total.totalGasto)}</b></div>
      <div class="l"><span>Diferença</span><b>${moeda(total.totalEstimado - total.totalGasto)}</b></div>
      <p class="peq" style="margin:1rem 0 0">${total.itensComprados} de ${total.totalItens} itens comprados</p>
      <div class="barra"><i style="width:${percentual(total)}%"></i></div>
    </div>`;
}

async function telaDetalhe(id) {
  const [lista, grupos, total] = await Promise.all([
    api(`/listas/${id}`),
    api(`/listas/${id}/itens`),
    api(`/listas/${id}/total`),
  ]);
  const ativa = lista.status === 'ATIVA';

  const acoesDoTopo = `
    <div class="linha">
      <button class="btn ${modoMercado ? 'ok' : ''}" data-act="mercado" data-id="${id}">🛒 Modo mercado</button>
      <a class="btn pri" style="display:flex;align-items:center" href="#/lista/${id}/adicionar">+ Adicionar item</a>
    </div>`;

  app.innerHTML = `
    <a href="#/" class="mudo" style="font-weight:600">← Minhas listas</a>

    <div class="topo-pag" style="margin-top:.75rem">
      <div>
        <h1>${escapar(lista.nome)}</h1>
        <p class="mudo peq">Criada em ${formatarData(lista.criadaEm)} · ${ativa ? 'Ativa' : 'Concluída'}</p>
      </div>
      ${ativa ? acoesDoTopo : ''}
    </div>

    <div class="detalhe ${modoMercado ? 'mercado' : ''}">
      <section>
        ${grupos.length
          ? grupos.map((g) => grupoDeCategoria(g, id, ativa)).join('')
          : '<p class="vazio">Lista vazia. Adicione o primeiro item.</p>'}
      </section>

      <aside class="lateral">
        ${painelDeTotais(total)}
        <div class="tres">
          ${ativa ? `<button class="btn" data-act="concluir" data-id="${id}">Concluir</button>` : ''}
          <button class="btn" data-act="duplicar" data-id="${id}">Duplicar</button>
          <button class="btn perigo" data-act="excluir" data-id="${id}" data-nome="${escapar(lista.nome)}">Excluir</button>
        </div>
      </aside>
    </div>`;
}


/* ---------- tela: adicionar item ---------- */

// Produtos já usados em qualquer lista, com o último preço como referência
async function carregarSugestoes(listas) {
  const gruposPorLista = await Promise.all(listas.map((l) => api(`/listas/${l.id}/itens`)));
  const sugestoes = {};

  // do mais antigo para o mais novo, assim o preço mais recente prevalece
  for (const grupos of gruposPorLista.reverse()) {
    for (const grupo of grupos) {
      for (const item of grupo.itens) {
        sugestoes[item.produto.toLowerCase()] = {
          produto: item.produto,
          categoria: item.categoria,
          unidade: item.unidade,
          preco: item.precoPago ?? item.precoEstimado,
        };
      }
    }
  }
  return sugestoes;
}

async function telaAdicionar(id) {
  const [lista, categorias, listas] = await Promise.all([
    api(`/listas/${id}`),
    api('/categorias'),
    api('/listas'),
  ]);
  const sugestoes = await carregarSugestoes(listas);

  const opcoesSugestao = Object.values(sugestoes)
    .map((s) => `<option value="${escapar(s.produto)}">${escapar(s.categoria)}</option>`)
    .join('');

  app.innerHTML = `
    <a href="#/lista/${id}" class="mudo" style="font-weight:600">← ${escapar(lista.nome)}</a>
    <h1 style="margin:.75rem 0 1.5rem">Adicionar item</h1>

    <form class="card add" data-form="item" data-id="${id}">
      <label>Produto
        <input name="produto" list="sug" required autofocus autocomplete="off" placeholder="Ex.: Arroz">
      </label>
      <datalist id="sug">${opcoesSugestao}</datalist>

      <label>Categoria
        <select name="categoriaId">${montarOpcoes(categorias.map((c) => [c.id, c.nome]))}</select>
      </label>

      <div class="tres-c">
        <label>Quantidade<input name="quantidade" type="number" min="0.001" step="any" value="1" required></label>
        <label>Unidade<select name="unidade">${opcoesUnidade('un')}</select></label>
        <label>Preço estimado (R$)<input name="precoEstimado" type="number" min="0" step="0.01" value="0"></label>
      </div>

      <div class="linha" style="justify-content:flex-end">
        <button type="button" class="btn" data-act="outro">Salvar e adicionar outro</button>
        <button class="btn pri">Adicionar</button>
      </div>
    </form>`;

  // ao escolher um produto conhecido, preenche categoria, unidade e preço
  const form = app.querySelector('form');
  form.elements.produto.addEventListener('input', () => {
    const sugestao = sugestoes[form.elements.produto.value.trim().toLowerCase()];
    if (!sugestao) return;

    const categoria = categorias.find((c) => c.nome === sugestao.categoria);
    if (categoria) form.elements.categoriaId.value = categoria.id;
    form.elements.unidade.value = sugestao.unidade;
    form.elements.precoEstimado.value = sugestao.preco ?? 0;
  });
}

async function salvarItem(form, adicionarOutro) {
  if (!form.reportValidity()) return;

  const listaId = form.dataset.id;
  const dados = Object.fromEntries(new FormData(form));

  await api(`/listas/${listaId}/itens`, 'POST', {
    produto: dados.produto.trim(),
    categoriaId: Number(dados.categoriaId),
    quantidade: paraNumero(dados.quantidade),
    unidade: dados.unidade,
    precoEstimado: paraNumero(dados.precoEstimado),
  });

  if (adicionarOutro) {
    form.reset();
    form.elements.produto.focus();
    mostrarAviso('Item adicionado.', true);
  } else {
    location.hash = `#/lista/${listaId}`;
  }
}


/* ---------- ações dos botões ---------- */

const atualizar = () => rotear();
const estaNaHome = () => location.hash === '' || location.hash === '#/';

const acoes = {
  aba(dados) {
    abaAtual = dados.aba;
    atualizar();
  },

  mercado() {
    modoMercado = !modoMercado;
    atualizar();
  },

  renomear(dados) {
    abrirDialogo('Renomear lista', [{ nome: 'nome', rotulo: 'Novo nome', valor: dados.nome, obrigatorio: true }],
      async (valores) => {
        await api(`/listas/${dados.id}`, 'PUT', { nome: valores.nome });
        atualizar();
      });
  },

  async concluir(dados) {
    await api(`/listas/${dados.id}/concluir`, 'POST');
    mostrarAviso('Lista concluída e guardada no histórico.', true);
    if (estaNaHome()) atualizar();
    else location.hash = '#/';
  },

  async duplicar(dados) {
    const copia = await api(`/listas/${dados.id}/duplicar`, 'POST');
    location.hash = `#/lista/${copia.id}`;
  },

  async excluir(dados) {
    if (!confirm(`Excluir "${dados.nome}"?`)) return;
    await api(`/listas/${dados.id}`, 'DELETE');
    if (estaNaHome()) atualizar();
    else location.hash = '#/';
  },

  async toggle(dados) {
    const urlComprado = `/listas/${dados.id}/itens/${dados.item}/comprado`;

    if (dados.comprado === 'true') {
      await api(urlComprado, 'PATCH', { comprado: false });
      atualizar();
      return;
    }

    abrirDialogo('Preço pago por unidade (R$)',
      [{ nome: 'preco', rotulo: 'Preço', tipo: 'number', valor: dados.estimado, obrigatorio: true }],
      async (valores) => {
        await api(urlComprado, 'PATCH', { comprado: true, precoPago: paraNumero(valores.preco) });
        atualizar();
      });
  },

  editar(dados) {
    const campos = [
      { nome: 'q', rotulo: 'Quantidade', tipo: 'number', valor: dados.q, obrigatorio: true },
      { nome: 'u', rotulo: 'Unidade', select: opcoesUnidade(dados.u) },
      { nome: 'p', rotulo: 'Preço estimado (R$)', tipo: 'number', valor: dados.p },
    ];
    abrirDialogo('Editar item', campos, async (valores) => {
      await api(`/listas/${dados.id}/itens/${dados.item}`, 'PATCH', {
        quantidade: paraNumero(valores.q),
        unidade: valores.u,
        precoEstimado: paraNumero(valores.p),
      });
      atualizar();
    });
  },

  async remover(dados) {
    if (!confirm('Remover este item?')) return;
    await api(`/listas/${dados.id}/itens/${dados.item}`, 'DELETE');
    atualizar();
  },

  outro(dados, botao) {
    return salvarItem(botao.closest('form'), true);
  },
};

document.addEventListener('click', (evento) => {
  const botao = evento.target.closest('[data-act]');
  if (!botao) return;
  comTratamento(() => acoes[botao.dataset.act](botao.dataset, botao));
});

document.addEventListener('submit', (evento) => {
  const form = evento.target;
  if (!form.dataset.form) return; // formulários dos diálogos não passam por aqui
  evento.preventDefault();

  comTratamento(async () => {
    if (form.dataset.form === 'criar') {
      const nova = await api('/listas', 'POST', { nome: form.elements.nome.value.trim() });
      location.hash = `#/lista/${nova.id}`;
    } else {
      await salvarItem(form, false);
    }
  });
});


/* ---------- rotas (pelo hash da URL) ---------- */

async function rotear() {
  const [, pagina, idTexto, subpagina] = location.hash.split('/');
  const id = Number(idTexto);

  await comTratamento(async () => {
    if (pagina === 'lista' && id && subpagina === 'adicionar') await telaAdicionar(id);
    else if (pagina === 'lista' && id) await telaDetalhe(id);
    else await telaHome();
  });
}

addEventListener('hashchange', () => {
  window.scrollTo(0, 0);
  rotear();
});

rotear();