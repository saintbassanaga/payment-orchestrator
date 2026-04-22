package io.payorch.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Converts numeric amounts to French or English text representation.
 *
 * <p>Vocabulary is loaded from classpath resource bundles:
 * {@code io/payorch/core/util/amount_words_fr.properties} and
 * {@code io/payorch/core/util/amount_words_en.properties}.
 * Adding a new language requires only a new properties file and a {@link Language} entry.
 *
 * <p>Examples:
 * <pre>{@code
 * AmountToWords.inLetter(500_000L, Language.FR)                        // "Cinq cent mille"
 * AmountToWords.inLetter(500_000L, Language.EN)                        // "Five hundred thousand"
 * AmountToWords.inLetter(new BigDecimal("1500.50"), Language.FR)       // "Mille cinq cents et cinquante centimes"
 * AmountToWords.inLetter(new BigDecimal("5000"), "XAF", Language.FR)   // "Cinq mille Francs CFA"
 * AmountToWords.inLetter(new BigDecimal("5000"), "USD", Language.EN)   // "Five thousand US Dollars"
 * }</pre>
 *
 * <p>French orthography rules applied by the algorithm:
 * <ul>
 *   <li>70 = soixante-dix, 71 = soixante et onze, 80 = quatre-vingts, 90 = quatre-vingt-dix</li>
 *   <li>"cents" plural only when it terminates a number (deux cents, but deux cent mille)</li>
 *   <li>"mille" is invariable and never preceded by "un"</li>
 * </ul>
 *
 * @since 0.1.0
 */
public final class AmountToWords {

    private static final String BUNDLE_BASE = "io.payorch.core.util.amount_words";

    /**
     * Output language for amount-to-words conversion.
     *
     * @since 0.1.0
     */
    public enum Language {
        /** Standard French (Belgium/Switzerland/West Africa rules not applied). */
        FR(Locale.FRENCH),
        /** Standard English. */
        EN(Locale.ENGLISH);

        private final Locale locale;

        Language(Locale locale) {
            this.locale = locale;
        }
    }

    /** Per-language vocabulary loaded once at class-init time. */
    private record Words(
            String[] units,
            String[] tens,
            String zero,
            String hundred,
            String thousand,
            String millionOne,
            String millionMany,
            String billionOne,
            String billionMany,
            String subunitConnector,
            String subunitOne,
            String subunitMany
    ) {
        static Words load(Language lang) {
            ResourceBundle b = ResourceBundle.getBundle(BUNDLE_BASE, lang.locale);
            return new Words(
                    b.getString("units").split(",", -1),
                    b.getString("tens").split(",", -1),
                    b.getString("zero"),
                    b.getString("hundred"),
                    b.getString("thousand"),
                    b.getString("million.one"),
                    b.getString("million.many"),
                    b.getString("billion.one"),
                    b.getString("billion.many"),
                    b.getString("subunit.connector"),
                    b.getString("subunit.one"),
                    b.getString("subunit.many")
            );
        }
    }

    private static final Words WORDS_FR = Words.load(Language.FR);
    private static final Words WORDS_EN = Words.load(Language.EN);

    private AmountToWords() {
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Converts a whole number to text in the given language.
     *
     * @param amount the amount, must be &gt;= 0
     * @param lang   the output language
     * @return the text, first letter capitalized
     * @throws NullPointerException     if lang is null
     * @throws IllegalArgumentException if amount is negative
     * @since 0.1.0
     */
    public static String inLetter(long amount, Language lang) {
        Objects.requireNonNull(lang, "lang must not be null");
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative, got: " + amount);
        }
        Words w = words(lang);
        if (amount == 0) {
            return capitalize(w.zero());
        }
        return capitalize(convert(amount, true, lang, w));
    }

