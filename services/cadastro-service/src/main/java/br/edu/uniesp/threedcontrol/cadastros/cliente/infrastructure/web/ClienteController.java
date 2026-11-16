package br.edu.uniesp.threedcontrol.cadastros.cliente.infrastructure.web;

import br.edu.uniesp.threedcontrol.cadastros.cliente.application.GerenciarClientesUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clientes")
class ClienteController {

    private final GerenciarClientesUseCase useCase;

    ClienteController(GerenciarClientesUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ClienteResponse cadastrar(@Valid @RequestBody ClienteRequest request) {
        return ClienteResponse.from(useCase.cadastrar(request.toCommand()));
    }

    @GetMapping
    List<ClienteResponse> listar(@RequestParam(required = false) String nome) {
        return useCase.listar(nome).stream().map(ClienteResponse::from).toList();
    }

    @GetMapping("/{id}")
    ClienteResponse buscar(@PathVariable UUID id) {
        return ClienteResponse.from(useCase.buscar(id));
    }

    @PutMapping("/{id}")
    ClienteResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ClienteRequest request) {
        return ClienteResponse.from(useCase.atualizar(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void inativar(@PathVariable UUID id) {
        useCase.inativar(id);
    }
}
