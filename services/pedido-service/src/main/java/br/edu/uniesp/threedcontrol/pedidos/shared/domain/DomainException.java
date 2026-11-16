package br.edu.uniesp.threedcontrol.pedidos.shared.domain;

/** Violação de uma regra de negócio. Não depende de nenhum framework. */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
