// Comunicação com a API, sessão (token JWT) e utilitários compartilhados pelas páginas.

const CHAVE_SESSAO = 'financas.sessao';

function obterSessao() {
  try {
    return JSON.parse(localStorage.getItem(CHAVE_SESSAO));
  } catch {
    return null;
  }
}

function salvarSessao(sessao) {
  localStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessao));
}

function sair() {
  localStorage.removeItem(CHAVE_SESSAO);
  window.location.href = 'index.html';
}

class ErroApi extends Error {
  constructor(status, corpo) {
    super(corpo?.mensagem || 'Erro ao falar com o servidor');
    this.status = status;
    this.campos = corpo?.campos || {};
  }
}

/**
 * Faz uma requisição à API com o token da sessão. Aceita objeto (vira JSON) ou FormData.
 * Em 401 a sessão expirou: volta pra tela de login.
 */
async function api(caminho, { metodo = 'GET', corpo } = {}) {
  const headers = {};
  const sessao = obterSessao();
  if (sessao?.token) {
    headers.Authorization = `Bearer ${sessao.token}`;
  }

  let body;
  if (corpo instanceof FormData) {
    body = corpo;
  } else if (corpo !== undefined) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(corpo);
  }

  let resposta;
  try {
    resposta = await fetch(API_URL + caminho, { method: metodo, headers, body });
  } catch {
    // O navegador não diferencia API fora do ar de bloqueio por CORS: detalhes no console (F12)
    throw new ErroApi(0, {
      mensagem: 'Não foi possível conectar à API. Ela pode estar iniciando (aguarde ~1 minuto) '
        + 'ou este site não está liberado em CORS_ORIGENS.',
    });
  }

  if (resposta.status === 401 && caminho !== '/auth/login') {
    sair();
    throw new ErroApi(401, { mensagem: 'Sessão expirada' });
  }

  const texto = await resposta.text();
  const dados = texto ? JSON.parse(texto) : null;
  if (!resposta.ok) {
    throw new ErroApi(resposta.status, dados);
  }
  return dados;
}

// ---------- Formatação ----------

const formatoMoeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

function moeda(valor) {
  return formatoMoeda.format(Number(valor));
}

// "2026-10-02" -> "02/10/2026" (sem passar por Date, pra não sofrer com fuso horário)
function dataBR(iso) {
  if (!iso) return '';
  const [ano, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${ano}`;
}

function hojeISO() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function mesAtualISO() {
  return hojeISO().slice(0, 7);
}

// "2026-10" -> { inicio: "2026-10-01", fim: "2026-10-31" }
function periodoDoMes(mes) {
  const [ano, m] = mes.split('-').map(Number);
  const ultimoDia = new Date(ano, m, 0).getDate();
  return { inicio: `${mes}-01`, fim: `${mes}-${String(ultimoDia).padStart(2, '0')}` };
}

function nomeDoMes(mes) {
  const [ano, m] = mes.split('-').map(Number);
  const nome = new Date(ano, m - 1, 1).toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
  return nome.charAt(0).toUpperCase() + nome.slice(1);
}

// Escapa texto vindo da API (ex.: descrições de extrato) antes de montar HTML
function esc(texto) {
  return String(texto ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  })[c]);
}

// ---------- Feedback ----------

function toast(mensagem, tipo = '') {
  const el = document.createElement('div');
  el.className = `toast ${tipo}`;
  el.setAttribute('role', 'status');
  el.textContent = mensagem;
  document.body.appendChild(el);
  setTimeout(() => el.remove(), 3500);
}

function mostrarErro(erro) {
  const detalhes = Object.values(erro.campos || {});
  toast(detalhes.length ? `${erro.message}: ${detalhes.join(', ')}` : erro.message, 'erro');
}

// Desabilita o botão enquanto a ação roda, evitando clique duplo
async function comCarregamento(botao, acao) {
  botao.disabled = true;
  try {
    return await acao();
  } finally {
    botao.disabled = false;
  }
}
