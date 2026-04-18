# PayOrch — Payment Orchestrator for Java

## Mission

PayOrch est une librairie Java open source qui unifie les agrégateurs de
paiement africains et internationaux (PawaPay, CinetPay, MonetBill, Notchpay,
PayDunya, Stripe) derrière un contrat unique. Le développeur passe ses
credentials et le nom du provider. Le reste disparaît.

**Ce que PayOrch n'est pas** : un agrégateur de paiement, un service cloud,
un SDK propriétaire. Il ne touche jamais aux flux financiers.

---

## Règle Absolue N°1

> **Le core ne dépend de rien. Tout dépend du core.**

`payment-core` ne contient aucune dépendance externe. Pur Java 21.
Si tu importes quelque chose d'externe dans `payment-core` → violation architecturale. Stop.

---

## Stack Technique

| Élément | Choix |
|---|---|
| Java | **21 (LTS)** |
| Build | **Gradle 8.7+ avec Kotlin DSL** |
| Config | **YAML (.yml)** |
| HTTP | **OkHttp 4.12.0** (dans `payment-http-support` uniquement) |
| JSON | **Jackson 2.17.1** (dans les adapters uniquement) |
| SPI auto-registration | **Google AutoService 1.1.1** |
| Tests | **JUnit 5 + Mockito + AssertJ + WireMock** |
| Spring Boot | **3.3.0** (dans `payment-spring-boot-starter` uniquement) |
| Qualité | **Checkstyle + SpotBugs + JaCoCo** |

---

## Structure du Mono-repo

```
payment-orchestrator/
├── gradle/libs.versions.toml
├── buildSrc/src/main/kotlin/
│   ├── payorch.java-library.gradle.kts
│   ├── payorch.java-quality.gradle.kts
│   └── payorch.java-publish.gradle.kts
├── config/checkstyle/checkstyle.xml
├── config/spotbugs/exclude.xml
├── payment-bom/
├── payment-core/
│   └── src/main/java/io/payorch/core/
│       ├── model/        ← Records immuables uniquement
│       ├── spi/          ← PaymentProviderSpi + ProviderRegistry
│       ├── port/         ← PaymentGateway
│       ├── exception/    ← Hiérarchie d'exceptions
│       └── util/
├── payment-http-support/
├── payment-webhook-support/
├── payment-provider-pawapay/
├── payment-provider-cinetpay/
├── payment-provider-monetbill/
├── payment-test-support/
└── payment-spring-boot-starter/
```

---

## Packages

| Module | Package racine |
|---|---|
| `payment-core` | `io.payorch.core` |
| `payment-http-support` | `io.payorch.http` |
| `payment-webhook-support` | `io.payorch.webhook` |
| `payment-provider-pawapay` | `io.payorch.provider.pawapay` |
| `payment-provider-cinetpay` | `io.payorch.provider.cinetpay` |
| `payment-provider-monetbill` | `io.payorch.provider.monetbill` |
| `payment-test-support` | `io.payorch.test` |
| `payment-spring-boot-starter` | `io.payorch.autoconfigure` |

---

## Règles Java — Non Négociables

**Immuabilité**
- Tous les modèles sont des `record` Java
- Aucun setter public sur aucune classe de `payment-core`
- Collections retournées → `List.copyOf()` ou `Collections.unmodifiableList()`

**Null interdit dans les API publiques**
- Aucune méthode publique ne retourne `null`
- Valeur optionnelle → `Optional<T>`
- Paramètres publics → `Objects.requireNonNull()` en première ligne
- Champs de record → validés dans le compact constructor

**Exceptions**
- Pas d'exceptions checked qui traversent `PaymentProviderSpi`
- Tout converti en sous-type de `PaymentException` (unchecked)
- Exceptions bas niveau toujours enchaînées via `cause` — jamais avalées
- Message : provider + opération + cause racine

