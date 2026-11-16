package br.edu.uniesp.threedcontrol.cadastros.cliente.infrastructure.web;

import br.edu.uniesp.threedcontrol.cadastros.cliente.domain.CanalAtendimento;
import br.edu.uniesp.threedcontrol.cadastros.cliente.domain.Cliente;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClienteResponse(UUID id, String nome, String documento, String telefone, String email,
                              CanalAtendimento canalPreferencial, String preferenciasPersonalizacao,
                              boolean ativo, LocalDateTime cadastradoEm) {

    static ClienteResponse from(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNome(), c.getDocumento(), c.getTelefone(), c.getEmail(),
                c.getCanalPreferencial(), c.getPreferenciasPersonalizacao(), c.isAtivo(), c.getCadastradoEm());
    }
}
