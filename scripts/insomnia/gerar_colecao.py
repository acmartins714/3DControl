"""
Gera a coleção do Insomnia (formato de exportação v4) com todos os endpoints do 3D Control.

Uso:  python scripts/insomnia/gerar_colecao.py
Saída: docs/insomnia/3dcontrol-insomnia.json  (Insomnia → Import → arquivo)

As requisições estão em pastas numeradas na ordem do fluxo de negócio. Os IDs são encadeados por
template tags de resposta do Insomnia: por exemplo, "Registrar pedido" usa o id de "Cadastrar cliente".
Basta executar as requisições na ordem (cada uma usa a última resposta da requisição de origem).
"""
import base64
import json
import pathlib

SAIDA = pathlib.Path(__file__).resolve().parents[2] / "docs" / "insomnia" / "3dcontrol-insomnia.json"
WS = "wrk_3dcontrol"
recursos = []
ordem = [0]


def ref(req_id, jsonpath):
    """Template tag que lê um campo (JSONPath) da última resposta de outra requisição."""
    filtro = base64.b64encode(jsonpath.encode()).decode()
    return "{% response 'body', '" + req_id + "', 'b64::" + filtro + "::46b', 'never', 60 %}"


def pasta(pid, nome, descricao="", pai=WS):
    ordem[0] += 1
    recursos.append({"_id": pid, "_type": "request_group", "parentId": pai, "name": nome,
                     "description": descricao, "metaSortKey": ordem[0], "environment": {}})
    return pid


def req(rid, pai, nome, metodo, url, corpo=None, descricao=""):
    ordem[0] += 1
    r = {"_id": rid, "_type": "request", "parentId": pai, "name": nome, "description": descricao,
         "method": metodo, "url": "{{ _.base_url }}" + url, "parameters": [], "authentication": {},
         "metaSortKey": ordem[0], "settingFollowRedirects": "global", "headers": [], "body": {}}
    if corpo is not None:
        r["body"] = {"mimeType": "application/json", "text": json.dumps(corpo, ensure_ascii=False, indent=2)}
        r["headers"] = [{"name": "Content-Type", "value": "application/json"}]
    recursos.append(r)


# Referências usadas em vários lugares
CLIENTE = ref("req_cliente_criar", "$.id")
FILAMENTO = ref("req_insumo_filamento", "$.id")
ARGOLA = ref("req_insumo_argola", "$.id")
PRODUTO = ref("req_produto_criar", "$.id")
PEDIDO = ref("req_pedido_registrar", "$.id")
CODIGO = ref("req_pedido_registrar", "$.codigo")
SOLICITACAO = ref("req_solicitacoes_pendentes", "$[0].id")
ORDEM = ref("req_producao_ordem_pedido", "$.id")

recursos.append({"_id": WS, "_type": "workspace", "name": "3D Control",
                 "description": "API do Sistema de Controle de Vendas 3D Control (via api-gateway).",
                 "scope": "collection"})
recursos.append({"_id": "env_base", "_type": "environment", "parentId": WS, "name": "Base Environment",
                 "data": {"base_url": "http://localhost:8080/api", "prazo_entrega": "2026-12-20"},
                 "dataPropertyOrder": {"&": ["base_url", "prazo_entrega"]}, "isPrivate": False})
recursos.append({"_id": "env_gateway", "_type": "environment", "parentId": "env_base",
                 "name": "Local (api-gateway :8080)", "data": {"base_url": "http://localhost:8080/api"},
                 "dataPropertyOrder": {"&": ["base_url"]}, "isPrivate": False, "color": "#7d69cb"})

# ------------------------------------------------------------------ 1. Cadastros
p = pasta("fld_cadastros", "1. Cadastros (cadastro-service)")
c = pasta("fld_clientes", "Clientes", pai=p)
req("req_cliente_criar", c, "Cadastrar cliente", "POST", "/clientes", {
    "nome": "Maria Souza", "telefone": "83999990000",
    "email": "maria@example.com", "canalPreferencial": "WHATSAPP",
    "preferenciasPersonalizacao": "Gosta de tons pastel"},
    descricao="O campo opcional \"documento\" (CPF/CNPJ) é único: inclua-o com um valor novo a cada execução.")
