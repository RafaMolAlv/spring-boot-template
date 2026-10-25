-- Datos de ejemplo. PIN de todas las tarjetas activas: 1234. Tarjeta pendiente de activar: 0000.
INSERT INTO customer (id, name) VALUES (1, 'Ana García');
INSERT INTO customer (id, name) VALUES (2, 'Luis Pérez');

INSERT INTO account (id, iban, balance, customer_id, version) VALUES (1, 'ES0690000001000000000001', 1000.00, 1, 0);
INSERT INTO account (id, iban, balance, customer_id, version) VALUES (2, 'ES7690000001000000000002', 200.00, 1, 0);
INSERT INTO account (id, iban, balance, customer_id, version) VALUES (3, 'ES4990000001000000000003', 500.00, 2, 0);

-- Tarjeta Débito activa, límite 600 por operación
INSERT INTO card (id, card_number, type, account_id, pin_hash, active, withdrawal_limit, credit_limit)
VALUES (1, '4000000000000001', 'DEBIT', 1, '$2a$10$GDPSL2twdVfwm7WS6upliOuRVUOy1KT5RSW/gcCXiLf9giT3WgeXi', TRUE, 600.00, NULL);
-- Tarjeta Crédito activa, límite 1000 por operación, crédito máximo 1500
INSERT INTO card (id, card_number, type, account_id, pin_hash, active, withdrawal_limit, credit_limit)
VALUES (2, '4000000000000002', 'CREDIT', 2, '$2a$10$GDPSL2twdVfwm7WS6upliOuRVUOy1KT5RSW/gcCXiLf9giT3WgeXi', TRUE, 1000.00, 1500.00);
-- Tarjeta Débito pendiente de activar (PIN inicial 0000)
INSERT INTO card (id, card_number, type, account_id, pin_hash, active, withdrawal_limit, credit_limit)
VALUES (3, '4000000000000003', 'DEBIT', 3, '$2a$10$SWbrJKnWCpqitX.6wJHYs.L0iic5sX/LAfAymWWjOseXyDb2rSMmq', FALSE, 500.00, NULL);

-- Cajero propio (sin comisión) y cajero de otra entidad (comisión 2,00)
INSERT INTO atm (id, bank_code, withdrawal_fee) VALUES (1, '9000', 0.00);
INSERT INTO atm (id, bank_code, withdrawal_fee) VALUES (2, '9100', 2.00);

INSERT INTO movement (account_id, type, amount, description, created_at)
VALUES (1, 'DEPOSIT', 1000.00, 'Ingreso inicial', TIMESTAMP '2026-10-25 11:00:00');