    /**
     * Converts a decimal amount to text in the given language.
     *
     * @param amount the amount, must be &gt;= 0
     * @param lang   the output language
     * @return the text with fractional part when non-zero, first letter capitalized
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if amount is negative
     * @since 0.1.0
     */
    public static String inLetter(BigDecimal amount, Language lang) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(lang, "lang must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must not be negative, got: " + amount);
        }
        Words w = words(lang);
        BigDecimal[] parts = amount.setScale(2, RoundingMode.HALF_UP)
                                   .divideAndRemainder(BigDecimal.ONE);
        long whole   = parts[0].longValueExact();
        int  subunit = parts[1].movePointRight(2).intValue();

        String wholeText = whole == 0 ? w.zero() : convert(whole, true, lang, w);
        if (subunit == 0) {
            return capitalize(wholeText);
        }
        String subText = convert(subunit, true, lang, w);
        String label   = subunit == 1 ? w.subunitOne() : w.subunitMany();
        return capitalize(wholeText + " " + w.subunitConnector() + " " + subText + " " + label);
    }

    /**
     * Converts a decimal amount to text in the given language with a currency label.
     *
     * @param amount       the amount, must be &gt;= 0
     * @param currencyCode ISO 4217 currency code
     * @param lang         the output language
     * @return the text with currency label, first letter capitalized
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if amount is negative
     * @since 0.1.0
     */
    public static String inLetter(BigDecimal amount, String currencyCode, Language lang) {
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
        return inLetter(amount, lang) + " " + currencyLabel(currencyCode.toUpperCase(), lang);
    }

    // ── Magnitude bucketing ───────────────────────────────────────────────────

    /**
     * Classifies a non-negative long into a numeric magnitude bucket.
     * Used as the switch selector in {@link #convert} so we get an exhaustive
     * enum switch on a reference type — valid in Java 21, no preview needed.
     */
    private enum Magnitude {
        ZERO, UNIT, BELOW_100, BELOW_1K, BELOW_1M, BELOW_1B, BILLIONS;

        static Magnitude of(long n) {
            if (n == 0) {
                return ZERO;
            }
            if (n < 20) {
                return UNIT;
            }
            if (n < 100) {
                return BELOW_100;
            }
            if (n < 1_000) {
                return BELOW_1K;
            }
            if (n < 1_000_000L) {
                return BELOW_1M;
            }
            if (n < 1_000_000_000L) {
                return BELOW_1B;
            }
            return BILLIONS;
        }
    }

    // ── Core algorithm ────────────────────────────────────────────────────────

    private static String convert(long n, boolean terminal, Language lang, Words w) {
        return switch (Magnitude.of(n)) {
            case ZERO      -> "";
            case UNIT      -> w.units()[(int) n];
            case BELOW_100 -> below100((int) n, terminal, lang, w);
            case BELOW_1K  -> below1000((int) n, terminal, lang, w);
            case BELOW_1M  -> {
                long th  = n / 1_000;
                long rem = n % 1_000;
                // French: "un" is omitted before "mille" → "mille" not "un mille"
                String prefix = (lang == Language.FR && th == 1)
                        ? w.thousand()
                        : convert(th, false, lang, w) + " " + w.thousand();
                yield rem == 0 ? prefix : prefix + " " + convert(rem, terminal, lang, w);
            }
            case BELOW_1B  -> {
                long m   = n / 1_000_000;
                long rem = n % 1_000_000;
                String label  = m == 1 ? w.millionOne() : w.millionMany();
                String prefix = convert(m, true, lang, w) + " " + label;
                yield rem == 0 ? prefix : prefix + " " + convert(rem, terminal, lang, w);
            }
            case BILLIONS  -> {
                long b   = n / 1_000_000_000L;
                long rem = n % 1_000_000_000L;
                String label  = b == 1 ? w.billionOne() : w.billionMany();
                String prefix = convert(b, true, lang, w) + " " + label;
                yield rem == 0 ? prefix : prefix + " " + convert(rem, terminal, lang, w);
            }
        };
    }

    /** Handles 20–99. French special cases for 70–99 are algorithmic, not data. */
    private static String below100(int n, boolean terminal, Language lang, Words w) {
        int t = n / 10;
        int u = n % 10;
        return switch (lang) {
            case EN -> u == 0 ? w.tens()[t] : w.tens()[t] + "-" + w.units()[u];
            case FR -> switch (t) {
                // 70–79: soixante-dix…; 71 = soixante et onze
                case 7 -> u == 1
                        ? w.tens()[6] + " et " + w.units()[11]
                        : w.tens()[6] + "-" + w.units()[10 + u];
                // 80 = quatre-vingts (s only when terminal); 81–89 = quatre-vingt-X
                case 8 -> u == 0
                        ? (terminal ? w.tens()[8] + "s" : w.tens()[8])
                        : w.tens()[8] + "-" + w.units()[u];
                // 90–99: quatre-vingt-dix…
                case 9 -> w.tens()[8] + "-" + w.units()[10 + u];
                default -> switch (u) {
                    case 0  -> w.tens()[t];
                    case 1  -> w.tens()[t] + " et un";
                    default -> w.tens()[t] + "-" + w.units()[u];
                };
            };
        };
    }

    /** Handles 100–999. */
    private static String below1000(int n, boolean terminal, Language lang, Words w) {
        int    h      = n / 100;
        int    rem    = n % 100;
        String subRem = rem == 0 ? "" : " " + (rem < 20 ? w.units()[rem] : below100(rem, terminal, lang, w));
        return switch (lang) {
            case EN -> {
                String base = w.units()[h] + " " + w.hundred();
                yield rem == 0 ? base : base + subRem;
            }
            case FR -> switch (h) {
                // French: "cent" alone for 100; no "un cent"
                case 1  -> rem == 0 ? w.hundred() : w.hundred() + subRem;
                // French: "deux cents" (terminal, no rem) but "deux cent mille" (non-terminal)
                default -> {
                    String base = w.units()[h] + " " + w.hundred();
                    yield rem == 0 ? (terminal ? base + "s" : base) : base + subRem;
                }
            };
        };
    }

    // ── Currency labels ───────────────────────────────────────────────────────

    /**
     * Resolves a currency display name for the given language.
     *
     * <p>XAF/XOF are overridden because CLDR names ("franc CFA BEAC / BCEAO") differ
     * from the common usage expected by West/Central African integrators ("Francs CFA").
     * All other ISO 4217 codes are resolved automatically via {@link Currency#getDisplayName}.
     */
    private static String currencyLabel(String code, Language lang) {
        return Optional.ofNullable(switch (code) {
                    case "XAF", "XOF" -> lang == Language.FR ? "Francs CFA" : "CFA Francs";
                    default            -> null;
                })
                .orElseGet(() -> jdkCurrencyName(code, lang.locale));
    }

    private static String jdkCurrencyName(String code, Locale locale) {
        try {
            return Currency.getInstance(code).getDisplayName(locale);
        } catch (IllegalArgumentException e) {
            return code;
        }
    }

    private static Words words(Language lang) {
        return switch (lang) {
            case FR -> WORDS_FR;
            case EN -> WORDS_EN;
        };
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
