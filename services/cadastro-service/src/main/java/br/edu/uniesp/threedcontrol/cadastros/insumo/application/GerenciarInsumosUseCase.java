package br.edu.uniesp.threedcontrol.cadastros.insumo.application;

import br.edu.uniesp.threedcontrol.cadastros.insumo.domain.Insumo;
import br.edu.uniesp.threedcontrol.cadastros.insumo.domain.InsumoRepository;
import br.edu.uniesp.threedcontrol.cadastros.shared.domain.RecursoNaoEncontradoException;

import java.util.List;
import java.util.UUID;

public class GerenciarInsumosUseCase {

    private final InsumoRepository repository;

    public GerenciarInsumosUseCase(InsumoRepository repository) {
        this.repository = repository;
    }

    public Insumo cadastrar(InsumoCommand cmd) {
        return repository.salvar(Insumo.novo(cmd.nome(), cmd.descricao(), cmd.unidadeMedida(),
                cmd.custoUnitario(), cmd.usoRegular()));
    }

    public Insumo atualizar(UUID id, InsumoCommand cmd) {
        Insumo insumo = buscar(id);
        insumo.atualizar(cmd.nome(), cmd.descricao(), cmd.unidadeMedida(), cmd.custoUnitario(), cmd.usoRegular());
        return repository.salvar(insumo);
    }

    public Insumo buscar(UUID id) {
        return repository.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Insumo", id));
    }

    public List<Insumo> listar() {
        return repository.listar();
    }

    public void inativar(UUID id) {
        Insumo insumo = buscar(id);
        insumo.inativar();
        repository.salvar(insumo);
    }
}
