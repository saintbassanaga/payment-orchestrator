package io.payorch.core.model;

/**
 * Represents the deployment environment used when communicating with a payment provider.
 *
 * @since 0.1.0
 */
public enum Environment {

    /** Sandbox environment — no real money movement, safe for testing. */
    SANDBOX,

    /** Production environment — live credentials and real transactions. */
    PRODUCTION
}