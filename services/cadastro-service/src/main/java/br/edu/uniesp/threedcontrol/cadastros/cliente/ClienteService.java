package br.edu.uniesp.threedcontrol.cadastros.cliente;

import br.edu.uniesp.threedcontrol.cadastros.cliente.ClienteDtos.ClienteRequest;
import br.edu.uniesp.threedcontrol.cadastros.cliente.ClienteDtos.ClienteResponse;
import br.edu.uniesp.threedcontrol.cadastros.shared.RecursoNaoEncontradoException;
import br.edu.uniesp.threedcontrol.cadastros.shared.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    public ClienteResponse cadastrar(ClienteRequest req) {
        garantirDocumentoUnico(req.documento(), null);
        Cliente cliente = new Cliente(req.nome(), req.documento(), req.telefone(), req.email(),
                req.canalPreferencial(), req.preferenciasPersonalizacao());
        return ClienteResponse.from(repository.save(cliente));
    }

    public ClienteResponse atualizar(UUID id, ClienteRequest req) {
        Cliente cliente = buscarEntidade(id);
        garantirDocumentoUnico(req.documento(), id);
        cliente.atualizar(req.nome(), req.documento(), req.telefone(), req.email(),
                req.canalPreferencial(), req.preferenciasPersonalizacao());
        return ClienteResponse.from(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscar(UUID id) {
        return ClienteResponse.from(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(String nome) {
        List<Cliente> clientes = (nome == null || nome.isBlank())
                ? repository.findAllByOrderByNome()
                : repository.findByNomeContainingIgnoreCaseOrderByNome(nome);
        return clientes.stream().map(ClienteResponse::from).toList();
    }

    public void inativar(UUID id) {
        buscarEntidade(id).inativar();
    }

    private Cliente buscarEntidade(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
    }

    private void garantirDocumentoUnico(String documento, UUID idAtual) {
        if (documento == null || documento.isBlank()) {
            return;
        }
        boolean existe = idAtual == null
                ? repository.existsByDocumento(documento)
                : repository.existsByDocumentoAndIdNot(documento, idAtual);
        if (existe) {
            throw new RegraNegocioException("Já existe um cliente cadastrado com o documento " + documento + ".");
        }
    }
}
