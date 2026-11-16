package br.edu.uniesp.threedcontrol.cadastros.cliente.application;

import br.edu.uniesp.threedcontrol.cadastros.cliente.domain.CanalAtendimento;

public record ClienteCommand(String nome, String documento, String telefone, String email,
                             CanalAtendimento canalPreferencial, String preferenciasPersonalizacao) {
}