**Javadoc**
- Toute classe et méthode publique dans `payment-core` → Javadoc complète
- `@param`, `@return`, `@throws`, `@since`
- Texte commence par un verbe 3ème personne : *Initiates a payment...*

**Compilation**
- `-Xlint:all -Werror` : warnings = erreurs de build

---

## Contrats du Core

### Types dans `model/`

```
PaymentRequest       record — ce que le client construit
PaymentResult        record — réponse normalisée
PaymentEvent         record — webhook normalisé
RefundRequest        record — demande de remboursement
RefundResult         record — réponse remboursement
WebhookRequest       record — headers + body entrants
ProviderCredentials  record — credentials injectés dans un adapter
ProviderCapabilities record — capacités du provider
Money                record — montant + devise ISO 4217 (validé)
PaymentStatus        enum  — INITIATED, PENDING, SUCCESS, FAILED,
                             CANCELLED, EXPIRED, REFUNDED, PARTIAL_REFUND
Environment          enum  — SANDBOX, PRODUCTION
```

### Interface SPI

```java
public interface PaymentProviderSpi {
   String providerName();
   void configure(ProviderCredentials credentials);
   PaymentResult initiate(PaymentRequest request);
   PaymentResult getStatus(String transactionId);
   RefundResult refund(RefundRequest request);
   PaymentEvent parseWebhook(WebhookRequest request);
   ProviderCapabilities capabilities();
}
```

### Hiérarchie d'exceptions

```
PaymentException (abstract, unchecked)
├── ProviderAuthException
├── ProviderUnavailableException
├── ProviderNotFoundException
├── InvalidPaymentRequestException
├── WebhookValidationException
├── TransactionNotFoundException
└── UnsupportedProviderOperationException
```

---

## Structure d'un Adapter

```
XxxProviderSpi        ← implémente PaymentProviderSpi, orchestre
XxxClient             ← appels HTTP uniquement, DTOs bruts
mapper/
  XxxRequestMapper    ← PaymentRequest → DTO provider (fonction pure)
  XxxResponseMapper   ← réponse provider → PaymentResult (fonction pure)
dto/
  XxxPayoutRequest    ← DTO entrant provider
  XxxPayoutResponse   ← DTO sortant provider
```

Les mappers sont des **fonctions pures** : pas d'appel réseau, pas de logique
métier, pas d'état. La traduction des statuts provider → `PaymentStatus`
vit **exclusivement** dans `XxxResponseMapper`.

---

## Ordre de Développement — 12 Étapes

```
Étape  1 — Structure Gradle (settings, buildSrc, libs.versions.toml, bom)
Étape  2 — payment-core : modèles (tous les records + enums)
Étape  3 — payment-core : exceptions (hiérarchie complète)
Étape  4 — payment-core : SPI (PaymentProviderSpi + ProviderRegistry)
Étape  5 — payment-core : gateway (PaymentGateway + builder)
Étape  6 — payment-test-support (MockProvider + AbstractSpiTest)
Étape  7 — payment-http-support (OkHttp, interceptors, retry)
Étape  8 — payment-provider-pawapay
Étape  9 — payment-provider-cinetpay
Étape 10 — payment-provider-monetbill
Étape 11 — payment-spring-boot-starter
Étape 12 — Publication Maven Central
```

> Étapes 1 à 6 : zéro appel réseau. Ce sont les plus importantes.
> Ne pas passer à l'étape suivante sans couverture de tests suffisante.

---

## Couverture Minimale

| Module | Minimum |
|---|---|
| `payment-core` | 90% |
| `payment-webhook-support` | 85% |
| `payment-http-support` | 80% |
| `payment-provider-*` | 75% |
| `payment-spring-boot-starter` | 70% |

---

## Conventions de Nommage

