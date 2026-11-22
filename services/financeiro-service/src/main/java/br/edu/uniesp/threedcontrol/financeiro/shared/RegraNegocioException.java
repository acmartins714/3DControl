package br.edu.uniesp.threedcontrol.financeiro.shared;

/** Violação de uma regra de negócio. Respondida como HTTP 422 pelo {@link GlobalExceptionHandler}. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String message) {
        super(message);
    }
}
