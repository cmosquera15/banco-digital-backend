-- V9__reinforce_integrity_and_transaction_indexes.sql
-- Refuerza integridad referencial e indices para consultas financieras frecuentes.

ALTER TABLE public.cuentas
    ALTER COLUMN num_doc_dueno TYPE VARCHAR(50);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_cuenta_estado'
          AND conrelid = 'public.cuentas'::regclass
    ) THEN
        ALTER TABLE public.cuentas
            ADD CONSTRAINT fk_cuenta_estado
                FOREIGN KEY (id_estado_cuenta)
                REFERENCES public.estados_cuenta (id_estado_cuenta);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_cuentas_id_estado_cuenta
    ON public.cuentas (id_estado_cuenta);

CREATE INDEX IF NOT EXISTS idx_transacciones_origen_fecha_hora
    ON public.transacciones (id_cuenta_origen, fecha_hora);

CREATE INDEX IF NOT EXISTS idx_transacciones_destino_fecha_hora
    ON public.transacciones (id_cuenta_destino, fecha_hora);

CREATE INDEX IF NOT EXISTS idx_transacciones_id_estado_transaccion
    ON public.transacciones (id_estado_transaccion);
