package br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.infrastructure.config;

import br.edu.uniesp.threedcontrol.estoque.estoque.application.GerenciarEstoqueUseCase;
import br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.application.GerenciarSolicitacoesCompraUseCase;
import br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.domain.SolicitacaoCompraRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SolicitacaoCompraConfig {

    @Bean
    GerenciarSolicitacoesCompraUseCase gerenciarSolicitacoesCompraUseCase(SolicitacaoCompraRepository repository,
                                                                          GerenciarEstoqueUseCase estoque) {
        return new GerenciarSolicitacoesCompraUseCase(repository, estoque);
    }
}