| Élément | Convention | Exemple |
|---|---|---|
| Classe / Interface | PascalCase | `PaymentRequest` |
| Méthode | camelCase | `initiate()` |
| Constante | UPPER_SNAKE_CASE | `DEFAULT_TIMEOUT` |
| Test class | Classe + Test | `PaymentRequestTest` |
| Test method | `should_action_when_condition` | `should_throw_when_amount_is_negative` |
| Commit | Conventional Commits | `feat(pawapay): implement initiate()` |

---

## Dépendances (libs.versions.toml)

```toml
[versions]
java               = "21"
okhttp             = "4.12.0"
jackson            = "2.17.1"
google-autoservice = "1.1.1"
junit              = "5.10.2"
mockito            = "5.11.0"
assertj            = "3.25.3"
wiremock           = "3.6.0"
spring-boot        = "3.3.0"
jacoco             = "0.8.12"
checkstyle         = "10.17.0"
spotbugs           = "4.8.5"
nexus-publish      = "2.0.0"
```

---

## Ce que Claude Code ne doit JAMAIS faire

- Ajouter une dépendance externe dans `payment-core`
- Retourner `null` dans une méthode publique
- Créer un setter sur un modèle de données
- Mettre de la logique métier dans un mapper
- Avaler une exception silencieusement
- Hardcoder une URL ou une clé dans le code source
- Créer un fichier dans le mauvais module
- Utiliser `var` pour les types dans les API publiques

---

## Java 21 — Utilisation Obligatoire des Features Modernes

Ce projet cible Java 21 délibérément. Utiliser Java 21 comme si c'était
Java 11 est une violation du standard de qualité. Chaque feature ci-dessous
a un contexte d'application précis et obligatoire.

---

### 1. Records — Modèles de données

**Où** : tout le package `model/` dans `payment-core`. Sans exception.

**Règle** : aucune classe de données n'est une POJO classique. Tout modèle
qui transporte des données entre modules est un `record`.

**Compact constructor obligatoire** pour la validation à la construction :

```java
public record Money(BigDecimal amount, String currency) {

    // Compact constructor — validation à la construction, pas ailleurs
    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentRequestException(
                "Amount must be strictly positive, got: " + amount
            );
        }
        if (currency.isBlank() || currency.length() != 3) {
            throw new InvalidPaymentRequestException(
                "Currency must be a valid ISO 4217 code, got: " + currency
            );
        }
        currency = currency.toUpperCase(); // normalisation dans le compact constructor
    }
}
```

**Interdit** : `new Money(null, "XAF")` ne compile pas proprement — il
explose à la construction avec un message clair. C'est l'objectif.

---

### 2. Sealed Interfaces + Classes — Hiérarchie fermée

**Où** : hiérarchie des exceptions ET `PaymentProviderSpi`.

#### 2a. Hiérarchie des exceptions (sealed)

La hiérarchie d'exceptions est **fermée**. Personne en dehors de
`payment-core` ne peut créer un nouveau sous-type de `PaymentException`
sans modifier le core. C'est intentionnel — on contrôle le contrat.

