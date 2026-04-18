package io.payorch.core.spi;

import io.payorch.core.exception.ProviderNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discovers and manages {@link PaymentProviderSpi} implementations.
 *
 * <p>On first access, providers are loaded via {@link ServiceLoader} (Java SPI).
 * Additional providers can be registered programmatically via {@link #register(PaymentProviderSpi)}.
 *
 * @since 0.1.0
 */
public final class ProviderRegistry {

    private final Map<String, PaymentProviderSpi> providers = new ConcurrentHashMap<>();

    /**
     * Creates a registry pre-loaded with all providers discoverable via {@link ServiceLoader}.
     *
     * @return a new registry containing all SPI-discovered providers
     */
    public static ProviderRegistry loadFromServiceLoader() {
        ProviderRegistry registry = new ProviderRegistry();
        ServiceLoader.load(PaymentProviderSpi.class)
                .forEach(registry::register);
        return registry;
    }

    /**
     * Creates an empty registry with no pre-registered providers.
     *
     * @return an empty registry
     */
    public static ProviderRegistry empty() {
        return new ProviderRegistry();
    }

    /**
     * Registers a provider adapter in this registry.
     *
     * <p>If a provider with the same name is already registered, it is replaced.
     *
     * @param provider the provider to register, never null
     * @throws NullPointerException if {@code provider} is null
     */
    public void register(PaymentProviderSpi provider) {
        Objects.requireNonNull(provider, "provider must not be null");
        providers.put(provider.providerName(), provider);
    }

    /**
     * Returns the provider registered under the given name.
     *
     * @param providerName the canonical provider name, never null
     * @return the matching provider, never null
     * @throws NullPointerException      if {@code providerName} is null
     * @throws ProviderNotFoundException if no provider with that name is registered
     */
    public PaymentProviderSpi get(String providerName) {
        Objects.requireNonNull(providerName, "providerName must not be null");
        PaymentProviderSpi provider = providers.get(providerName);
        if (provider == null) {
            throw new ProviderNotFoundException(providerName);
        }
        return provider;
    }

    /**
     * Returns an unmodifiable view of all registered providers.
     *
     * @return a collection of registered providers, never null
     */
    public Collection<PaymentProviderSpi> all() {
        return List.copyOf(providers.values());
    }

    /**
     * Returns whether a provider with the given name is registered.
     *
     * @param providerName the canonical provider name, never null
     * @return {@code true} if a provider with that name exists
     * @throws NullPointerException if {@code providerName} is null
     */
    public boolean contains(String providerName) {
        Objects.requireNonNull(providerName, "providerName must not be null");
        return providers.containsKey(providerName);
    }
}