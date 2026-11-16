"""
Teste ponta a ponta do 3D Control, executado através do api-gateway.

Percorre o ciclo de vida completo de um pedido e verifica as regras de negócio centrais:
falta de insumo -> solicitação de compra -> reprocessamento -> alteração antes da produção ->
Regra de Ouro -> baixa de estoque -> entrega bloqueada sem quitação -> rastreio -> lucratividade.

Pré-requisito: ambiente no ar (`docker compose up -d --build`).
Uso:          python scripts/e2e/fluxo_completo.py   [variável opcional: API_BASE_URL]

Somente biblioteca padrão do Python 3. Pode ser executado várias vezes: cada execução usa
dados novos (sufixo aleatório), sem interferir nas anteriores.
"""
import json
import os
import sys
import urllib.error
import urllib.request
import uuid
from datetime import date, timedelta

BASE = os.environ.get("API_BASE_URL", "http://localhost:8080/api").rstrip("/")
SUFIXO = uuid.uuid4().hex[:6].upper()
falhas = []

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")


def req(metodo, caminho, corpo=None):
    dados = json.dumps(corpo).encode() if corpo is not None else None
    r = urllib.request.Request(BASE + caminho, data=dados, method=metodo,
                               headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(r, timeout=60) as resp:
            texto = resp.read().decode()
            return resp.status, (json.loads(texto) if texto else None)
    except urllib.error.HTTPError as e:
        texto = e.read().decode()
        try:
            return e.code, json.loads(texto)
        except ValueError:
            return e.code, texto


def check(nome, condicao, detalhe=""):
    print(("OK   " if condicao else "FALHA ") + nome + ("" if condicao else f"\n      -> {detalhe}"))
    if not condicao:
        falhas.append(nome)
    return condicao


def exigir(nome, condicao, detalhe=""):
    """Como check(), mas interrompe o teste: os passos seguintes dependem deste."""
    if not check(nome, condicao, detalhe):
        print(f"\nInterrompido: '{nome}' é pré-requisito dos próximos passos.")
        sys.exit(1)


def main():
    print(f"API: {BASE}  |  execução: {SUFIXO}\n")
    categoria = f"Chaveiros-{SUFIXO}"

    # 1. Cadastros base -------------------------------------------------------------------------
    s, filamento = req("POST", "/insumos", {"nome": f"Filamento PLA {SUFIXO}", "unidadeMedida": "KG",
                                             "custoUnitario": 120.00, "usoRegular": True})
    exigir("cria insumo filamento (201)", s == 201, filamento)
    s, argola = req("POST", "/insumos", {"nome": f"Argola metálica {SUFIXO}", "unidadeMedida": "UN",
                                          "custoUnitario": 0.50, "usoRegular": False})
    exigir("cria insumo argola (201)", s == 201, argola)

    s, produto = req("POST", "/produtos", {
        "nome": "Chaveiro 3D personalizado", "categoria": categoria, "tempoMedioFabricacaoHoras": 2,
        "precoBase": 15.00,
        "composicao": [{"insumoId": filamento["id"], "quantidade": 0.05},
                       {"insumoId": argola["id"], "quantidade": 1}]})
    exigir("cria produto com ficha técnica (201)", s == 201, produto)
    check("custo estimado = 0,05 kg x 120 + 1 x 0,50 = 6,50", float(produto["custoEstimado"]) == 6.50, produto)

    s, cliente = req("POST", "/clientes", {"nome": f"Maria Souza {SUFIXO}", "telefone": "83999990000",
                                            "canalPreferencial": "WHATSAPP"})
    exigir("cria cliente (201)", s == 201, cliente)

    # 2. Estoque: filamento suficiente, argolas insuficientes (2 para 5 chaveiros) ---------------
    for insumo, nome, unidade in ((filamento, "Filamento PLA", "KG"), (argola, "Argola metálica", "UN")):
        s, r = req("POST", "/estoque", {"insumoId": insumo["id"], "nomeInsumo": nome,
                                        "unidadeMedida": unidade, "estoqueMinimo": 0})
        exigir(f"habilita controle de estoque de {nome}", s == 201, r)
    check("entrada de 1 kg de filamento", req("POST", f"/estoque/{filamento['id']}/entradas", {"quantidade": 1})[0] == 200)
    check("entrada de 2 argolas", req("POST", f"/estoque/{argola['id']}/entradas", {"quantidade": 2})[0] == 200)

    # 3. Pedido via WhatsApp sem insumo suficiente ---------------------------------------------
    prazo = (date.today() + timedelta(days=5)).isoformat()
    s, pedido = req("POST", "/pedidos", {
        "clienteId": cliente["id"], "canalOrigem": "WHATSAPP", "atendenteResponsavel": "Ana",
        "prazoEntrega": prazo, "modalidadeEntrega": "ENTREGA", "condicaoPagamento": "SINAL_E_RESTANTE_NA_ENTREGA",
        "itens": [{"produtoId": produto["id"], "quantidade": 5, "personalizacao": "Nome: Maria"}]})
    exigir("registra pedido (201)", s == 201, pedido)
    pid = pedido["id"]
    check("pedido fica AGUARDANDO_INSUMO (faltam argolas)", pedido["status"] == "AGUARDANDO_INSUMO", pedido["status"])
    check("valor total = 5 x 15 = 75", float(pedido["valorTotal"]) == 75.0, pedido["valorTotal"])

    s, sols = req("GET", "/solicitacoes-compra?status=PENDENTE")
    sol = [x for x in sols if x["pedidoId"] == pid]
    exigir("solicitação de compra aberta para as 3 argolas faltantes",
           len(sol) == 1 and float(sol[0]["quantidade"]) == 3, sols)

    s, fila = req("GET", "/producao/fila")
    ordem = next((o for o in fila if o["pedidoId"] == pid), None)
    exigir("ordem entra na fila marcada sem insumos", ordem is not None and ordem["insumosDisponiveis"] is False, fila)
    s, r = req("POST", f"/producao/ordens/{ordem['id']}/inicio", {"responsavel": "Carlos"})
    check("produção NÃO inicia sem insumos (422)", s == 422, r)

    # 4. Compra chega -> reprocessa -> reserva ------------------------------------------------
    s, r = req("POST", f"/solicitacoes-compra/{sol[0]['id']}/recebimento", {"quantidadeRecebida": 10})
    check("registra recebimento da compra (10 argolas)", s == 200, r)
    s, pedido = req("POST", f"/pedidos/{pid}/reprocessamento-insumos")
    exigir("após reprocessar, pedido volta a EM_TRIAGEM", s == 200 and pedido["status"] == "EM_TRIAGEM", pedido)
    s, arg = req("GET", f"/estoque/{argola['id']}")
    check("argolas: 12 físicas, 5 reservadas",
          float(arg["quantidadeDisponivel"]) == 12 and float(arg["quantidadeReservada"]) == 5, arg)

    # 5. Alteração solicitada pelo cliente antes da produção -> permitida ---------------------
    s, pedido = req("PUT", f"/pedidos/{pid}", {
        "prazoEntrega": prazo, "modalidadeEntrega": "ENTREGA", "condicaoPagamento": "SINAL_E_RESTANTE_NA_ENTREGA",
        "observacoes": "Cliente pediu cor azul",
        "itens": [{"produtoId": produto["id"], "quantidade": 4, "personalizacao": "Nome: Maria (AZUL)"}]})
    exigir("alteração antes da produção é aceita", s == 200 and float(pedido["valorTotal"]) == 60.0, pedido)
    s, arg = req("GET", f"/estoque/{argola['id']}")
    check("reserva refeita: 4 argolas reservadas", float(arg["quantidadeReservada"]) == 4, arg)
    s, conta = req("GET", f"/financeiro/contas/pedido/{pid}")
    check("conta a receber atualizada para R$ 60", float(conta["valorTotal"]) == 60.0, conta)

    # 6. Produção inicia -> pedido bloqueado (Regra de Ouro) ----------------------------------
    s, fila = req("GET", "/producao/fila")
    ordem = next(o for o in fila if o["pedidoId"] == pid)
    s, ordem = req("POST", f"/producao/ordens/{ordem['id']}/inicio", {"responsavel": "Carlos"})
    exigir("produção iniciada", s == 200 and ordem["status"] == "EM_PRODUCAO", ordem)
    s, pedido = req("GET", f"/pedidos/{pid}")
    check("pedido EM_PRODUCAO e bloqueado para alteração",
          pedido["status"] == "EM_PRODUCAO" and pedido["permiteAlteracao"] is False, pedido)
    s, arg = req("GET", f"/estoque/{argola['id']}")
    check("baixa definitiva: 8 argolas físicas, 0 reservadas",
          float(arg["quantidadeDisponivel"]) == 8 and float(arg["quantidadeReservada"]) == 0, arg)

    s, r = req("PUT", f"/pedidos/{pid}", {
        "prazoEntrega": prazo, "modalidadeEntrega": "ENTREGA", "condicaoPagamento": "A_VISTA",
        "itens": [{"produtoId": produto["id"], "quantidade": 1}]})
    check("REGRA DE OURO: alteração após início da produção é recusada (422)", s == 422, r)

    s, r = req("POST", f"/producao/ordens/{ordem['id']}/conclusao")
    exigir("produção concluída", s == 200, r)
    s, pedido = req("POST", f"/pedidos/{pid}/despacho")
    exigir("pedido despachado (EM_ROTA_ENTREGA)", s == 200 and pedido["status"] == "EM_ROTA_ENTREGA", pedido)

    # 7. Entrega condicionada à quitação -------------------------------------------------------
    s, r = req("POST", f"/pedidos/{pid}/entrega")
    check("entrega BLOQUEADA sem pagamento (422)", s == 422, r)
    s, conta = req("POST", f"/financeiro/contas/pedido/{pid}/pagamentos", {"valor": 20, "forma": "PIX"})
    check("sinal de R$ 20 registrado como SINAL", s == 200 and conta["pagamentos"][0]["tipo"] == "SINAL", conta)
    s, r = req("POST", f"/pedidos/{pid}/entrega")
    check("entrega ainda bloqueada com saldo de R$ 40 (422)", s == 422 and "40" in str(r.get("detail", "")), r)
    s, r = req("POST", f"/financeiro/contas/pedido/{pid}/pagamentos", {"valor": 50, "forma": "DINHEIRO"})
    check("pagamento acima do saldo é recusado (422)", s == 422, r)
    s, conta = req("POST", f"/financeiro/contas/pedido/{pid}/pagamentos", {"valor": 40, "forma": "DINHEIRO"})
    check("quitação registrada (conta QUITADA)",
          s == 200 and conta["status"] == "QUITADA" and conta["pagamentos"][1]["tipo"] == "QUITACAO", conta)
    s, pedido = req("POST", f"/pedidos/{pid}/entrega")
    exigir("entrega liberada após quitação (ENTREGUE)", s == 200 and pedido["status"] == "ENTREGUE", pedido)

    # 8. Rastreio e lucratividade --------------------------------------------------------------
    s, rast = req("GET", f"/rastreio/{pedido['codigo'].lower()}")
    check("rastreio por código (sem diferenciar maiúsculas)", s == 200 and rast["status"] == "ENTREGUE", rast)
    check("linha do tempo com todas as etapas (>= 8)", s == 200 and len(rast["linhaDoTempo"]) >= 8, rast)
    s, luc = req("GET", "/financeiro/lucratividade")
    ch = next((x for x in luc if x["categoria"] == categoria), None)
    check("lucratividade: receita 60, custo 26, margem 56,67%",
          ch is not None and float(ch["receita"]) == 60 and float(ch["custo"]) == 26
          and float(ch["margemPercentual"]) == 56.67, luc)

    # 9. Erros padronizados (RFC 9457) --------------------------------------------------------
    s, r = req("GET", "/pedidos/00000000-0000-0000-0000-000000000000")
    check("404 Problem Details para pedido inexistente", s == 404 and r.get("title") == "Recurso não encontrado", r)
    s, r = req("POST", "/clientes", {"nome": ""})
    check("400 com a lista de campos inválidos", s == 400 and "campos" in r, r)

    print(f"\nCódigo do pedido testado: {pedido['codigo']}")


if __name__ == "__main__":
    try:
        main()
    except urllib.error.URLError as e:
        print(f"Não foi possível acessar {BASE}: {e.reason}. O ambiente está no ar (docker compose up -d)?")
        sys.exit(2)
    print("RESULTADO:", "TODOS OS PASSOS OK" if not falhas else f"{len(falhas)} FALHA(S): {falhas}")
    sys.exit(1 if falhas else 0)
