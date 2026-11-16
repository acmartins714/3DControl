package br.edu.uniesp.threedcontrol.cadastros.insumo.infrastructure.web;

import br.edu.uniesp.threedcontrol.cadastros.insumo.application.GerenciarInsumosUseCase;
import br.edu.uniesp.threedcontrol.cadastros.insumo.application.InsumoCommand;
import br.edu.uniesp.threedcontrol.cadastros.insumo.domain.Insumo;
import br.edu.uniesp.threedcontrol.cadastros.insumo.domain.UnidadeMedida;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/insumos")
class InsumoController {

    record InsumoRequest(@NotBlank @Size(max = 120) String nome,
                         String descricao,
                         @NotNull UnidadeMedida unidadeMedida,
                         @NotNull @DecimalMin("0.0") BigDecimal custoUnitario,
                         boolean usoRegular) {
        InsumoCommand toCommand() {
            return new InsumoCommand(nome, descricao, unidadeMedida, custoUnitario, usoRegular);
        }
    }

    record InsumoResponse(UUID id, String nome, String descricao, UnidadeMedida unidadeMedida,
                          BigDecimal custoUnitario, boolean usoRegular, boolean ativo) {
        static InsumoResponse from(Insumo i) {
            return new InsumoResponse(i.getId(), i.getNome(), i.getDescricao(), i.getUnidadeMedida(),
                    i.getCustoUnitario(), i.isUsoRegular(), i.isAtivo());
        }
    }

    private final GerenciarInsumosUseCase useCase;

    InsumoController(GerenciarInsumosUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    InsumoResponse cadastrar(@Valid @RequestBody InsumoRequest request) {
        return InsumoResponse.from(useCase.cadastrar(request.toCommand()));
    }

    @GetMapping
    List<InsumoResponse> listar() {
        return useCase.listar().stream().map(InsumoResponse::from).toList();
    }

    @GetMapping("/{id}")
    InsumoResponse buscar(@PathVariable UUID id) {
        return InsumoResponse.from(useCase.buscar(id));
    }

    @PutMapping("/{id}")
    InsumoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody InsumoRequest request) {
        return InsumoResponse.from(useCase.atualizar(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void inativar(@PathVariable UUID id) {
        useCase.inativar(id);
    }
}
