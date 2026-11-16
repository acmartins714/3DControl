"""
Teste de compensação (saga orquestrada) do registro de pedido.

Cenário: o financeiro-service está FORA DO AR quando um pedido é registrado. O pedido-service já
reservou insumos e incluiu a ordem na fila antes de falhar ao abrir a conta a receber; ele deve
desfazer esses efeitos e responder 503, sem gravar o pedido.

Uso:
    docker compose stop financeiro-service
    python scripts/e2e/compensacao.py          [variável opcional: API_BASE_URL]
    docker compose start financeiro-service

Somente biblioteca padrão do Python 3. Pode ser executado várias vezes.
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


def main():
    print(f"API: {BASE}  |  execução: {SUFIXO}\n")

    s, _ = req("GET", "/financeiro/contas")
    if s < 500:
        print("O financeiro-service está respondendo. Pare-o antes deste teste:\n"
              "    docker compose stop financeiro-service")
        sys.exit(2)

    # Preparação: insumo com saldo, produto e cliente próprios desta execução
    _, insumo = req("POST", "/insumos", {"nome": f"Resina {SUFIXO}", "unidadeMedida": "ML",
                                          "custoUnitario": 0.10, "usoRegular": True})
    _, produto = req("POST", "/produtos", {"nome": "Miniatura", "categoria": f"Miniaturas-{SUFIXO}",
                                            "tempoMedioFabricacaoHoras": 3, "precoBase": 40,
                                            "composicao": [{"insumoId": insumo["id"], "quantidade": 30}]})
    _, cliente = req("POST", "/clientes", {"nome": f"João {SUFIXO}", "email": "joao@example.com",
                                            "canalPreferencial": "PRESENCIAL"})
    req("POST", "/estoque", {"insumoId": insumo["id"], "nomeInsumo": "Resina", "unidadeMedida": "ML",
                             "estoqueMinimo": 0})
    req("POST", f"/estoque/{insumo['id']}/entradas", {"quantidade": 500})
    pedidos_antes = len(req("GET", f"/pedidos?clienteId={cliente['id']}")[1])
    fila_antes = len(req("GET", "/producao/fila")[1])

    # Ação: registra o pedido com o financeiro fora do ar
    s, r = req("POST", "/pedidos", {
        "clienteId": cliente["id"], "canalOrigem": "PRESENCIAL", "atendenteResponsavel": "Ana",
        "prazoEntrega": (date.today() + timedelta(days=3)).isoformat(), "modalidadeEntrega": "RETIRADA",
        "condicaoPagamento": "A_VISTA", "itens": [{"produtoId": produto["id"], "quantidade": 2}]})

    # Verificações
    check("registro falha com 503 (serviço dependente indisponível)", s == 503, (s, r))
    pedidos_depois = len(req("GET", f"/pedidos?clienteId={cliente['id']}")[1])
    check("nenhum pedido gravado", pedidos_antes == pedidos_depois == 0, (pedidos_antes, pedidos_depois))
    _, item = req("GET", f"/estoque/{insumo['id']}")
    check("reserva de 60 ml desfeita (reservado = 0, saldo livre = 500)",
          float(item["quantidadeReservada"]) == 0 and float(item["saldoLivre"]) == 500, item)
    _, movs = req("GET", f"/estoque/{insumo['id']}/movimentacoes")
    tipos = sorted(m["tipo"] for m in movs)
    check("auditoria registra a reserva E a liberação", tipos == ["ENTRADA", "LIBERACAO_RESERVA", "RESERVA"], tipos)
    fila_depois = len(req("GET", "/producao/fila")[1])
    check("ordem criada foi cancelada (fila de produção inalterada)", fila_antes == fila_depois,
          (fila_antes, fila_depois))


if __name__ == "__main__":
    try:
        main()
    except urllib.error.URLError as e:
        print(f"Não foi possível acessar {BASE}: {e.reason}. O ambiente está no ar (docker compose up -d)?")
        sys.exit(2)
    print("RESULTADO:", "TODOS OS PASSOS OK" if not falhas else f"{len(falhas)} FALHA(S): {falhas}")
    sys.exit(1 if falhas else 0)
