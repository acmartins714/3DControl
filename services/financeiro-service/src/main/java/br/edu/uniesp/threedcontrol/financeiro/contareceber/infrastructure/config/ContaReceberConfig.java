package br.edu.uniesp.threedcontrol.financeiro.contareceber.infrastructure.config;

import br.edu.uniesp.threedcontrol.financeiro.contareceber.application.GerenciarContasReceberUseCase;
import br.edu.uniesp.threedcontrol.financeiro.contareceber.domain.ContaReceberRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ContaReceberConfig {

    @Bean
    GerenciarContasReceberUseCase gerenciarContasReceberUseCase(ContaReceberRepository repository) {
        return new GerenciarContasReceberUseCase(repository);
    }
}
