package io.payorch.phone;

/**
 * Mobile network operators supported by PayOrch provider adapters.
 *
 * <p>Each constant maps to the branded operator identity used in PawaPay
 * correspondent codes (e.g. {@code MTN} → {@code MTN_MOMO_CMR}).
 *
 * @since 0.1.0
 */
public enum MobileOperator {

    /** MTN Mobile Money — active in Cameroon, Ghana, Rwanda, Uganda, Zambia, Côte d'Ivoire, etc. */
    MTN,

    /** Orange Money — active in Cameroon, Senegal, Mali, Burkina Faso, etc. */
    ORANGE,

    /** Airtel Money — active in Kenya, Tanzania, Uganda, Zambia, Madagascar, etc. */
    AIRTEL,

    /** Moov Money — active in Burkina Faso, Benin, Togo, Niger, Côte d'Ivoire, etc. */
    MOOV,

    /** Wave — active in Senegal, Côte d'Ivoire, Burkina Faso, Mali, Uganda. */
    WAVE,

    /** Vodacom M-Pesa — active in Tanzania, Mozambique, DRC. */
    VODACOM,

    /** Vodafone Cash — active in Ghana. */
    VODAFONE,

    /** Tigo Pesa — active in Tanzania, Ghana, Senegal. */
    TIGO,

    /** Zamtel Kwacha — active in Zambia. */
    ZAMTEL
}