req("req_cliente_listar", c, "Listar clientes (filtro por nome)", "GET", "/clientes?nome=maria")
req("req_cliente_buscar", c, "Buscar cliente", "GET", "/clientes/" + CLIENTE)
req("req_cliente_atualizar", c, "Atualizar cliente", "PUT", "/clientes/" + CLIENTE, {
    "nome": "Maria Souza", "telefone": "83988887777",
    "email": "maria@example.com", "canalPreferencial": "INSTAGRAM",
    "preferenciasPersonalizacao": "Tons pastel; prefere retirada na loja"})
req("req_cliente_inativar", c, "Inativar cliente", "DELETE", "/clientes/" + CLIENTE,
    descricao="Inativa o cliente (exclusão lógica). Execute por último: cliente inativo não pode fazer pedidos.")

i = pasta("fld_insumos", "Insumos", pai=p)
req("req_insumo_filamento", i, "Cadastrar insumo — Filamento PLA", "POST", "/insumos", {
    "nome": "Filamento PLA", "descricao": "Filamento 1,75 mm", "unidadeMedida": "KG",
    "custoUnitario": 120.00, "usoRegular": True})
req("req_insumo_argola", i, "Cadastrar insumo — Argola (uso não regular)", "POST", "/insumos", {
    "nome": "Argola metálica", "unidadeMedida": "UN", "custoUnitario": 0.50, "usoRegular": False})
req("req_insumo_listar", i, "Listar insumos", "GET", "/insumos")
req("req_insumo_buscar", i, "Buscar insumo", "GET", "/insumos/" + FILAMENTO)
req("req_insumo_atualizar", i, "Atualizar insumo", "PUT", "/insumos/" + FILAMENTO, {
    "nome": "Filamento PLA", "descricao": "Filamento 1,75 mm — fornecedor B", "unidadeMedida": "KG",
    "custoUnitario": 115.00, "usoRegular": True})

pr = pasta("fld_produtos", "Produtos", pai=p)
req("req_produto_criar", pr, "Cadastrar produto com ficha técnica", "POST", "/produtos", {
    "nome": "Chaveiro 3D personalizado", "descricao": "Chaveiro impresso em 3D com nome",
    "categoria": "Chaveiros", "especificacoesTecnicas": "5 cm, PLA, argola metálica",
    "tempoMedioFabricacaoHoras": 2, "precoBase": 15.00,
    "composicao": [{"insumoId": FILAMENTO, "quantidade": 0.05}, {"insumoId": ARGOLA, "quantidade": 1}]},
    descricao="Requer os dois insumos cadastrados. A resposta traz o custo estimado calculado pela ficha técnica.")
req("req_produto_listar", pr, "Listar produtos (filtro por categoria)", "GET", "/produtos?categoria=Chaveiros")
req("req_produto_buscar", pr, "Buscar produto (com custo estimado)", "GET", "/produtos/" + PRODUTO)
req("req_produto_atualizar", pr, "Atualizar produto", "PUT", "/produtos/" + PRODUTO, {
    "nome": "Chaveiro 3D personalizado", "descricao": "Chaveiro impresso em 3D com nome ou logotipo",
    "categoria": "Chaveiros", "especificacoesTecnicas": "5 cm, PLA, argola metálica, acabamento fosco",
    "tempoMedioFabricacaoHoras": 2, "precoBase": 15.00,
    "composicao": [{"insumoId": FILAMENTO, "quantidade": 0.05}, {"insumoId": ARGOLA, "quantidade": 1}]},
    descricao="Mantém o preço base em R$ 15 para que os valores dos exemplos do financeiro fechem (4 x 15 = 60).")

