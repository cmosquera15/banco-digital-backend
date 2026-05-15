-- V8__add_financial_check_constraints.sql
-- Refuerza reglas financieras basicas sobre montos y saldos.

ALTER TABLE public.transacciones
    ADD CONSTRAINT chk_transaccion_monto
        CHECK (monto > 0);

ALTER TABLE public.cuentas
    ADD CONSTRAINT chk_cuenta_saldo
        CHECK (saldo >= 0);
