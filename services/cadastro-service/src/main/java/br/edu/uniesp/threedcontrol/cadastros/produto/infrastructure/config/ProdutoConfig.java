package br.edu.uniesp.threedcontrol.cadastros.produto.infrastructure.config;

import br.edu.uniesp.threedcontrol.cadastros.insumo.domain.InsumoRepository;
import br.edu.uniesp.threedcontrol.cadastros.produto.application.GerenciarProdutosUseCase;
import br.edu.uniesp.threedcontrol.cadastros.produto.domain.ProdutoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ProdutoConfig {

    @Bean
    GerenciarProdutosUseCase gerenciarProdutosUseCase(ProdutoRepository produtos, InsumoRepository insumos) {
        return new GerenciarProdutosUseCase(produtos, insumos);
    }
}