```java
// payment-core/exception/PaymentException.java
public abstract sealed class PaymentException extends RuntimeException
    permits ProviderAuthException,
            ProviderUnavailableException,
            ProviderNotFoundException,
            InvalidPaymentRequestException,
            WebhookValidationException,
            TransactionNotFoundException,
            UnsupportedProviderOperationException {

    protected PaymentException(String message) {
        super(message);
    }

    protected PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Chaque sous-classe est `final` :

```java
public final class ProviderAuthException extends PaymentException {
    public ProviderAuthException(String provider, Throwable cause) {
        super("Authentication failed for provider '%s'".formatted(provider), cause);
    }
}
```

#### 2b. PaymentProviderSpi (sealed interface)

`PaymentProviderSpi` est une `sealed interface`. Seuls les modules officiels
du projet peuvent l'implémenter directement. Les adaptateurs communautaires
passent par une interface intermédiaire `CommunityPaymentProviderSpi` qui,
elle, est ouverte.

```java
public sealed interface PaymentProviderSpi
    permits AbstractOfficialProviderSpi, CommunityPaymentProviderSpi {
    // contrat SPI
}
```

---

### 3. Pattern Matching sur Switch — Mappers de statuts

**Où** : dans chaque `XxxResponseMapper`, pour traduire les statuts natifs
du provider vers `PaymentStatus`.

**Règle** : le switch sur un type sealed ou un enum **doit être exhaustif**.
Pas de `default`. Si un nouveau statut apparaît côté provider et qu'on
l'ajoute dans l'enum, le compilateur casse le build partout où le switch
n'est pas mis à jour. C'est le filet de sécurité.

```java
// Dans PawaPayResponseMapper
private PaymentStatus mapStatus(String pawaPayStatus) {
    return switch (pawaPayStatus) {
        case "ACCEPTED"  -> PaymentStatus.PENDING;
        case "COMPLETED" -> PaymentStatus.SUCCESS;
        case "FAILED"    -> PaymentStatus.FAILED;
        case "REJECTED"  -> PaymentStatus.CANCELLED;
        case "EXPIRED"   -> PaymentStatus.EXPIRED;
        // Pas de default — si PawaPay ajoute un statut inconnu,
        // on lève une exception explicite plutôt que d'avaler silencieusement
        default -> throw new ProviderUnavailableException(
            "PawaPay returned unknown status '%s' — adapter update required"
            .formatted(pawaPayStatus)
        );
    };
}
```

**Pour les types sealed**, le switch est vérifié à la compilation — pas
de `default` possible ni nécessaire :

```java
// Exemple dans un handler d'exception
String userMessage = switch (exception) {
    case ProviderAuthException e       -> "Invalid credentials for " + e.getProvider();
    case ProviderUnavailableException e -> "Provider temporarily unavailable";
    case InvalidPaymentRequestException e -> "Invalid request: " + e.getMessage();
    // ... tous les sous-types couverts → pas de default
};
```

---

### 4. Virtual Threads — HTTP Client

**Où** : dans `payment-http-support`, configuration du dispatcher OkHttp.

**Règle** : tous les appels HTTP vers les providers s'exécutent sur des
virtual threads. L'API de `PaymentGateway` reste synchrone et bloquante
— les virtual threads rendent cette API performante sous charge sans
réécrire quoi que ce soit en réactif.

```java
// Dans PayOrchHttpClient (payment-http-support)
OkHttpClient buildClient(HttpClientConfig config) {
    return new OkHttpClient.Builder()
        .dispatcher(new Dispatcher(
            Executors.newVirtualThreadPerTaskExecutor() // Java 21
        ))
        .connectTimeout(config.connectTimeout())
        .readTimeout(config.readTimeout())
        .addInterceptor(new LoggingInterceptor())
        .addInterceptor(new RetryInterceptor(config.maxRetries()))
        .build();
}
```

**Conséquence** : 1000 appels simultanés vers PawaPay consomment 1000
virtual threads (quelques Ko chacun) plutôt que 1000 platform threads
(1 Mo chacun). Le serveur du client ne s'effondre pas sous charge.

---

### 5. Pattern Matching instanceof — Éliminer les casts

**Où** : partout où un cast explicite était nécessaire en Java 11.

**Avant (Java 11)** :
```java
if (exception instanceof ProviderAuthException) {
    ProviderAuthException authEx = (ProviderAuthException) exception;
    log.error("Auth failed: {}", authEx.getProvider());
}
```

**Après (Java 21)** :
```java
if (exception instanceof ProviderAuthException authEx) {
    log.error("Auth failed: {}", authEx.getProvider());
}
```

---

### 6. Text Blocks — Messages JSON dans les tests

**Où** : dans les tests WireMock des adapters, pour les payloads JSON de
stub. Plus lisible qu'une String concaténée.

```java
// Dans PawaPayProviderSpiTest
wireMockServer.stubFor(post("/v1/payouts")
    .willReturn(okJson("""
        {
            "payoutId": "TX-123456",
            "status": "ACCEPTED",
            "amount": "5000",
            "currency": "XAF",
            "correspondent": "MTN_MOMO_CMR",
            "created": "2026-04-18T10:00:00Z"
        }
        """)));
