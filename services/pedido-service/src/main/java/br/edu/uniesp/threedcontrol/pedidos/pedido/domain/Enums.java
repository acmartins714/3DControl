package br.edu.uniesp.threedcontrol.pedidos.pedido.domain;

/** Enumerações simples do contexto de pedidos. */
public final class Enums {

    private Enums() {
    }

    /** Canal pelo qual o pedido chegou (atendimento omnicanal). */
    public enum CanalAtendimento { WHATSAPP, INSTAGRAM, FACEBOOK, TELEFONE, SITE, PRESENCIAL }

    public enum ModalidadeEntrega { RETIRADA, ENTREGA }

    public enum CondicaoPagamento { A_VISTA, SINAL_E_RESTANTE_NA_ENTREGA, PARCELADO, PAGAMENTO_NA_ENTREGA }
}
