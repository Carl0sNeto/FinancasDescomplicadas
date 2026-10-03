const campoMes = document.getElementById('mes');
campoMes.value = mesAtualISO();
campoMes.addEventListener('change', carregar);

async function carregar() {
  const mes = campoMes.value || mesAtualISO();
  const { inicio, fim } = periodoDoMes(mes);
  document.getElementById('titulo-mes').textContent = nomeDoMes(mes);

  try {
    const [resumo, transacoes, metas] = await Promise.all([
      api(`/transacoes/resumo?inicio=${inicio}&fim=${fim}`),
      api(`/transacoes?inicio=${inicio}&fim=${fim}`),
      api('/metas'),
    ]);
    mostrarResumo(resumo);
    mostrarGastos(resumo.despesasPorCategoria, resumo.despesas);
    mostrarMetas(metas);
    mostrarUltimas(transacoes.slice(0, 6));
  } catch (erro) {
    mostrarErro(erro);
  }
}

function mostrarResumo(resumo) {
  document.getElementById('total-receitas').textContent = moeda(resumo.receitas);
  document.getElementById('total-despesas').textContent = moeda(resumo.despesas);

  const saldo = document.getElementById('saldo');
  saldo.textContent = moeda(resumo.saldo);
  saldo.className = `numero ${Number(resumo.saldo) < 0 ? 'despesa' : 'receita'}`;

  const aviso = document.getElementById('aviso-pendentes');
  aviso.hidden = resumo.pendentes === 0;
  aviso.textContent = resumo.pendentes === 1
    ? '1 transação está sem categoria. Clique para revisar.'
    : `${resumo.pendentes} transações estão sem categoria. Clique para revisar.`;
}

function mostrarGastos(categorias, totalDespesas) {
  const alvo = document.getElementById('gastos-categoria');
  if (categorias.length === 0) {
    alvo.innerHTML = '<p class="vazio">Nenhuma despesa neste mês.</p>';
    return;
  }

  const total = Number(totalDespesas) || 1;
  alvo.innerHTML = categorias.map((c) => {
    const percentual = (Number(c.total) / total) * 100;
    return `
      <div class="barra-item">
        <div class="barra-titulo">
          <span>${esc(c.categoriaNome ?? 'Sem categoria')}</span>
          <span>${moeda(c.total)} · ${percentual.toFixed(0)}%</span>
        </div>
        <div class="barra despesa"><div style="width: ${percentual}%"></div></div>
      </div>`;
  }).join('');
}

function mostrarMetas(metas) {
  const alvo = document.getElementById('metas');
  if (metas.length === 0) {
    alvo.innerHTML = '<p class="vazio">Nenhuma meta ainda. <a href="metas.html">Criar uma meta</a></p>';
    return;
  }

  alvo.innerHTML = metas.slice(0, 4).map((m) => `
    <div class="barra-item">
      <div class="barra-titulo">
        <span>${esc(m.titulo)}</span>
        <span>${moeda(m.valorAtual)} de ${moeda(m.valorAlvo)}</span>
      </div>
      <div class="barra ${m.concluida ? 'concluida' : ''}"><div style="width: ${m.percentual}%"></div></div>
    </div>`).join('');
}

function mostrarUltimas(transacoes) {
  const alvo = document.getElementById('ultimas');
  if (transacoes.length === 0) {
    alvo.innerHTML = '<tr><td class="vazio">Nenhuma transação neste mês.</td></tr>';
    return;
  }

  alvo.innerHTML = transacoes.map((t) => `
    <tr>
      <td class="data">${dataBR(t.data)}</td>
      <td>${esc(t.descricao)}</td>
      <td class="opcional">${t.pendente
        ? '<span class="etiqueta pendente">Sem categoria</span>'
        : `<span class="etiqueta">${esc(t.categoriaNome)}</span>`}</td>
      <td class="valor ${t.tipo === 'RECEITA' ? 'receita' : 'despesa'}">${moeda(t.valor)}</td>
    </tr>`).join('');
}

carregar();