```

---

### 7. `String.formatted()` — Messages d'exception

**Où** : dans tous les constructeurs d'exceptions et les messages de log.
Remplace `String.format()` — même résultat, syntaxe fluide sur l'instance.

```java
throw new ProviderNotFoundException(
    "Provider '%s' not found in registry. Did you add the dependency?"
    .formatted(providerName)
);
```

---

### 8. `instanceof` Guards dans les Switch (Java 21)

**Où** : dans `ProviderRegistry` et le gestionnaire d'erreurs central,
pour dispatcher les traitements selon le type réel.

```java
String describe(Object payload) {
    return switch (payload) {
        case PaymentRequest r  -> "Payment of %s %s".formatted(r.money().amount(), r.money().currency());
        case WebhookRequest w  -> "Webhook from %s bytes body".formatted(w.body().length());
        case null              -> "null payload — reject";
        default                -> "Unknown payload type: " + payload.getClass().getSimpleName();
    };
}
```

---

### Récapitulatif — Feature Java 21 par contexte

| Feature Java 21 | Contexte d'application dans PayOrch |
|---|---|
| `record` | Tous les modèles dans `model/` |
| Compact constructor | Validation dans chaque record |
| `sealed class/interface` | Hiérarchie exceptions + `PaymentProviderSpi` |
| Pattern matching `switch` | Mappers de statuts dans chaque adapter |
| Pattern matching `instanceof` | Éliminer tous les casts explicites |
| Virtual threads | Dispatcher OkHttp dans `payment-http-support` |
| Text blocks | Payloads JSON dans les tests WireMock |
| `String.formatted()` | Messages d'exception et de log |
| Switch exhaustif (sealed) | Handlers d'exceptions dans `PaymentGateway` |

---

### Ce que Claude Code ne doit JAMAIS faire avec Java

- Créer une classe POJO avec getters/setters à la place d'un `record`
- Écrire un `switch` avec `default` sur un type `sealed` ou un `enum` connu
- Utiliser `String.format()` au lieu de `"...".formatted()`
- Créer un sous-type de `PaymentException` avec `extends` sans `permits`
- Ignorer le compact constructor et valider dans un service à la place
- Utiliser un `ThreadPoolExecutor` classique dans `payment-http-support`
  au lieu de `Executors.newVirtualThreadPerTaskExecutor()`
- Écrire `if (x instanceof Foo) { Foo f = (Foo) x; }` au lieu du
  pattern matching direct

---

## Configuration Gradle Complète

### gradle/libs.versions.toml

```toml
[versions]
java               = "21"
okhttp             = "4.12.0"
jackson            = "2.17.1"
google-autoservice = "1.1.1"
junit              = "5.10.2"
mockito            = "5.11.0"
assertj            = "3.25.3"
wiremock           = "3.6.0"
spring-boot        = "3.3.0"
jacoco             = "0.8.12"
checkstyle         = "10.17.0"
spotbugs           = "4.8.5"
nexus-publish      = "2.0.0"

[libraries]
# HTTP
okhttp                       = { module = "com.squareup.okhttp3:okhttp",                          version.ref = "okhttp" }
okhttp-mockwebserver         = { module = "com.squareup.okhttp3:mockwebserver",                   version.ref = "okhttp" }

