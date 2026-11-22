package br.edu.uniesp.threedcontrol.pedidos.pedido;

/**
 * Ciclo de vida do pedido e suas transições válidas (máquina de estados).
 * <p>
 * REGRA DE OURO DA PRODUÇÃO: alterações só são permitidas enquanto o pedido NÃO entrou em produção
 * ({@link #permiteAlteracao()}).
 */
public enum StatusPedido {
    EM_TRIAGEM("Em triagem", true),
    AGUARDANDO_INSUMO("Aguardando insumo", true),
    EM_PRODUCAO("Em produção", false),
    FINALIZADO("Finalizado", false),
    EM_ROTA_ENTREGA("Em rota de entrega", false),
    ENTREGUE("Entregue", false),
    CANCELADO("Cancelado", false);

    private final String descricao;
    private final boolean permiteAlteracao;

    StatusPedido(String descricao, boolean permiteAlteracao) {
        this.descricao = descricao;
        this.permiteAlteracao = permiteAlteracao;
    }

    public String descricao() {
        return descricao;
    }

    public boolean permiteAlteracao() {
        return permiteAlteracao;
    }

    public boolean podeTransitarPara(StatusPedido destino) {
        return switch (this) {
            case EM_TRIAGEM -> destino == AGUARDANDO_INSUMO || destino == EM_PRODUCAO || destino == CANCELADO;
            case AGUARDANDO_INSUMO -> destino == EM_TRIAGEM || destino == CANCELADO;
            case EM_PRODUCAO -> destino == FINALIZADO;
            case FINALIZADO -> destino == EM_ROTA_ENTREGA || destino == ENTREGUE;
            case EM_ROTA_ENTREGA -> destino == ENTREGUE;
            case ENTREGUE, CANCELADO -> false;
        };
    }
}
