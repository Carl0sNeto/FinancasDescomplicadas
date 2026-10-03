// Cabeçalho comum das páginas internas. Sem sessão, manda pro login.

(function montarLayout() {
  const sessao = obterSessao();
  if (!sessao?.token) {
    window.location.href = 'index.html';
    return;
  }

  const paginas = [
    ['painel.html', 'Painel'],
    ['transacoes.html', 'Transações'],
    ['categorias.html', 'Categorias'],
    ['metas.html', 'Metas'],
    ['importar.html', 'Importar extrato'],
  ];
  // Só esconde o link: quem protege de verdade é a API (/admin/** exige ADMIN)
  if (sessao.papel === 'ADMIN') {
    paginas.push(['admin.html', 'Administração']);
  }
  const atual = window.location.pathname.split('/').pop() || 'painel.html';

  const topo = document.createElement('header');
  topo.className = 'topo';
  topo.innerHTML = `
    <div class="topo-conteudo">
      <a class="marca" href="painel.html">Finanças Descomplicadas</a>
      <nav class="menu" aria-label="Navegação principal">
        ${paginas.map(([href, nome]) =>
          `<a href="${href}" class="${href === atual ? 'ativo' : ''}">${nome}</a>`).join('')}
      </nav>
      <div class="usuario">
        <span>${esc(sessao.nome)}</span>
        <button type="button" class="secundario pequeno" id="botao-sair">Sair</button>
      </div>
    </div>`;
  document.body.prepend(topo);
  document.getElementById('botao-sair').addEventListener('click', sair);
})();
