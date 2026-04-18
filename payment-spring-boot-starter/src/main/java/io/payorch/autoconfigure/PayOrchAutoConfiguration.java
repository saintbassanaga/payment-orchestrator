package io.payorch.autoconfigure;

import io.payorch.core.port.PaymentGateway;
import io.payorch.core.spi.ProviderRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configures the PayOrch {@link PaymentGateway} and its backing {@link ProviderRegistry}.
 *
 * <p>Providers are discovered via Java SPI ({@link java.util.ServiceLoader}), which means
 * any adapter JAR on the classpath (e.g. {@code payment-provider-pawapay}) is automatically
 * registered — no explicit bean declaration is required.
 *
 * <p>Both beans can be overridden by declaring your own {@link PaymentGateway} or
 * {@link ProviderRegistry} bean in an application {@code @Configuration} class.
 *
 * @since 0.1.0
 */
@AutoConfiguration
public class PayOrchAutoConfiguration {

    /**
     * Creates a {@link ProviderRegistry} populated with all providers discoverable via SPI.
     *
     * @return a fully populated provider registry
     */
    @Bean
    @ConditionalOnMissingBean
    public ProviderRegistry payOrchProviderRegistry() {
        return ProviderRegistry.loadFromServiceLoader();
    }

    /**
     * Creates a {@link PaymentGateway} backed by the autoconfigured registry.
     *
     * @param registry the provider registry to use
     * @return a ready-to-use payment gateway
     */
    @Bean
    @ConditionalOnMissingBean
    public PaymentGateway paymentGateway(ProviderRegistry registry) {
        return PaymentGateway.builder()
                .registry(registry)
                .build();
    }
}