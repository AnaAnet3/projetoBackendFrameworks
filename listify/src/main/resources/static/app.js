const app = document.querySelector('#app');

const UNIDADES = { un: 'un', kg: 'kg', g: 'g', L: 'L', pacote: 'pct' };

const PRIORIDADES = [['NORMAL', 'Normal'], ['IMPORTANTE', 'Importante'], ['ESSENCIAL', 'Essencial']];
const ROTULO_PRIORIDADE = { IMPORTANTE: 'Importante', ESSENCIAL: 'Essencial' };
const PESO_PRIORIDADE = { ESSENCIAL: 0, IMPORTANTE: 1, NORMAL: 2 };

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
    const erroDeCampo = Object.values(json?.campos ?? {})[0];
    throw new Error(erroDeCampo || json?.erro || 'Não foi possível falar com o servidor.');
  }
  return json;
}

async function apiTexto(caminho) {
  const resposta = await fetch(caminho);
  if (!resposta.ok) throw new Error('Não foi possível gerar o texto da lista.');
  return resposta.text();
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
  const dica = campo.comDica ? '<small class="dica mudo peq"></small>' : '';
  return `<label>${escapar(campo.rotulo)}${controle}${dica}</label>`;
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

function dadosDaLista(lista) {
  return `data-id="${lista.id}" data-nome="${escapar(lista.nome)}" data-limite="${lista.limite ?? ''}"`;
}

function botoesDaLista(lista) {
  return `
    <div class="acoes">
      <button class="btn" title="Editar nome e limite" data-act="renomear" ${dadosDaLista(lista)}>✏️</button>
      <button class="btn" title="Concluir" data-act="concluir" data-id="${lista.id}">✅</button>
      <button class="btn" title="Duplicar" data-act="duplicar" data-id="${lista.id}">📋</button>
      <button class="btn" title="Salvar como modelo" data-act="salvarModelo" data-id="${lista.id}">📌</button>
      <button class="btn perigo" title="Excluir" data-act="excluir" ${dadosDaLista(lista)}>🗑️</button>
    </div>`;
}

/** Selo com a situação do orçamento (vazio se a lista não tem limite). */
function seloOrcamento(total) {
  const limite = total.limite != null ? moeda(total.limite) : '';
  switch (total.situacaoOrcamento) {
    case 'GASTO_ESTOUROU': return `<span class="dif ruim">Gasto acima do limite · ${limite}</span>`;
    case 'ESTIMADO_ESTOUROU': return `<span class="dif ruim">Estimado acima do limite · ${limite}</span>`;
    case 'PERTO_DO_LIMITE': return `<span class="dif aviso">Perto do limite · ${limite}</span>`;
    case 'DENTRO': return `<span class="dif bom">Dentro do limite · ${limite}</span>`;
    default: return '';
  }
}

function cartaoDaLista({ lista, total }) {
  return `
    <div class="card">
      <a href="#/lista/${lista.id}">
        <h3>${escapar(lista.nome)}</h3>
        <p class="mudo peq">Criada em ${formatarData(lista.criadaEm)} · ${total.totalItens} itens</p>
        <div class="barra"><i style="width:${percentual(total)}%"></i></div>
        <div class="val">
          <span>Estimado <b>${moeda(total.totalEstimado)}</b></span>
          <span>Gasto <b style="color:var(--pri)">${moeda(total.totalGasto)}</b></span>
        </div>
        ${total.limite != null ? `<p style="margin:.6rem 0 0">${seloOrcamento(total)}</p>` : ''}
      </a>
      ${botoesDaLista(lista)}
    </div>`;
}

function cartaoDoModelo({ lista, total }) {
  const limite = lista.limite != null ? ` · limite ${moeda(lista.limite)}` : '';
  return `
    <div class="card modelo">
      <a href="#/lista/${lista.id}">
        <h3>${escapar(lista.nome)} <span class="etiqueta">Modelo</span></h3>
        <p class="mudo peq">${total.totalItens} itens · estimado ${moeda(total.totalEstimado)}${limite}</p>
      </a>
      <div class="acoes">
        <button class="btn pri" data-act="usarModelo" data-id="${lista.id}">Usar modelo</button>
        <button class="btn" title="Editar nome e limite" data-act="renomear" ${dadosDaLista(lista)}>✏️</button>
        <button class="btn perigo" title="Excluir" data-act="excluir" ${dadosDaLista(lista)}>🗑️</button>
      </div>
    </div>`;
}

async function telaHome() {
  // as listas concluídas ficam na página de histórico; os modelos têm seção própria
  const abertas = (await api('/listas')).filter((lista) => lista.status === 'ATIVA');
  const totais = await Promise.all(abertas.map((l) => api(`/listas/${l.id}/total`)));
  const cartoes = abertas.map((lista, i) => ({ lista, total: totais[i] }));
  const ativas = cartoes.filter(({ lista }) => !lista.modelo);
  const modelos = cartoes.filter(({ lista }) => lista.modelo);

  app.innerHTML = `
    <div class="topo-pag">
      <div>
        <h1>Minhas listas</h1>
        <p class="mudo">Organize o que comprar e acompanhe quanto gastou.</p>
      </div>
      <form class="linha" data-form="criar">
        <input name="nome" placeholder="Nome da nova lista" maxlength="120" required style="width:16rem">
        <input name="limite" type="number" min="0.01" step="0.01" placeholder="Limite (R$)"
          aria-label="Limite de gasto (opcional)" style="width:9rem">
        <button class="btn pri">+ Criar</button>
      </form>
    </div>

    ${ativas.length
      ? `<div class="grade">${ativas.map(cartaoDaLista).join('')}</div>`
      : '<p class="vazio">Nenhuma lista ativa. Crie uma acima!</p>'}

    ${modelos.length ? `
      <h2 class="secao"> Modelos</h2>
      <p class="mudo peq" style="margin:0 0 1rem">Listas prontas para reutilizar, como a "Compra do mês". Use um modelo para criar uma lista nova com os mesmos itens.</p>
      <div class="grade">${modelos.map(cartaoDoModelo).join('')}</div>`
      : '<p class="mudo peq" style="margin-top:1.5rem">Dica: use 📌 em uma lista para guardá-la como modelo e reaproveitá-la todo mês.</p>'}

    <p class="mudo peq" style="margin-top:1.5rem">
      As listas concluídas ficam no <a href="#/historico" style="text-decoration:underline">histórico de compras</a>.
    </p>`;
}


/* ---------- tela: detalhe da lista ---------- */

function linhaDoItem(item, listaId, ativa, modelo) {
  const unidade = UNIDADES[item.unidade] || escapar(item.unidade);
  const precoPago = item.comprado && item.precoPago != null ? ` · pago ${moeda(item.precoPago)}` : '';
  const subtotal = item.quantidade * (item.precoPago ?? item.precoEstimado ?? 0);

  const marca = item.marca ? ` <span class="marca">${escapar(item.marca)}</span>` : '';
  const rotulo = ROTULO_PRIORIDADE[item.prioridade];
  const selo = rotulo ? ` <span class="tag ${item.prioridade.toLowerCase()}">${rotulo}</span>` : '';
  const observacao = item.observacao ? `<div class="obs peq">📝 ${escapar(item.observacao)}</div>` : '';

  const botoesEdicao = `
    <div class="edita">
      <button class="btn" data-act="editar" data-id="${listaId}" data-item="${item.id}"
        data-q="${item.quantidade}" data-u="${item.unidade}" data-p="${item.precoEstimado ?? ''}"
        data-m="${escapar(item.marca ?? '')}" data-o="${escapar(item.observacao ?? '')}" data-pr="${item.prioridade}">✏️</button>
      <button class="btn perigo" data-act="remover" data-id="${listaId}" data-item="${item.id}">🗑️</button>
    </div>`;

  // modelos não são "comprados": sem a bolinha de marcar
  const marcador = modelo ? '' : `
      <button class="chk" ${ativa ? '' : 'disabled'} data-act="toggle" data-id="${listaId}" data-item="${item.id}"
        data-comprado="${item.comprado}" data-estimado="${item.precoEstimado ?? ''}" data-produto="${escapar(item.produto)}" aria-label="Marcar como comprado">${item.comprado ? '✓' : ''}</button>`;

  return `
    <li class="item ${item.comprado ? 'c' : ''}">${marcador}
      <div class="info">
        <div><span class="nome">${escapar(item.produto)}</span>${marca}${selo}</div>
        <div class="mudo peq">${item.quantidade} ${unidade} × ${moeda(item.precoEstimado)}${precoPago}</div>
        ${observacao}
      </div>
      <b>${moeda(subtotal)}</b>
      ${ativa ? botoesEdicao : ''}
    </li>`;
}

function grupoDeCategoria(grupo, listaId, ativa, modelo) {
  // comprados vão para o fim do grupo; entre os demais, os mais prioritários primeiro
  const itens = [...grupo.itens].sort((a, b) =>
    Number(a.comprado) - Number(b.comprado)
    || (PESO_PRIORIDADE[a.prioridade] ?? 2) - (PESO_PRIORIDADE[b.prioridade] ?? 2));
  return `
    <h2 class="cat">${escapar(grupo.categoria)}</h2>
    <ul class="itens">${itens.map((item) => linhaDoItem(item, listaId, ativa, modelo)).join('')}</ul>`;
}

/* ----- orçamento ----- */

function mensagemOrcamento(total) {
  const limite = total.limite;
  switch (total.situacaoOrcamento) {
    case 'GASTO_ESTOUROU':
      return { classe: 'erro', texto: `🚨 Você já gastou ${moeda(total.totalGasto - limite)} acima do limite de ${moeda(limite)}.` };
    case 'ESTIMADO_ESTOUROU':
      return { classe: 'erro', texto: `⚠️ O total estimado passa ${moeda(total.totalEstimado - limite)} do limite de ${moeda(limite)}.` };
    case 'PERTO_DO_LIMITE':
      return { classe: 'aviso', texto: `⚠️ Atenção: você está perto do limite de ${moeda(limite)}.` };
    default:
      return null;
  }
}

function bannerOrcamento(total) {
  const aviso = mensagemOrcamento(total);
  return aviso ? `<div class="banner ${aviso.classe}" role="alert">${escapar(aviso.texto)}</div>` : '';
}

function linhaOrcamento(rotulo, valor, limite) {
  const razao = valor / limite;
  const classe = razao > 1 ? 'erro' : razao >= 0.9 ? 'aviso' : '';
  return `
    <div class="l"><span>${rotulo}</span><b>${moeda(valor)} · ${Math.round(razao * 100)}%</b></div>
    <div class="barra orc ${classe}"><i style="width:${Math.min(100, razao * 100)}%"></i></div>`;
}

function blocoOrcamento(total) {
  if (total.limite == null) return '';
  return `
    <div class="orc-bloco">
      <div class="l"><span>Limite</span><b>${moeda(total.limite)}</b></div>
      ${linhaOrcamento('Estimado', total.totalEstimado, total.limite)}
      ${linhaOrcamento('Gasto', total.totalGasto, total.limite)}
    </div>`;
}

function painelDeTotais(total, modelo) {
  const compras = modelo
    ? `<p class="peq" style="margin:1rem 0 0">${total.totalItens} itens no modelo</p>`
    : `
      <div class="l"><span>Gasto</span><b>${moeda(total.totalGasto)}</b></div>
      <div class="l"><span>Diferença</span><b>${moeda(total.totalEstimado - total.totalGasto)}</b></div>
      <p class="peq" style="margin:1rem 0 0">${total.itensComprados} de ${total.totalItens} itens comprados</p>
      <div class="barra"><i style="width:${percentual(total)}%"></i></div>`;

  return `
    <div class="tot">
      <h2>Totais</h2>
      <div class="l"><span>Estimado</span><b>${moeda(total.totalEstimado)}</b></div>
      ${compras}
      ${blocoOrcamento(total)}
    </div>`;
}

/* ----- exportar e compartilhar ----- */

function blocoExportar(id) {
  return `
    <div class="exportar no-print">
      <h2 class="cat">Exportar e compartilhar</h2>
      <div class="dois">
        <button class="btn" data-act="imprimir">🖨️ PDF / imprimir</button>
        <a class="btn" href="/listas/${id}/exportar/csv" download>📊 CSV</a>
        <button class="btn" data-act="whatsapp" data-id="${id}">💬 WhatsApp</button>
        <button class="btn" data-act="copiarTexto" data-id="${id}">📋 Copiar texto</button>
      </div>
    </div>`;
}

async function telaDetalhe(id) {
  const [lista, grupos, total] = await Promise.all([
    api(`/listas/${id}`),
    api(`/listas/${id}/itens`),
    api(`/listas/${id}/total`),
  ]);
  const ativa = lista.status === 'ATIVA';
  const modelo = lista.modelo;
  const situacao = modelo ? 'Modelo' : ativa ? 'Ativa' : 'Concluída';

  const botaoEditar = `<button class="btn" title="Editar nome e limite" data-act="renomear" ${dadosDaLista(lista)}>✏️</button>`;
  const botaoAdicionar = `<a class="btn pri" style="display:flex;align-items:center" href="#/lista/${id}/adicionar">+ Adicionar item</a>`;

  const acoesDoTopo = modelo
    ? `<div class="linha no-print">
         ${botaoEditar}
         <button class="btn pri" data-act="usarModelo" data-id="${id}">Usar modelo</button>
         ${botaoAdicionar}
       </div>`
    : `<div class="linha no-print">
         ${botaoEditar}
         <button class="btn ${modoMercado ? 'ok' : ''}" data-act="mercado" data-id="${id}">🛒 Modo mercado</button>
         ${botaoAdicionar}
       </div>`;

  app.innerHTML = `
    <a href="#/" class="mudo no-print" style="font-weight:600">← Minhas listas</a>

    <div class="topo-pag" style="margin-top:.75rem">
      <div>
        <h1>${escapar(lista.nome)}</h1>
        <p class="mudo peq">Criada em ${formatarData(lista.criadaEm)} · ${situacao}</p>
      </div>
      ${ativa ? acoesDoTopo : ''}
    </div>

    <div class="detalhe ${modoMercado ? 'mercado' : ''}">
      <section>
        ${bannerOrcamento(total)}
        ${grupos.length
          ? grupos.map((g) => grupoDeCategoria(g, id, ativa, modelo)).join('')
          : '<p class="vazio">Lista vazia. Adicione o primeiro item.</p>'}
      </section>

      <aside class="lateral">
        ${painelDeTotais(total, modelo)}
        <div class="tres no-print">
          ${ativa && !modelo ? `<button class="btn" data-act="concluir" data-id="${id}">Concluir</button>` : ''}
          <button class="btn" data-act="duplicar" data-id="${id}">Duplicar</button>
          <button class="btn perigo" data-act="excluir" ${dadosDaLista(lista)}>Excluir</button>
        </div>
        ${ativa && !modelo ? `<button class="btn no-print" data-act="salvarModelo" data-id="${id}">📌 Salvar como modelo</button>` : ''}
        ${blocoExportar(id)}
      </aside>
    </div>`;
}


async function buscarUltimoPreco(nome) {
  const produtos = await api(`/produtos?busca=${encodeURIComponent(nome)}`);
  const produto = produtos.find((p) => p.nome.toLowerCase() === nome.toLowerCase());
  return produto?.ultimoPreco ?? null;
}

function textoDeVariacao(preco, referencia) {
  if (referencia == null) return 'Primeira compra deste produto.';
  if (preco == null) return `Última compra: ${moeda(referencia)}`;

  const variacao = ((preco - referencia) / referencia) * 100;
  if (Math.abs(variacao) < 0.5) return `Igual à última compra (${moeda(referencia)}).`;
  const sentido = variacao > 0 ? '▲ subiu' : '▼ caiu';
  return `${sentido} ${Math.abs(variacao).toFixed(0)}% desde a última compra (${moeda(referencia)}).`;
}


/* ---------- tela: adicionar item ---------- */

async function telaAdicionar(id) {
  const [lista, categorias] = await Promise.all([
    api(`/listas/${id}`),
    api('/categorias'),
  ]);

  app.innerHTML = `
    <a href="#/lista/${id}" class="mudo" style="font-weight:600">← ${escapar(lista.nome)}</a>
    <h1 style="margin:.75rem 0 1.5rem">Adicionar item</h1>

    <form class="card add" data-form="item" data-id="${id}">
      <label>Produto
        <input name="produto" list="sug" required autofocus autocomplete="off" placeholder="Ex.: Arroz">
      </label>
      <datalist id="sug"></datalist>

      <label>Categoria
        <select name="categoriaId">${montarOpcoes(categorias.map((c) => [c.id, c.nome]))}</select>
      </label>

      <div class="tres-c">
        <label>Quantidade<input name="quantidade" type="number" min="0.001" step="any" value="1" required></label>
        <label>Unidade<select name="unidade">${opcoesUnidade('un')}</select></label>
        <label>Preço estimado (R$)<input name="precoEstimado" type="number" min="0" step="0.01" value="0"><small id="dica-preco" class="mudo peq"></small></label>
      </div>

      <div class="dois-c">
        <label>Marca (opcional)<input name="marca" maxlength="60" placeholder="Ex.: Tio João"></label>
        <label>Prioridade<select name="prioridade">${montarOpcoes(PRIORIDADES, 'NORMAL')}</select></label>
      </div>

      <label>Observação (opcional)
        <input name="observacao" maxlength="255" placeholder="Ex.: sem glúten, pegar o mais verde">
      </label>

      <div class="linha" style="justify-content:flex-end">
        <button type="button" class="btn" data-act="outro">Salvar e adicionar outro</button>
        <button class="btn pri">Adicionar</button>
      </div>
    </form>`;

  const form = app.querySelector('form');
  const dicaPreco = document.querySelector('#dica-preco');
  let temporizador;

  form.elements.produto.addEventListener('input', () => {
    clearTimeout(temporizador);
    temporizador = setTimeout(async () => {
      const texto = form.elements.produto.value.trim();
      try {
        const produtos = texto ? await api(`/produtos?busca=${encodeURIComponent(texto)}`) : [];
        document.querySelector('#sug').innerHTML = produtos
          .map((p) => `<option value="${escapar(p.nome)}">${escapar(p.categoria)}</option>`)
          .join('');

        const escolhido = produtos.find((p) => p.nome.toLowerCase() === texto.toLowerCase());
        dicaPreco.textContent = '';
        if (!escolhido) return;

        form.elements.categoriaId.value = escolhido.categoriaId;
        form.elements.unidade.value = escolhido.unidade;
        if (escolhido.ultimoPreco != null) {
          form.elements.precoEstimado.value = escolhido.ultimoPreco;
          dicaPreco.textContent = `Última compra: ${moeda(escolhido.ultimoPreco)}`;
        }
      } catch (erro) {
        return;
      }
    }, 200);
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
    marca: dados.marca.trim(),
    observacao: dados.observacao.trim(),
    prioridade: dados.prioridade,
  });

  if (adicionarOutro) {
    form.reset();
    form.elements.produto.focus();
    mostrarAviso('Item adicionado.', true);
  } else {
    location.hash = `#/lista/${listaId}`;
  }
}


/* ---------- tela: histórico de compras ---------- */

const PERIODOS = [
  ['todos', 'Todo o período'],
  ['30dias', 'Últimos 30 dias'],
  ['mes', 'Este mês'],
  ['ano', 'Este ano'],
];

// compras vindas da API e filtros escolhidos na tela
let comprasConcluidas = [];
const filtrosHistorico = { busca: '', periodo: 'todos' };

function filtrarCompras(compras) {
  const busca = filtrosHistorico.busca.trim().toLowerCase();
  const agora = new Date();
  const trintaDias = 30 * 24 * 60 * 60 * 1000;

  return compras.filter((compra) => {
    if (busca && !compra.nomeLista.toLowerCase().includes(busca)) return false;

    const data = new Date(compra.concluidaEm);
    switch (filtrosHistorico.periodo) {
      case '30dias': return agora - data <= trintaDias;
      case 'mes': return data.getFullYear() === agora.getFullYear() && data.getMonth() === agora.getMonth();
      case 'ano': return data.getFullYear() === agora.getFullYear();
      default: return true;
    }
  });
}

// quanto se economizou (ou passou) nos itens que de fato foram comprados
function economiaDaCompra(compra) {
  return Number(compra.totalEstimadoComprados) - Number(compra.totalGasto);
}

function agruparPorMes(compras) {
  const grupos = new Map();
  for (const compra of compras) {
    const chave = compra.concluidaEm.slice(0, 7); // "2026-11"
    if (!grupos.has(chave)) grupos.set(chave, []);
    grupos.get(chave).push(compra);
  }
  return [...grupos.entries()]; // a API já manda da mais recente para a mais antiga
}

function nomeDoMes(chave) {
  const [ano, mes] = chave.split('-').map(Number);
  const texto = new Date(ano, mes - 1, 1).toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

function cartaoDeResumo(rotulo, valor, classe = '') {
  return `
    <div class="card">
      <div class="rotulo">${rotulo}</div>
      <div class="numero ${classe}">${valor}</div>
    </div>`;
}

function resumoDoHistorico(compras) {
  const totalGasto = compras.reduce((soma, c) => soma + Number(c.totalGasto), 0);
  const media = totalGasto / compras.length;

  return `
    <div class="resumo">
      ${cartaoDeResumo('Compras concluídas', compras.length)}
      ${cartaoDeResumo('Total gasto', moeda(totalGasto))}
      ${cartaoDeResumo('Média por compra', moeda(media))}
    </div>`;
}

function selo(compra) {
  const diferenca = economiaDaCompra(compra);
  if (compra.itensComprados === 0) return '<span class="dif neutro">Nenhum item comprado</span>';
  if (diferenca > 0.005) return `<span class="dif bom">Economizou ${moeda(diferenca)}</span>`;
  if (diferenca < -0.005) return `<span class="dif ruim">${moeda(-diferenca)} acima</span>`;
  return '<span class="dif neutro">No valor estimado</span>';
}

function cartaoDaCompra(compra) {
  const progresso = compra.totalItens ? (compra.itensComprados / compra.totalItens) * 100 : 0;

  return `
    <div class="card">
      <a href="#/lista/${compra.listaId}">
        <div class="linha" style="justify-content:space-between"><h3>${escapar(compra.nomeLista)}</h3>${selo(compra)}</div>
        <p class="mudo peq">Concluída em ${formatarData(compra.concluidaEm)} · ${compra.itensComprados} de ${compra.totalItens} itens comprados</p>
        <div class="barra"><i style="width:${progresso}%"></i></div>
        <div class="val">
          <span>Estimado <b>${moeda(compra.totalEstimado)}</b></span>
          <span>Gasto <b style="color:var(--pri)">${moeda(compra.totalGasto)}</b></span>
        </div>
      </a>
      <div class="acoes">
        <a class="btn" href="#/lista/${compra.listaId}">Ver lista</a>
        <button class="btn pri" data-act="duplicar" data-id="${compra.listaId}" title="Cria uma lista nova igual a esta">Comprar de novo</button>
      </div>
    </div>`;
}

function blocoDoMes([chave, compras]) {
  const gastoDoMes = compras.reduce((soma, c) => soma + Number(c.totalGasto), 0);
  const quantidade = compras.length === 1 ? '1 compra' : `${compras.length} compras`;

  return `
    <div class="mes">
      <h2>${nomeDoMes(chave)}</h2>
      <span class="mudo peq">${quantidade} · gasto <b>${moeda(gastoDoMes)}</b></span>
    </div>
    <div class="grade">${compras.map(cartaoDaCompra).join('')}</div>`;
}

// Redesenha só a área de resultados, para o campo de busca não perder o foco
function desenharHistorico() {
  const area = document.querySelector('#resultado-historico');
  if (!area) return;

  if (comprasConcluidas.length === 0) {
    area.innerHTML = `
      <p class="vazio">Nenhuma compra concluída ainda. Quando você concluir uma lista, ela aparece aqui.<br><br>
        <a class="btn pri" href="#/">Ir para minhas listas</a></p>`;
    return;
  }

  const compras = filtrarCompras(comprasConcluidas);
  if (compras.length === 0) {
    area.innerHTML = '<p class="vazio">Nenhuma compra encontrada com esses filtros.</p>';
    return;
  }

  area.innerHTML = resumoDoHistorico(compras) + agruparPorMes(compras).map(blocoDoMes).join('');
}

async function telaHistorico() {
  comprasConcluidas = await api('/listas/historico');
  filtrosHistorico.busca = '';
  filtrosHistorico.periodo = 'todos';

  app.innerHTML = `
    <div class="topo-pag">
      <div>
        <h1>Histórico de compras</h1>
        <p class="mudo">Todas as compras que você já concluiu, mês a mês.</p>
      </div>
    </div>

    <div class="filtros">
      <input type="search" data-filtro="busca" placeholder="Buscar pelo nome da lista" aria-label="Buscar compra">
      <select data-filtro="periodo" aria-label="Período">${montarOpcoes(PERIODOS, 'todos')}</select>
    </div>

    <div id="resultado-historico"></div>`;

  desenharHistorico();
}


/* ---------- ações dos botões ---------- */

const atualizar = () => rotear();
const estaNaHome = () => location.hash === '' || location.hash === '#/';

const acoes = {
  mercado() {
    modoMercado = !modoMercado;
    atualizar();
  },

  renomear(dados) {
    const campos = [
      { nome: 'nome', rotulo: 'Nome', valor: dados.nome, obrigatorio: true },
      { nome: 'limite', rotulo: 'Limite de gasto (R$, opcional)', tipo: 'number', valor: dados.limite },
    ];
    abrirDialogo('Editar lista', campos, async (valores) => {
      // limite vazio = lista sem orçamento
      await api(`/listas/${dados.id}`, 'PUT', { nome: valores.nome, limite: paraNumero(valores.limite) });
      atualizar();
    });
  },

    async salvarModelo(dados) {
    await api(`/listas/${dados.id}/salvar-modelo`, 'POST');
    if (estaNaHome()) atualizar();
  },

  async usarModelo(dados) {
    const nova = await api(`/listas/${dados.id}/usar-modelo`, 'POST');
    location.hash = `#/lista/${nova.id}`;
  },

  imprimir() {
    window.print(); // no diálogo de impressão, escolha "Salvar como PDF"
  },

  async whatsapp(dados) {
    const texto = await apiTexto(`/listas/${dados.id}/exportar/texto`);
    const janela = window.open(`https://wa.me/?text=${encodeURIComponent(texto)}`, '_blank');
    if (janela) {
      janela.opener = null;
    } else {
      mostrarAviso('O navegador bloqueou a janela do WhatsApp. Libere os pop-ups ou use "Copiar texto".');
    }
  },

  async copiarTexto(dados) {
    const texto = await apiTexto(`/listas/${dados.id}/exportar/texto`);
    if (!navigator.clipboard?.writeText) {
      mostrarAviso('Este navegador não permite copiar automaticamente. Use o botão do WhatsApp ou o CSV.');
      return;
    }
    await navigator.clipboard.writeText(texto);
    mostrarAviso('Texto da lista copiado!', true);
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

    const referencia = await buscarUltimoPreco(dados.produto);

    abrirDialogo('Preço pago por unidade (R$)',
      [{ nome: 'preco', rotulo: 'Preço', tipo: 'number', valor: dados.estimado, obrigatorio: true, comDica: true }],
      async (valores) => {
        await api(urlComprado, 'PATCH', { comprado: true, precoPago: paraNumero(valores.preco) });
        atualizar();
      });

    const campoPreco = document.querySelector('#dlg input[name="preco"]');
    const dica = document.querySelector('#dlg .dica');
    const mostrarVariacao = () => { dica.textContent = textoDeVariacao(paraNumero(campoPreco.value), referencia); };
    campoPreco.addEventListener('input', mostrarVariacao);
    mostrarVariacao();
  },

  editar(dados) {
    const campos = [
      { nome: 'q', rotulo: 'Quantidade', tipo: 'number', valor: dados.q, obrigatorio: true },
      { nome: 'u', rotulo: 'Unidade', select: opcoesUnidade(dados.u) },
      { nome: 'p', rotulo: 'Preço estimado (R$)', tipo: 'number', valor: dados.p },
      { nome: 'm', rotulo: 'Marca', valor: dados.m },
      { nome: 'pr', rotulo: 'Prioridade', select: montarOpcoes(PRIORIDADES, dados.pr) },
      { nome: 'o', rotulo: 'Observação', valor: dados.o },
    ];
    abrirDialogo('Editar item', campos, async (valores) => {
      await api(`/listas/${dados.id}/itens/${dados.item}`, 'PATCH', {
        quantidade: paraNumero(valores.q),
        unidade: valores.u,
        precoEstimado: paraNumero(valores.p),
        marca: valores.m.trim(),         // texto vazio apaga a marca
        observacao: valores.o.trim(),    // texto vazio apaga a observação
        prioridade: valores.pr,
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

document.addEventListener('input', (evento) => {
  const campo = evento.target.closest('[data-filtro]');
  if (!campo) return;
  filtrosHistorico[campo.dataset.filtro] = campo.value;
  desenharHistorico();
});

document.addEventListener('submit', (evento) => {
  const form = evento.target;
  if (!form.dataset.form) return; // formulários dos diálogos não passam por aqui
  evento.preventDefault();

  comTratamento(async () => {
    if (form.dataset.form === 'criar') {
      const nova = await api('/listas', 'POST', {
        nome: form.elements.nome.value.trim(),
        limite: paraNumero(form.elements.limite.value),
      });
      location.hash = `#/lista/${nova.id}`;
    } else {
      await salvarItem(form, false);
    }
  });
});


/* ---------- rotas (pelo hash da URL) ---------- */

function marcarMenu(atual) {
  document.querySelectorAll('.menu a').forEach((link) => {
    link.classList.toggle('on', link.dataset.nav === atual);
  });
}

async function rotear() {
  const [, pagina, idTexto, subpagina] = location.hash.split('/');
  const id = Number(idTexto);

  marcarMenu(pagina === 'historico' ? 'historico' : 'home');

  await comTratamento(async () => {
    if (pagina === 'historico') await telaHistorico();
    else if (pagina === 'lista' && id && subpagina === 'adicionar') await telaAdicionar(id);
    else if (pagina === 'lista' && id) await telaDetalhe(id);
    else await telaHome();
  });
}

addEventListener('hashchange', () => {
  window.scrollTo(0, 0);
  rotear();
});

rotear();