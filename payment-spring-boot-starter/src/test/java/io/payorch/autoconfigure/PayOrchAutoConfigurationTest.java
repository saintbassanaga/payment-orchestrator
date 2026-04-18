package io.payorch.autoconfigure;

import io.payorch.core.port.PaymentGateway;
import io.payorch.core.spi.ProviderRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class PayOrchAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PayOrchAutoConfiguration.class));

    @Test
    void should_register_provider_registry_bean() {
        contextRunner.run(ctx ->
                assertThat(ctx).hasSingleBean(ProviderRegistry.class));
    }

    @Test
    void should_register_payment_gateway_bean() {
        contextRunner.run(ctx ->
                assertThat(ctx).hasSingleBean(PaymentGateway.class));
    }

    @Test
    void should_not_override_user_defined_registry() {
        contextRunner
                .withUserConfiguration(CustomRegistryConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ProviderRegistry.class);
                    ProviderRegistry registry = ctx.getBean(ProviderRegistry.class);
                    assertThat(registry.contains("custom-provider")).isTrue();
                });
    }

    @Test
    void should_not_override_user_defined_gateway() {
        contextRunner
                .withUserConfiguration(CustomGatewayConfig.class)
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(PaymentGateway.class));
    }

    @Test
    void should_wire_gateway_with_auto_configured_registry() {
        contextRunner.run(ctx -> {
            PaymentGateway gateway = ctx.getBean(PaymentGateway.class);
            ProviderRegistry registry = ctx.getBean(ProviderRegistry.class);
            assertThat(gateway.registry()).isSameAs(registry);
        });
    }

    @Configuration
    static class CustomRegistryConfig {
        @Bean
        ProviderRegistry payOrchProviderRegistry() {
            ProviderRegistry registry = ProviderRegistry.empty();
            registry.register(new StubProvider("custom-provider"));
            return registry;
        }
    }

    @Configuration
    static class CustomGatewayConfig {
        @Bean
        PaymentGateway paymentGateway() {
            return PaymentGateway.builder()
                    .registry(ProviderRegistry.empty())
                    .build();
        }
    }
}