# ------------------------------------------------------------------ 2. Estoque
e = pasta("fld_estoque", "2. Estoque (estoque-service)")
req("req_estoque_hab_filamento", e, "Habilitar estoque — Filamento", "POST", "/estoque", {
    "insumoId": FILAMENTO, "nomeInsumo": "Filamento PLA", "unidadeMedida": "KG", "estoqueMinimo": 0.5})
req("req_estoque_hab_argola", e, "Habilitar estoque — Argola", "POST", "/estoque", {
    "insumoId": ARGOLA, "nomeInsumo": "Argola metálica", "unidadeMedida": "UN", "estoqueMinimo": 0})
req("req_estoque_entrada_filamento", e, "Entrada — 1 kg de filamento", "POST",
    "/estoque/" + FILAMENTO + "/entradas", {"quantidade": 1, "observacao": "Compra NF 123"})
req("req_estoque_entrada_argola", e, "Entrada — 2 argolas (propositalmente pouco)", "POST",
    "/estoque/" + ARGOLA + "/entradas", {"quantidade": 2},
    descricao="Só 2 argolas: um pedido de 5 chaveiros ficará AGUARDANDO_INSUMO e gerará solicitação de compra.")
req("req_estoque_listar", e, "Listar saldos", "GET", "/estoque")
req("req_estoque_buscar", e, "Saldo de um insumo", "GET", "/estoque/" + ARGOLA)
req("req_estoque_alertas", e, "Alertas (abaixo do mínimo)", "GET", "/estoque/alertas")
req("req_estoque_parametros", e, "Atualizar estoque mínimo", "PUT", "/estoque/" + FILAMENTO, {
    "nomeInsumo": "Filamento PLA", "unidadeMedida": "KG", "estoqueMinimo": 0.8})
req("req_estoque_movimentacoes", e, "Movimentações de um insumo (auditoria)", "GET",
    "/estoque/" + ARGOLA + "/movimentacoes")

# ------------------------------------------------------------------ 3. Pedidos
pd = pasta("fld_pedidos", "3. Pedidos (pedido-service)",
           "Execute após 1 e 2. O prazo usa a variável de ambiente prazo_entrega (deve ser hoje ou futura).")
req("req_pedido_registrar", pd, "Registrar pedido (WhatsApp)", "POST", "/pedidos", {
    "clienteId": CLIENTE, "canalOrigem": "WHATSAPP", "atendenteResponsavel": "Ana",
    "prazoEntrega": "{{ _.prazo_entrega }}", "modalidadeEntrega": "ENTREGA",
    "condicaoPagamento": "SINAL_E_RESTANTE_NA_ENTREGA", "observacoes": "Entregar à tarde",
    "itens": [{"produtoId": PRODUTO, "quantidade": 5, "personalizacao": "Nome: Maria"}]},
    descricao="Com só 2 argolas em estoque, o pedido fica AGUARDANDO_INSUMO e abre uma solicitação de compra.")
req("req_pedido_site", pd, "Registrar pedido (site — sem atendente)", "POST", "/pedidos", {
    "clienteId": CLIENTE, "canalOrigem": "SITE", "prazoEntrega": "{{ _.prazo_entrega }}",
    "modalidadeEntrega": "RETIRADA", "condicaoPagamento": "A_VISTA",
    "itens": [{"produtoId": PRODUTO, "quantidade": 1, "precoUnitario": 18.00, "personalizacao": "Nome: Ana"}]},
    descricao="Pedidos do site dispensam atendente. precoUnitario é opcional (padrão: preço base do catálogo).")
