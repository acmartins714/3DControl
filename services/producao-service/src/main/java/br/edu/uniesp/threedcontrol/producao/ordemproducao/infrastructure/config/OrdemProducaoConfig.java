package br.edu.uniesp.threedcontrol.producao.ordemproducao.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class OrdemProducaoConfig {

    /** Relógio injetável: permite testar a priorização da fila com uma data fixa. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
