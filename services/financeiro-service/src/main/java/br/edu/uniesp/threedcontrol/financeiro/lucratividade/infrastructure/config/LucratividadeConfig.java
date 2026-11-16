package br.edu.uniesp.threedcontrol.financeiro.lucratividade.infrastructure.config;

import br.edu.uniesp.threedcontrol.financeiro.contareceber.domain.ContaReceberRepository;
import br.edu.uniesp.threedcontrol.financeiro.lucratividade.application.AnalisarLucratividadeUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class LucratividadeConfig {

    @Bean
    AnalisarLucratividadeUseCase analisarLucratividadeUseCase(ContaReceberRepository repository) {
        return new AnalisarLucratividadeUseCase(repository);
    }
}
