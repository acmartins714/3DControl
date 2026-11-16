package br.edu.uniesp.threedcontrol.cadastros.shared.infrastructure.config;

import org.springframework.aop.Advisor;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Aplica transações aos casos de uso SEM anotar a camada de aplicação com {@code @Transactional}.
 * <p>
 * Toda classe {@code *UseCase} do pacote {@code ..application..} é envolvida por um proxy transacional:
 * cada chamada pública de caso de uso é uma unidade de trabalho atômica (rollback em RuntimeException).
 * Assim a camada de aplicação permanece em Java puro, e a decisão técnica fica na infraestrutura.
 */
@Configuration(proxyBeanMethods = false)
class TransacaoConfig {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static TransactionInterceptor casoDeUsoTransactionInterceptor() {
        NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
        source.addTransactionalMethod("*", new DefaultTransactionAttribute());
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionAttributeSource(source);
        return interceptor;
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static Advisor casoDeUsoTransactionAdvisor(TransactionInterceptor casoDeUsoTransactionInterceptor) {
        ClassFilter casosDeUso = clazz -> clazz.getSimpleName().endsWith("UseCase")
                && clazz.getPackageName().endsWith(".application");
        return new DefaultPointcutAdvisor(new ComposablePointcut(casosDeUso, MethodMatcher.TRUE),
                casoDeUsoTransactionInterceptor);
    }
}
