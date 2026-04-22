package io.payorch.provider.pawapay.mapper;

import io.payorch.core.exception.InvalidPaymentRequestException;
import io.payorch.core.model.Money;
import io.payorch.core.model.PaymentRequest;
import io.payorch.provider.pawapay.dto.PawaPayDepositRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PawaPayRequestMapperTest {

    private static final String CMR_NUMBER       = "+237671000001";
    private static final String CORRESPONDENT    = "MTN_MOMO_CMR";
    private static final Map<String, String> META = Map.of("correspondent", CORRESPONDENT);

    @Test
    void should_format_whole_amount_without_decimals() {
        PaymentRequest req = request("5000.00", CMR_NUMBER, META);
        PawaPayDepositRequest dto = PawaPayRequestMapper.toDepositRequest(req);
        assertThat(dto.amount()).isEqualTo("5000");
    }

    @Test
    void should_format_amount_with_significant_decimals() {
        PaymentRequest req = request("5.50", CMR_NUMBER, META);
        PawaPayDepositRequest dto = PawaPayRequestMapper.toDepositRequest(req);
        assertThat(dto.amount()).isEqualTo("5.5");
    }

    @Test
    void should_preserve_valid_uuid_transaction_id() {
        String uuid = UUID.randomUUID().toString();
        PaymentRequest req = requestWithId(uuid, CMR_NUMBER, META);
        PawaPayDepositRequest dto = PawaPayRequestMapper.toDepositRequest(req);
        assertThat(dto.depositId()).isEqualTo(uuid);
    }

    @Test
    void should_convert_non_uuid_to_deterministic_uuid() {
        PaymentRequest req1 = requestWithId("TX-001", CMR_NUMBER, META);
        PaymentRequest req2 = requestWithId("TX-001", CMR_NUMBER, META);
        String id1 = PawaPayRequestMapper.toDepositRequest(req1).depositId();
        String id2 = PawaPayRequestMapper.toDepositRequest(req2).depositId();

        assertThat(id1).isEqualTo(id2);
        assertThat(UUID.fromString(id1)).isNotNull(); // valid UUID format
    }

    @Test
    void should_use_correspondent_from_metadata() {
        PaymentRequest req = request("5000", CMR_NUMBER, META);
        PawaPayDepositRequest dto = PawaPayRequestMapper.toDepositRequest(req);
        assertThat(dto.payer().accountDetails().provider()).isEqualTo(CORRESPONDENT);
    }

    @Test
    void should_truncate_description_to_22_chars() {
        String longDesc = "This description is definitely too long for PawaPay";
        PaymentRequest req = new PaymentRequest(
                "TX-001", Money.of(new BigDecimal("5000"), "XAF"),
                CMR_NUMBER, longDesc, Optional.empty(), META);
        PawaPayDepositRequest dto = PawaPayRequestMapper.toDepositRequest(req);
        assertThat(dto.customerMessage()).hasSize(22);
    }

    @Test
    void should_set_payer_type_to_mmo() {
        PaymentRequest req = request("5000", CMR_NUMBER, META);
        PawaPayDepositRequest dto = PawaPayRequestMapper.toDepositRequest(req);
        assertThat(dto.payer().type()).isEqualTo("MMO");
    }

    @Test
    void should_throw_when_correspondent_unresolvable() {
        // Phone number that has no carrier mapping and no metadata override
        PaymentRequest req = new PaymentRequest(
                "TX-001", Money.of(new BigDecimal("5000"), "XAF"),
                "+15550001234", "desc", Optional.empty(), Map.of());

        assertThatThrownBy(() -> PawaPayRequestMapper.toDepositRequest(req))
                .isInstanceOf(InvalidPaymentRequestException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static PaymentRequest request(String amount, String phone, Map<String, String> meta) {
        return new PaymentRequest(
                UUID.randomUUID().toString(),
                Money.of(new BigDecimal(amount), "XAF"),
                phone, "Payment", Optional.empty(), meta);
    }

    private static PaymentRequest requestWithId(String id, String phone, Map<String, String> meta) {
        return new PaymentRequest(
                id, Money.of(new BigDecimal("5000"), "XAF"),
                phone, "Payment", Optional.empty(), meta);
    }
}