# JSON
jackson-databind             = { module = "com.fasterxml.jackson.core:jackson-databind",          version.ref = "jackson" }
jackson-jsr310               = { module = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310", version.ref = "jackson" }

# SPI
google-autoservice           = { module = "com.google.auto.service:auto-service",                 version.ref = "google-autoservice" }
google-autoservice-ann       = { module = "com.google.auto.service:auto-service-annotations",     version.ref = "google-autoservice" }

# Tests
junit-jupiter                = { module = "org.junit.jupiter:junit-jupiter",                      version.ref = "junit" }
junit-jupiter-params         = { module = "org.junit.jupiter:junit-jupiter-params",               version.ref = "junit" }
mockito-core                 = { module = "org.mockito:mockito-core",                             version.ref = "mockito" }
mockito-junit5               = { module = "org.mockito:mockito-junit-jupiter",                    version.ref = "mockito" }
assertj-core                 = { module = "org.assertj:assertj-core",                             version.ref = "assertj" }
wiremock                     = { module = "org.wiremock:wiremock-standalone",                     version.ref = "wiremock" }

# Spring Boot
spring-boot-autoconfigure    = { module = "org.springframework.boot:spring-boot-autoconfigure",   version.ref = "spring-boot" }
spring-boot-config-processor = { module = "org.springframework.boot:spring-boot-configuration-processor", version.ref = "spring-boot" }
spring-boot-starter-test     = { module = "org.springframework.boot:spring-boot-starter-test",   version.ref = "spring-boot" }

[bundles]
jackson = ["jackson-databind", "jackson-jsr310"]
testing = ["junit-jupiter", "junit-jupiter-params", "mockito-core", "mockito-junit5", "assertj-core"]

[plugins]
nexus-publish = { id = "io.github.gradle-nexus.publish-plugin", version.ref = "nexus-publish" }
```

---

### settings.gradle.kts

```kotlin
rootProject.name = "payment-orchestrator"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

include(
    ":payment-bom",
    ":payment-core",
    ":payment-http-support",
    ":payment-webhook-support",
    ":payment-provider-pawapay",
    ":payment-provider-cinetpay",
    ":payment-provider-monetbill",
    ":payment-test-support",
    ":payment-spring-boot-starter"
)
```

---

### buildSrc/build.gradle.kts

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.0.9")
}
```

---

### buildSrc/src/main/kotlin/payorch.java-library.gradle.kts

```kotlin
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf(
        "-Xlint:all",
        "-Xlint:-processing",
        "-Werror"
    ))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading") // Java 21 + Mockito
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
    }
}

dependencies {
    testImplementation(libs.bundles.testing)
}
```

---

### buildSrc/src/main/kotlin/payorch.java-quality.gradle.kts

```kotlin
plugins {
    checkstyle
    id("com.github.spotbugs")
    jacoco
}

checkstyle {
    toolVersion = "10.17.0"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
}

spotbugs {
    toolVersion = "4.8.5"
    excludeFilter = rootProject.file("config/spotbugs/exclude.xml")
    effort = "max"
    reportLevel = "medium"
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
    dependsOn(tasks.test)
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
```

---

### buildSrc/src/main/kotlin/payorch.java-publish.gradle.kts

```kotlin
plugins {
    `maven-publish`
    signing
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name = provider { "${project.group}:${project.name}" }
                description = provider { project.description ?: "" }
                url = "https://github.com/devbackend4/payment-orchestrator"
                inceptionYear = "2026"

                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }

                developers {
                    developer {
                        id = "devbackend4"
                        name = "Saint Paul Bassanaga"
                        url = "https://github.com/devbackend4"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/devbackend4/payment-orchestrator.git"
                    developerConnection = "scm:git:ssh://github.com/devbackend4/payment-orchestrator.git"
                    url = "https://github.com/devbackend4/payment-orchestrator"
                }
            }
        }
    }
}

signing {
    val gpgKey: String? = System.getenv("GPG_PRIVATE_KEY")
    val gpgPassphrase: String? = System.getenv("GPG_PASSPHRASE")
    if (gpgKey != null && gpgPassphrase != null) {
        useInMemoryPgpKeys(gpgKey, gpgPassphrase)
        sign(publishing.publications["mavenJava"])
    }
}
```

---

### payment-bom/build.gradle.kts

```kotlin
plugins {
    `java-platform`
    id("payorch.java-publish")
}

group = "io.payorch"
version = rootProject.version
description = "PayOrch Bill of Materials"

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api(project(":payment-core"))
        api(project(":payment-http-support"))
        api(project(":payment-webhook-support"))
        api(project(":payment-provider-pawapay"))
        api(project(":payment-provider-cinetpay"))
        api(project(":payment-provider-monetbill"))
        api(project(":payment-test-support"))
        api(project(":payment-spring-boot-starter"))
    }
}
```

---

### payment-core/build.gradle.kts

```kotlin
plugins {
    id("payorch.java-library")
    id("payorch.java-quality")
    id("payorch.java-publish")
}

group = "io.payorch"
version = rootProject.version
description = "PayOrch Core — contrats, modèles et mécanisme SPI"

dependencies {
    // ZERO dépendance externe — uniquement AutoService pour le SPI
    compileOnly(libs.google.autoservice.ann)
    annotationProcessor(libs.google.autoservice)
}
```

---

### payment-http-support/build.gradle.kts

```kotlin
plugins {
    id("payorch.java-library")
    id("payorch.java-quality")
    id("payorch.java-publish")
}

group = "io.payorch"
version = rootProject.version
description = "PayOrch HTTP Support — client OkHttp partagé entre adapters"

dependencies {
    api(project(":payment-core"))
    implementation(libs.okhttp)
    testImplementation(libs.okhttp.mockwebserver)
}
```

---

### payment-webhook-support/build.gradle.kts

```kotlin
plugins {
    id("payorch.java-library")
    id("payorch.java-quality")
    id("payorch.java-publish")
}

group = "io.payorch"
version = rootProject.version
description = "PayOrch Webhook Support — parsing et validation HMAC"

dependencies {
    api(project(":payment-core"))
    implementation(project(":payment-http-support"))
}
```

---

### payment-provider-pawapay/build.gradle.kts

```kotlin
plugins {
    id("payorch.java-library")
    id("payorch.java-quality")
    id("payorch.java-publish")
}

group = "io.payorch"
version = rootProject.version
description = "PayOrch — Adapter PawaPay"

dependencies {
    api(project(":payment-core"))
    implementation(project(":payment-http-support"))
    implementation(project(":payment-webhook-support"))
    implementation(libs.bundles.jackson)
    compileOnly(libs.google.autoservice.ann)
    annotationProcessor(libs.google.autoservice)

    testImplementation(project(":payment-test-support"))
    testImplementation(libs.wiremock)
}
```

> Même structure pour `payment-provider-cinetpay` et
> `payment-provider-monetbill` — seul le `description` change.

---

### payment-test-support/build.gradle.kts

```kotlin
plugins {
    id("payorch.java-library")
    id("payorch.java-publish")
}

group = "io.payorch"
version = rootProject.version
description = "PayOrch Test Support — MockProvider et contrats de test SPI"

dependencies {
    api(project(":payment-core"))
    api(libs.junit.jupiter)
    api(libs.assertj.core)
    api(libs.mockito.core)
}
```

---

### payment-spring-boot-starter/build.gradle.kts

```kotlin
plugins {
    id("payorch.java-library")
    id("payorch.java-quality")
    id("payorch.java-publish")
}

group = "io.payorch"
version = rootProject.version
description = "PayOrch Spring Boot Starter — auto-configuration"

dependencies {
    api(project(":payment-core"))
    implementation(libs.spring.boot.autoconfigure)
    annotationProcessor(libs.spring.boot.config.processor)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(project(":payment-test-support"))
}
```

---

### build.gradle.kts (racine)

```kotlin
plugins {
    alias(libs.plugins.nexus.publish)
}

group = "io.payorch"
version = "1.0.0-SNAPSHOT"

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl = uri("https://s01.oss.sonatype.org/service/local/")
            snapshotRepositoryUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            username = System.getenv("SONATYPE_USERNAME")
            password = System.getenv("SONATYPE_PASSWORD")
        }
    }
}
```

---

## Structure Complète d'un Adapter — Exemple PawaPay

Voici tous les fichiers à créer pour un adapter, dans l'ordre exact.
Remplacer `pawapay`/`PawaPay` par le nom du provider pour les autres adapters.

```
payment-provider-pawapay/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── java/io/payorch/provider/pawapay/
    │   │   │
    │   │   ├── dto/                              ← DTOs bruts du provider
    │   │   │   ├── PawaPayPayoutRequest.java      ← record
    │   │   │   ├── PawaPayPayoutResponse.java     ← record
    │   │   │   ├── PawaPayStatusResponse.java     ← record
    │   │   │   └── PawaPayRefundRequest.java      ← record
    │   │   │
    │   │   ├── mapper/                           ← Fonctions pures, zéro état
    │   │   │   ├── PawaPayRequestMapper.java      ← PaymentRequest → DTO
    │   │   │   └── PawaPayResponseMapper.java     ← DTO → PaymentResult
    │   │   │
    │   │   ├── PawaPayClient.java                ← HTTP uniquement
    │   │   └── PawaPayProviderSpi.java           ← implémente PaymentProviderSpi
    │   │
    │   └── resources/
    │       └── META-INF/services/
    │           └── io.payorch.core.spi.PaymentProviderSpi
    │               (contenu : io.payorch.provider.pawapay.PawaPayProviderSpi)
    │
    └── test/
        └── java/io/payorch/provider/pawapay/
            ├── PawaPayProviderSpiTest.java        ← étend AbstractPaymentProviderSpiTest
            ├── PawaPayClientTest.java             ← tests WireMock
            ├── mapper/
            │   ├── PawaPayRequestMapperTest.java  ← tests purs, zéro mock
            │   └── PawaPayResponseMapperTest.java ← tests purs, zéro mock
            └── fixture/
                └── PawaPayFixtures.java           ← données de test réutilisables
```

### Responsabilités strictes par fichier

| Fichier | Peut faire | Ne peut pas faire |
|---|---|---|
| `PawaPayProviderSpi` | Orchestrer, valider capabilities, déléguer | Appels HTTP, logique de mapping |
| `PawaPayClient` | Appels HTTP, désérialisation JSON brute | Mapping vers types PayOrch |
| `PawaPayRequestMapper` | Traduire `PaymentRequest` → DTO | Appel réseau, état mutable |
| `PawaPayResponseMapper` | Traduire DTO → `PaymentResult` | Appel réseau, état mutable |
| DTOs | Porter les données brutes du provider | Logique métier |

### Fichier META-INF/services

Ce fichier doit contenir exactement une ligne — le nom complet de la classe
qui implémente `PaymentProviderSpi` :

```
io.payorch.provider.pawapay.PawaPayProviderSpi
```

Ce fichier est généré automatiquement par Google AutoService si
`@AutoService(PaymentProviderSpi.class)` est présent sur la classe.
Ne pas créer ce fichier manuellement si AutoService est activé.

---

## Variables d'Environnement Requises

Ces variables ne sont jamais hardcodées dans le code source. Jamais.

| Variable | Usage |
|---|---|
| `GPG_PRIVATE_KEY` | Signature GPG pour Maven Central |
| `GPG_PASSPHRASE` | Passphrase de la clé GPG |
| `SONATYPE_USERNAME` | Compte Sonatype OSSRH |
| `SONATYPE_PASSWORD` | Mot de passe Sonatype |
| `PAWAPAY_API_KEY` | Tests d'intégration PawaPay (sandbox) |
| `CINETPAY_API_KEY` | Tests d'intégration CinetPay (sandbox) |
| `MONETBILL_API_KEY` | Tests d'intégration MonetBill (sandbox) |