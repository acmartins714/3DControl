package br.edu.uniesp.threedcontrol.cadastros.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/** Formatos de entrada e saída da API de clientes. */
public final class ClienteDtos {

    private ClienteDtos() {
    }

    public record ClienteRequest(
            @NotBlank @Size(max = 150) String nome,
            @Size(max = 20) String documento,
            @Size(max = 20) String telefone,
            @Email @Size(max = 150) String email,
            @NotNull CanalAtendimento canalPreferencial,
            String preferenciasPersonalizacao) {
    }

    public record ClienteResponse(UUID id, String nome, String documento, String telefone, String email,
                                  CanalAtendimento canalPreferencial, String preferenciasPersonalizacao,
                                  boolean ativo, LocalDateTime cadastradoEm) {

        static ClienteResponse from(Cliente c) {
            return new ClienteResponse(c.getId(), c.getNome(), c.getDocumento(), c.getTelefone(), c.getEmail(),
                    c.getCanalPreferencial(), c.getPreferenciasPersonalizacao(), c.isAtivo(), c.getCadastradoEm());
        }
    }
}
