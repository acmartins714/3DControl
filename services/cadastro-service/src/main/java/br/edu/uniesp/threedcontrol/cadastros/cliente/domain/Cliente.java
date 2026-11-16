package br.edu.uniesp.threedcontrol.cadastros.cliente.domain;

import br.edu.uniesp.threedcontrol.cadastros.shared.domain.DomainException;

import java.time.LocalDateTime;
import java.util.UUID;

/** Entidade de domínio Cliente (livre de anotações de framework). */
public class Cliente {

    private final UUID id;
    private String nome;
    private String documento;
    private String telefone;
    private String email;
    private CanalAtendimento canalPreferencial;
    private String preferenciasPersonalizacao;
    private boolean ativo;
    private final LocalDateTime cadastradoEm;

    private Cliente(UUID id, String nome, String documento, String telefone, String email,
                    CanalAtendimento canalPreferencial, String preferenciasPersonalizacao,
                    boolean ativo, LocalDateTime cadastradoEm) {
        this.id = id;
        this.nome = nome;
        this.documento = documento;
        this.telefone = telefone;
        this.email = email;
        this.canalPreferencial = canalPreferencial;
        this.preferenciasPersonalizacao = preferenciasPersonalizacao;
        this.ativo = ativo;
        this.cadastradoEm = cadastradoEm;
    }

    public static Cliente novo(String nome, String documento, String telefone, String email,
                               CanalAtendimento canalPreferencial, String preferenciasPersonalizacao) {
        Cliente c = new Cliente(UUID.randomUUID(), nome, documento, telefone, email,
                canalPreferencial, preferenciasPersonalizacao, true, LocalDateTime.now());
        c.validar();
        return c;
    }

    public static Cliente restaurar(UUID id, String nome, String documento, String telefone, String email,
                                    CanalAtendimento canalPreferencial, String preferenciasPersonalizacao,
                                    boolean ativo, LocalDateTime cadastradoEm) {
        return new Cliente(id, nome, documento, telefone, email, canalPreferencial,
                preferenciasPersonalizacao, ativo, cadastradoEm);
    }

    public void atualizar(String nome, String documento, String telefone, String email,
                          CanalAtendimento canalPreferencial, String preferenciasPersonalizacao) {
        this.nome = nome;
        this.documento = documento;
        this.telefone = telefone;
        this.email = email;
        this.canalPreferencial = canalPreferencial;
        this.preferenciasPersonalizacao = preferenciasPersonalizacao;
        validar();
    }

    public void inativar() {
        this.ativo = false;
    }

    private void validar() {
        if (nome == null || nome.isBlank()) {
            throw new DomainException("O nome do cliente é obrigatório.");
        }
        if (isBlank(telefone) && isBlank(email)) {
            throw new DomainException("Informe ao menos um meio de contato (telefone ou e-mail).");
        }
        if (canalPreferencial == null) {
            throw new DomainException("O canal de atendimento preferencial é obrigatório.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getDocumento() { return documento; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
    public CanalAtendimento getCanalPreferencial() { return canalPreferencial; }
    public String getPreferenciasPersonalizacao() { return preferenciasPersonalizacao; }
    public boolean isAtivo() { return ativo; }
    public LocalDateTime getCadastradoEm() { return cadastradoEm; }
}
