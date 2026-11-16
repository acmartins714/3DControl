package br.edu.uniesp.threedcontrol.cadastros.cliente.infrastructure.web;

import br.edu.uniesp.threedcontrol.cadastros.cliente.application.ClienteCommand;
import br.edu.uniesp.threedcontrol.cadastros.cliente.domain.CanalAtendimento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank @Size(max = 150) String nome,
        @Size(max = 20) String documento,
        @Size(max = 20) String telefone,
        @Email @Size(max = 150) String email,
        @NotNull CanalAtendimento canalPreferencial,
        String preferenciasPersonalizacao) {

    ClienteCommand toCommand() {
        return new ClienteCommand(nome, documento, telefone, email, canalPreferencial, preferenciasPersonalizacao);
    }
}
