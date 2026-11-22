package br.edu.uniesp.threedcontrol.producao.shared;

/** Falha de comunicação com outro microsserviço. Respondida como HTTP 503 pelo {@link GlobalExceptionHandler}. */
public class IntegracaoException extends RuntimeException {

    public IntegracaoException(String servico, Throwable causa) {
        super("Falha na comunicação com o serviço '" + servico + "': " + causa.getMessage(), causa);
    }
}
