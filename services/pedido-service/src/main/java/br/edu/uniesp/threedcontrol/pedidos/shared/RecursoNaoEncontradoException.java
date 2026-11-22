package br.edu.uniesp.threedcontrol.pedidos.shared;

/** Registro inexistente. Respondido como HTTP 404 pelo {@link GlobalExceptionHandler}. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(recurso + " não encontrado(a): " + id);
    }
}
