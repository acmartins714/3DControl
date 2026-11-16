package br.edu.uniesp.threedcontrol.cadastros.cliente.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.edu.uniesp.threedcontrol.cadastros.cliente.domain.Cliente;
import br.edu.uniesp.threedcontrol.cadastros.cliente.domain.ClienteRepository;
import br.edu.uniesp.threedcontrol.cadastros.shared.domain.DomainException;
import br.edu.uniesp.threedcontrol.cadastros.shared.domain.RecursoNaoEncontradoException;

import java.util.List;
import java.util.UUID;

/** Casos de uso do cadastro de clientes. As dependências (portas do domínio) chegam pelo construtor. */
@Service
@Transactional
public class GerenciarClientesUseCase {

    private final ClienteRepository repository;

    public GerenciarClientesUseCase(ClienteRepository repository) {
        this.repository = repository;
    }

    public Cliente cadastrar(ClienteCommand cmd) {
        garantirDocumentoUnico(cmd.documento(), null);
        Cliente cliente = Cliente.novo(cmd.nome(), cmd.documento(), cmd.telefone(), cmd.email(),
                cmd.canalPreferencial(), cmd.preferenciasPersonalizacao());
        return repository.salvar(cliente);
    }

    public Cliente atualizar(UUID id, ClienteCommand cmd) {
        Cliente cliente = buscar(id);
        garantirDocumentoUnico(cmd.documento(), id);
        cliente.atualizar(cmd.nome(), cmd.documento(), cmd.telefone(), cmd.email(),
                cmd.canalPreferencial(), cmd.preferenciasPersonalizacao());
        return repository.salvar(cliente);
    }

    @Transactional(readOnly = true)
    public Cliente buscar(UUID id) {
        return repository.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar(String nome) {
        return repository.listar(nome);
    }

    public void inativar(UUID id) {
        Cliente cliente = buscar(id);
        cliente.inativar();
        repository.salvar(cliente);
    }

    private void garantirDocumentoUnico(String documento, UUID idAtual) {
        if (documento != null && !documento.isBlank() && repository.existeOutroComDocumento(documento, idAtual)) {
            throw new DomainException("Já existe um cliente cadastrado com o documento " + documento + ".");
        }
    }
}
