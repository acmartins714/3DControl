package br.edu.uniesp.threedcontrol.cadastros.cliente.infrastructure.persistence;

import br.edu.uniesp.threedcontrol.cadastros.cliente.domain.CanalAtendimento;
import br.edu.uniesp.threedcontrol.cadastros.cliente.domain.Cliente;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cliente")
public class ClienteJpaEntity {

    @Id
    private UUID id;
    private String nome;
    private String documento;
    private String telefone;
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(name = "canal_preferencial")
    private CanalAtendimento canalPreferencial;
    @Column(name = "preferencias_personalizacao")
    private String preferenciasPersonalizacao;
    private boolean ativo;
    @Column(name = "cadastrado_em")
    private LocalDateTime cadastradoEm;

    protected ClienteJpaEntity() {
    }

    static ClienteJpaEntity from(Cliente c) {
        ClienteJpaEntity e = new ClienteJpaEntity();
        e.id = c.getId();
        e.nome = c.getNome();
        e.documento = c.getDocumento();
        e.telefone = c.getTelefone();
        e.email = c.getEmail();
        e.canalPreferencial = c.getCanalPreferencial();
        e.preferenciasPersonalizacao = c.getPreferenciasPersonalizacao();
        e.ativo = c.isAtivo();
        e.cadastradoEm = c.getCadastradoEm();
        return e;
    }

    Cliente toDomain() {
        return Cliente.restaurar(id, nome, documento, telefone, email, canalPreferencial,
                preferenciasPersonalizacao, ativo, cadastradoEm);
    }
}
