package es.nextdigital.demo.util;

import java.math.BigInteger;
import java.util.Locale;

/**
 * Validación de IBAN según ISO 13616 (algoritmo mod-97).
 */
public final class IbanValidator {

    private static final BigInteger NINETY_SEVEN = BigInteger.valueOf(97);

    private IbanValidator() {
    }

    /** Quita espacios y pasa a mayúsculas. Devuelve null si la entrada es null. */
    public static String normalize(String iban) {
        return iban == null ? null : iban.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String rawIban) {
        String iban = normalize(rawIban);
        if (iban == null || iban.length() < 15 || iban.length() > 34
                || !iban.matches("[A-Z]{2}\\d{2}[A-Z0-9]+")) {
            return false;
        }
        String reorganized = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numeric = new StringBuilder();
        for (char c : reorganized.toCharArray()) {
            numeric.append(Character.getNumericValue(c));
        }
        return new BigInteger(numeric.toString()).mod(NINETY_SEVEN).intValue() == 1;
    }

    /**
     * Código de entidad de un IBAN español (posiciones 5-8). Para IBAN de otros países devuelve null.
     */
    public static String spanishBankCode(String rawIban) {
        String iban = normalize(rawIban);
        if (iban == null || !iban.startsWith("ES") || iban.length() != 24) {
            return null;
        }
        return iban.substring(4, 8);
    }
}
