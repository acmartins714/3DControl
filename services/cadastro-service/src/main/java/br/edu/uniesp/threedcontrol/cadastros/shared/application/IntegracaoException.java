package br.edu.uniesp.threedcontrol.cadastros.shared.application;

/** Falha de comunicação com outro microsserviço. */
public class IntegracaoException extends RuntimeException {
    public IntegracaoException(String servico, Throwable causa) {
        super("Falha na comunicação com o serviço '" + servico + "': " + causa.getMessage(), causa);
    }
}
