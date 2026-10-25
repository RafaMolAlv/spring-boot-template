# ATM API

API REST a la que se conectan los cajeros automáticos de la entidad y de otros bancos.
Primera versión funcional desarrollada en el tiempo de la prueba técnica.

## Stack

- Java 21 · Spring Boot 3.5 (Web, Data JPA, Validation)
- H2 en memoria con datos de ejemplo (`src/main/resources/data.sql`)
- PIN hasheado con BCrypt (`spring-security-crypto`, sin el resto de Spring Security)
- JUnit 5 + Mockito (unitarios) y `@SpringBootTest` + MockMvc (integración)
- Docker + GitHub Actions

## Cómo ejecutarlo

```bash
# Tests unitarios + integración y empaquetado
./mvnw verify          # Windows: .\mvnw.cmd verify

# Arrancar en local (http://localhost:8080)
./mvnw spring-boot:run

# Con Docker
docker compose up --build
```

Consola de H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:atmdb`, usuario `sa`, sin contraseña).

## Endpoints

Todas las operaciones se hacen en un cajero con una tarjeta. El PIN va siempre en el cuerpo de la petición,
nunca en la URL.

Base: `/api/atms/{atmId}/cards/{cardNumber}`

| Método | Ruta | Cuerpo | Descripción |
|---|---|---|---|
| POST | `/activation` | `{"currentPin","newPin"}` | Activa la tarjeta y obliga a cambiar el PIN inicial |
| PUT | `/pin` | `{"currentPin","newPin"}` | Cambia el PIN |
| POST | `/withdrawals` | `{"pin","amount"}` | Retira efectivo |
| POST | `/deposits` | `{"pin","amount"}` | Ingresa efectivo |
| POST | `/transfers` | `{"pin","destinationIban","amount","concept"}` | Transferencia |
| POST | `/accounts/{iban}/movements` | `{"pin"}` | Movimientos de una cuenta del titular |

Los errores devuelven `{"code","message","timestamp"}` con el código HTTP adecuado
(401 PIN incorrecto, 403 tarjeta sin activar o cuenta ajena, 404 no encontrado, 422 regla de negocio, 400 validación).

### Ejemplo

```bash
curl -X POST localhost:8080/api/atms/2/cards/4000000000000001/withdrawals \
  -H "Content-Type: application/json" -d '{"pin":"1234","amount":100}'
# {"iban":"ES0690000001000000000001","amount":100,"fee":2.00,"balance":898.00}
```

### Datos de ejemplo

Nuestro banco tiene el código de entidad `9000` (configurable en `bank.code`).

| Tarjeta | Tipo | Cuenta | Saldo | Límite retirada | Crédito | Estado | PIN |
|---|---|---|---|---|---|---|---|
| 4000000000000001 | Débito | ES0690000001000000000001 (Ana) | 1000 | 600 | — | Activa | 1234 |
| 4000000000000002 | Crédito | ES7690000001000000000002 (Ana) | 200 | 1000 | 1500 | Activa | 1234 |
| 4000000000000003 | Débito | ES4990000001000000000003 (Luis) | 500 | 500 | — | Sin activar | 0000 |

Cajeros: `1` es de nuestra entidad (sin comisión) y `2` es de la entidad `9100` (comisión de 2,00 €).
IBAN externo válido para pruebas: `ES4491000001000000000004`.

## Decisiones y supuestos

- **Límite de la tarjeta**: es por operación. La comisión del cajero ajeno no cuenta para ese límite,
  pero sí para el saldo disponible.
- **Crédito**: con tarjeta de crédito se dispone de `saldo + crédito máximo`; la cuenta puede quedar en negativo.
- **Comisiones**: se registran como un movimiento `FEE` independiente, así el cliente ve cuánto le han cobrado.
- **Transferencias**: salen de la cuenta de la tarjeta. Si el IBAN destino es de nuestra entidad, la cuenta debe
  existir y recibe un movimiento `TRANSFER_IN`. Si es de otra entidad, se cobra la comisión configurada
  (`bank.external-transfer-fee`) y solo se registra la salida. La entidad se identifica por las posiciones 5-8
  del IBAN español; cualquier IBAN de otro país se trata como externo.
- **IBAN**: validación ISO 13616 (mod-97), admite espacios y minúsculas.
- **Activación**: es un único paso en el que se valida el PIN inicial y se fija uno nuevo distinto.
  Sin activar, la tarjeta no puede hacer ninguna otra operación (tampoco cambiar el PIN).
- **Movimientos**: se pueden consultar los de cualquier cuenta del titular de la tarjeta, desde cualquier cajero.
  El ingreso es la única operación restringida a cajeros propios.
- **Concurrencia**: `@Version` en `Account` (bloqueo optimista) para que dos operaciones simultáneas no pisen el saldo.
- **Importes**: `BigDecimal`, máximo 2 decimales.

## Git Flow y CI/CD

- `main`: versiones publicadas (tags `vX.Y.Z`). `develop`: integración. `feature/*`: una rama por funcionalidad,
  integradas en `develop` con `--no-ff`. `release/*`: preparación de versión.
- GitHub Actions (`.github/workflows/ci.yml`): en cada push ejecuta `./mvnw verify` (tests unitarios y de integración).
  En `develop` y `main` además construye la imagen Docker y la publica en GitHub Container Registry
  (`ghcr.io/<usuario>/<repo>:<rama>`).

## Uso de IA

Se ha usado un asistente de IA para acelerar la implementación de ciertas funcionalidades (en concreto, Claude).
El análisis de requisitos, las decisiones de diseño, la revisión del código y la integración
feature a feature en Git Flow son propias.
