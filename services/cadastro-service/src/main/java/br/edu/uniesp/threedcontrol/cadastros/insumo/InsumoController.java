package br.edu.uniesp.threedcontrol.cadastros.insumo;

import br.edu.uniesp.threedcontrol.cadastros.insumo.InsumoDtos.InsumoRequest;
import br.edu.uniesp.threedcontrol.cadastros.insumo.InsumoDtos.InsumoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/insumos")
public class InsumoController {

    private final InsumoService service;

    public InsumoController(InsumoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InsumoResponse cadastrar(@Valid @RequestBody InsumoRequest request) {
        return service.cadastrar(request);
    }

    @GetMapping
    public List<InsumoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public InsumoResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public InsumoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody InsumoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable UUID id) {
        service.inativar(id);
    }
}
