package io.payorch.core.spi;

/**
 * Open extension point for community-contributed payment provider adapters.
 *
 * <p>Third-party modules that cannot extend {@link AbstractOfficialProviderSpi}
 * directly must implement this interface instead. It is a non-sealed extension
 * of {@link PaymentProviderSpi}, allowing unrestricted subtyping outside of
 * {@code payment-core}.
 *
 * @since 0.1.0
 */
public non-sealed interface CommunityPaymentProviderSpi extends PaymentProviderSpi {
}
