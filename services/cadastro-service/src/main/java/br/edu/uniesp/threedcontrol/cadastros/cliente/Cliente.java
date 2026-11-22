package br.edu.uniesp.threedcontrol.cadastros.cliente;

import br.edu.uniesp.threedcontrol.cadastros.shared.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/** Cliente da empresa. Entidade JPA que também guarda as regras de validação do cadastro. */
@Entity
@Table(name = "cliente")
public class Cliente {

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

    /** Exigido pelo JPA. */
    protected Cliente() {
    }

    public Cliente(String nome, String documento, String telefone, String email,
                   CanalAtendimento canalPreferencial, String preferenciasPersonalizacao) {
        this.id = UUID.randomUUID();
        this.ativo = true;
        this.cadastradoEm = LocalDateTime.now();
        atualizar(nome, documento, telefone, email, canalPreferencial, preferenciasPersonalizacao);
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
            throw new RegraNegocioException("O nome do cliente é obrigatório.");
        }
        if (isBlank(telefone) && isBlank(email)) {
            throw new RegraNegocioException("Informe ao menos um meio de contato (telefone ou e-mail).");
        }
        if (canalPreferencial == null) {
            throw new RegraNegocioException("O canal de atendimento preferencial é obrigatório.");
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
