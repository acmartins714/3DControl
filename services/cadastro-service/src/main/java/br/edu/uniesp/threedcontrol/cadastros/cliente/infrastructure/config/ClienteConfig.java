package br.edu.uniesp.threedcontrol.cadastros.cliente.infrastructure.config;

import br.edu.uniesp.threedcontrol.cadastros.cliente.application.GerenciarClientesUseCase;
import br.edu.uniesp.threedcontrol.cadastros.cliente.domain.ClienteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composição: registra os casos de uso (classes puras) como beans do Spring. */
@Configuration
class ClienteConfig {

    @Bean
    GerenciarClientesUseCase gerenciarClientesUseCase(ClienteRepository repository) {
        return new GerenciarClientesUseCase(repository);
    }
}
