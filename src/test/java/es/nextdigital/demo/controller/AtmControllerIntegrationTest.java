package es.nextdigital.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración contra la aplicación completa (H2 + datos de data.sql).
 * Cada test hace rollback, así que no se afectan entre sí.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AtmControllerIntegrationTest {

    private static final String OWN_ATM = "/api/atms/1/cards/";
    private static final String FOREIGN_ATM = "/api/atms/2/cards/";
    private static final String DEBIT = "4000000000000001";
    private static final String CREDIT = "4000000000000002";
    private static final String INACTIVE = "4000000000000003";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void withdrawFromOwnAtm() throws Exception {
        call(OWN_ATM + DEBIT + "/withdrawals", "{\"pin\":\"1234\",\"amount\":100}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(900.0))
                .andExpect(jsonPath("$.fee").value(0));
    }

    @Test
    void withdrawFromForeignAtmChargesFeeAndShowsItInMovements() throws Exception {
        call(FOREIGN_ATM + DEBIT + "/withdrawals", "{\"pin\":\"1234\",\"amount\":100}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(898.0));

        call(OWN_ATM + DEBIT + "/accounts/ES0690000001000000000001/movements", "{\"pin\":\"1234\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.type=='FEE')].amount").value(2.0))
                .andExpect(jsonPath("$[?(@.type=='WITHDRAWAL')].amount").value(100.0));
    }

    @Test
    void creditCardCanGoBelowZero() throws Exception {
        call(OWN_ATM + CREDIT + "/withdrawals", "{\"pin\":\"1234\",\"amount\":500}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(-300.0));
    }

    @Test
    void withdrawAboveCardLimit() throws Exception {
        call(OWN_ATM + DEBIT + "/withdrawals", "{\"pin\":\"1234\",\"amount\":700}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("WITHDRAWAL_LIMIT_EXCEEDED"));
    }

    @Test
    void wrongPin() throws Exception {
        call(OWN_ATM + DEBIT + "/withdrawals", "{\"pin\":\"9999\",\"amount\":10}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_PIN"));
    }

    @Test
    void depositInForeignAtmIsRejected() throws Exception {
        call(FOREIGN_ATM + DEBIT + "/deposits", "{\"pin\":\"1234\",\"amount\":50}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DEPOSIT_NOT_ALLOWED"));
    }

    @Test
    void internalTransferAppearsInBothAccounts() throws Exception {
        call(OWN_ATM + DEBIT + "/transfers",
                "{\"pin\":\"1234\",\"destinationIban\":\"ES49 9000 0001 0000 0000 0003\",\"amount\":50,\"concept\":\"Regalo\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(950.0));

        // La cuenta 2 es del mismo cliente: puede consultarla con la tarjeta de la cuenta 1
        call(OWN_ATM + DEBIT + "/accounts/ES7690000001000000000002/movements", "{\"pin\":\"1234\"}")
                .andExpect(status().isOk());
        // La cuenta 3 es de otro cliente
        call(OWN_ATM + DEBIT + "/accounts/ES4990000001000000000003/movements", "{\"pin\":\"1234\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_OWNED"));
    }

    @Test
    void transferToInvalidIban() throws Exception {
        call(OWN_ATM + DEBIT + "/transfers",
                "{\"pin\":\"1234\",\"destinationIban\":\"ES0000000000000000000000\",\"amount\":50}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IBAN"));
    }

    @Test
    void invalidRequestBody() throws Exception {
        call(OWN_ATM + DEBIT + "/withdrawals", "{\"pin\":\"12\",\"amount\":-5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void inactiveCardMustBeActivatedBeforeOperating() throws Exception {
        call(OWN_ATM + INACTIVE + "/withdrawals", "{\"pin\":\"0000\",\"amount\":10}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CARD_NOT_ACTIVATED"));

        call(OWN_ATM + INACTIVE + "/activation", "{\"currentPin\":\"0000\",\"newPin\":\"5678\"}")
                .andExpect(status().isNoContent());

        // El PIN inicial deja de valer y el nuevo permite operar
        call(OWN_ATM + INACTIVE + "/withdrawals", "{\"pin\":\"0000\",\"amount\":10}")
                .andExpect(status().isUnauthorized());
        call(OWN_ATM + INACTIVE + "/withdrawals", "{\"pin\":\"5678\",\"amount\":10}")
                .andExpect(status().isOk());

        mockMvc.perform(put(OWN_ATM + INACTIVE + "/pin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPin\":\"5678\",\"newPin\":\"1111\"}"))
                .andExpect(status().isNoContent());
    }

    private ResultActions call(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