req("req_pedido_listar", pd, "Listar pedidos (filtros status/clienteId)", "GET", "/pedidos?status=AGUARDANDO_INSUMO")
req("req_pedido_buscar", pd, "Buscar pedido", "GET", "/pedidos/" + PEDIDO)
req("req_pedido_alterar", pd, "Alterar pedido (antes da produção)", "PUT", "/pedidos/" + PEDIDO, {
    "prazoEntrega": "{{ _.prazo_entrega }}", "modalidadeEntrega": "ENTREGA",
    "condicaoPagamento": "SINAL_E_RESTANTE_NA_ENTREGA", "observacoes": "Cliente pediu cor azul",
    "itens": [{"produtoId": PRODUTO, "quantidade": 4, "personalizacao": "Nome: Maria (AZUL)"}]},
    descricao="Permitido enquanto o pedido está EM_TRIAGEM ou AGUARDANDO_INSUMO. Depois da produção → 422 (Regra de Ouro).")
req("req_pedido_reprocessar", pd, "Reprocessar insumos", "POST", "/pedidos/" + PEDIDO + "/reprocessamento-insumos",
    descricao="Após receber a compra (pasta 4), tenta reservar novamente → EM_TRIAGEM.")
req("req_pedido_despachar", pd, "Despachar (sair para entrega)", "POST", "/pedidos/" + PEDIDO + "/despacho",
    descricao="Requer pedido FINALIZADO e modalidade ENTREGA.")
req("req_pedido_entregar", pd, "Confirmar entrega", "POST", "/pedidos/" + PEDIDO + "/entrega",
    descricao="Responde 422 enquanto houver saldo a receber no financeiro.")
req("req_pedido_cancelar", pd, "Cancelar pedido", "POST", "/pedidos/" + PEDIDO + "/cancelamento",
    {"motivo": "Cliente desistiu"},
    descricao="Só antes da produção. Libera a reserva de estoque, cancela a ordem e a conta a receber.")

# ------------------------------------------------------------------ 4. Compras
sc = pasta("fld_compras", "4. Solicitações de compra (estoque-service)")
req("req_solicitacoes_pendentes", sc, "Listar solicitações pendentes do pedido", "GET",
    "/solicitacoes-compra?status=PENDENTE&pedidoId=" + PEDIDO,
    descricao="Sem o filtro pedidoId, lista as pendentes de todos os pedidos.")
req("req_solicitacao_receber", sc, "Registrar recebimento", "POST",
    "/solicitacoes-compra/" + SOLICITACAO + "/recebimento", {"quantidadeRecebida": 10},
    descricao="Usa a 1ª solicitação da listagem anterior. Dá entrada no estoque. Depois rode 'Reprocessar insumos' (pasta 3).")
req("req_solicitacao_cancelar", sc, "Cancelar solicitação", "POST",
    "/solicitacoes-compra/" + SOLICITACAO + "/cancelamento")

# ------------------------------------------------------------------ 5. Produção
po = pasta("fld_producao", "5. Produção (producao-service)",
           "As ações usam a ordem do pedido registrado na pasta 3 (requisição 'Ordem do pedido').")
req("req_producao_fila", po, "Fila de produção (priorizada)", "GET", "/producao/fila")
req("req_producao_ordem_pedido", po, "Ordem do pedido", "GET", "/producao/ordens/pedido/" + PEDIDO)
req("req_producao_buscar", po, "Buscar ordem por id", "GET", "/producao/ordens/" + ORDEM)
req("req_producao_iniciar", po, "Iniciar produção", "POST", "/producao/ordens/" + ORDEM + "/inicio",
    {"responsavel": "Carlos"},
    descricao="Pedido vai para EM_PRODUCAO (bloqueado para alterações) e os insumos têm baixa no estoque.")
req("req_producao_retrabalho", po, "Registrar retrabalho", "POST", "/producao/ordens/" + ORDEM + "/retrabalhos",
    {"motivo": "Falha na impressão da letra M"})
req("req_producao_concluir", po, "Concluir produção", "POST", "/producao/ordens/" + ORDEM + "/conclusao")

