package br.edu.uniesp.threedcontrol.cadastros.insumo;

import br.edu.uniesp.threedcontrol.cadastros.insumo.InsumoDtos.InsumoRequest;
import br.edu.uniesp.threedcontrol.cadastros.insumo.InsumoDtos.InsumoResponse;
import br.edu.uniesp.threedcontrol.cadastros.shared.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InsumoService {

    private final InsumoRepository repository;

    public InsumoService(InsumoRepository repository) {
        this.repository = repository;
    }

    public InsumoResponse cadastrar(InsumoRequest req) {
        Insumo insumo = new Insumo(req.nome(), req.descricao(), req.unidadeMedida(),
                req.custoUnitario(), req.usoRegular());
        return InsumoResponse.from(repository.save(insumo));
    }

    public InsumoResponse atualizar(UUID id, InsumoRequest req) {
        Insumo insumo = buscarEntidade(id);
        insumo.atualizar(req.nome(), req.descricao(), req.unidadeMedida(), req.custoUnitario(), req.usoRegular());
        return InsumoResponse.from(insumo);
    }

    @Transactional(readOnly = true)
    public InsumoResponse buscar(UUID id) {
        return InsumoResponse.from(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<InsumoResponse> listar() {
        return repository.findAllByOrderByNome().stream().map(InsumoResponse::from).toList();
    }

    public void inativar(UUID id) {
        buscarEntidade(id).inativar();
    }

    private Insumo buscarEntidade(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Insumo", id));
    }
}
