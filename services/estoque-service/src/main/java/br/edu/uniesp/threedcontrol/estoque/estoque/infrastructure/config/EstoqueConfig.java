package br.edu.uniesp.threedcontrol.estoque.estoque.infrastructure.config;

import br.edu.uniesp.threedcontrol.estoque.estoque.application.GerenciarEstoqueUseCase;
import br.edu.uniesp.threedcontrol.estoque.estoque.application.ReservarInsumosUseCase;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.EstoqueRepositories.ItemEstoqueRepository;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.EstoqueRepositories.MovimentacaoRepository;
import br.edu.uniesp.threedcontrol.estoque.estoque.domain.EstoqueRepositories.ReservaRepository;
import br.edu.uniesp.threedcontrol.estoque.solicitacaocompra.domain.SolicitacaoCompraRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class EstoqueConfig {

    @Bean
    GerenciarEstoqueUseCase gerenciarEstoqueUseCase(ItemEstoqueRepository itens, MovimentacaoRepository movimentacoes) {
        return new GerenciarEstoqueUseCase(itens, movimentacoes);
    }

    @Bean
    ReservarInsumosUseCase reservarInsumosUseCase(ItemEstoqueRepository itens, ReservaRepository reservas,
                                                  MovimentacaoRepository movimentacoes,
                                                  SolicitacaoCompraRepository solicitacoes) {
        return new ReservarInsumosUseCase(itens, reservas, movimentacoes, solicitacoes);
    }
}