# ------------------------------------------------------------------ 6. Financeiro
f = pasta("fld_financeiro", "6. Financeiro (financeiro-service)")
req("req_fin_contas", f, "Listar contas a receber", "GET", "/financeiro/contas")
req("req_fin_conta_pedido", f, "Conta do pedido", "GET", "/financeiro/contas/pedido/" + PEDIDO)
req("req_fin_situacao", f, "Situação financeira do pedido", "GET", "/financeiro/contas/pedido/" + PEDIDO + "/situacao")
req("req_fin_sinal", f, "Registrar sinal (R$ 20 — PIX)", "POST", "/financeiro/contas/pedido/" + PEDIDO + "/pagamentos",
    {"valor": 20.00, "forma": "PIX", "observacao": "Sinal"})
req("req_fin_quitacao", f, "Registrar quitação (R$ 40 — dinheiro)", "POST",
    "/financeiro/contas/pedido/" + PEDIDO + "/pagamentos", {"valor": 40.00, "forma": "DINHEIRO"},
    descricao="Valor do pedido alterado (4 x 15 = 60) menos o sinal. Valores acima do saldo → 422.")
req("req_fin_lucratividade", f, "Lucratividade por categoria", "GET",
    "/financeiro/lucratividade?inicio=2026-01-01&fim=2026-12-31")

# ------------------------------------------------------------------ 7. Rastreio
r = pasta("fld_rastreio", "7. Rastreio (atendimento / site / WhatsApp)")
req("req_rastreio", r, "Rastrear pedido pelo código", "GET", "/rastreio/" + CODIGO)

# ------------------------------------------------------------------ 8. Integração interna
it = pasta("fld_interno", "8. Integração interna (chamadas entre serviços)",
           "Endpoints usados pelos próprios microsserviços. Úteis para depuração; no fluxo normal não precisam ser chamados à mão.")
req("req_int_reservar", it, "estoque ← pedido: reservar insumos", "POST", "/estoque/reservas", {
    "pedidoId": PEDIDO, "itens": [{"insumoId": ARGOLA, "quantidade": 1}]})
req("req_int_consumir", it, "estoque ← pedido: consumir reservas", "POST", "/estoque/reservas/" + PEDIDO + "/consumo")
req("req_int_liberar", it, "estoque ← pedido: liberar reservas", "DELETE", "/estoque/reservas/" + PEDIDO)
req("req_int_planejar", it, "produção ← pedido: planejar ordem", "POST", "/producao/ordens", {
    "pedidoId": PEDIDO, "codigoPedido": CODIGO, "prazoEntrega": "{{ _.prazo_entrega }}", "insumosDisponiveis": True})
req("req_int_cancelar_ordem", it, "produção ← pedido: cancelar ordem", "DELETE", "/producao/ordens/pedido/" + PEDIDO)
req("req_int_inicio", it, "pedido ← produção: início da produção", "POST", "/pedidos/" + PEDIDO + "/inicio-producao")
req("req_int_conclusao", it, "pedido ← produção: conclusão da produção", "POST", "/pedidos/" + PEDIDO + "/conclusao-producao")
req("req_int_abrir_conta", it, "financeiro ← pedido: abrir conta", "POST", "/financeiro/contas", {
    "pedidoId": PEDIDO, "clienteId": CLIENTE,
    "itens": [{"categoria": "Chaveiros", "valorVenda": 60.00, "custoEstimado": 26.00}]})
req("req_int_itens_conta", it, "financeiro ← pedido: atualizar itens da conta", "PUT",
    "/financeiro/contas/pedido/" + PEDIDO + "/itens",
    {"itens": [{"categoria": "Chaveiros", "valorVenda": 60.00, "custoEstimado": 26.00}]})
req("req_int_cancelar_conta", it, "financeiro ← pedido: cancelar conta", "DELETE", "/financeiro/contas/pedido/" + PEDIDO)

colecao = {"_type": "export", "__export_format": 4, "__export_source": "3dcontrol.gerar_colecao",
           "resources": recursos}
SAIDA.parent.mkdir(parents=True, exist_ok=True)
SAIDA.write_text(json.dumps(colecao, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"{sum(1 for x in recursos if x['_type'] == 'request')} requisicoes -> {SAIDA}")
