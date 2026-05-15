-- V6__refactor_fecha_hora_transacciones.sql
-- Consolida las columnas fecha y hora en una sola marca de tiempo.

ALTER TABLE public.transacciones
    ADD COLUMN fecha_hora TIMESTAMP;

UPDATE public.transacciones
SET fecha_hora = fecha + hora
WHERE fecha_hora IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.transacciones
        WHERE fecha_hora IS NULL
    ) THEN
        RAISE EXCEPTION 'No se pudo migrar fecha_hora para todas las transacciones';
    END IF;
END $$;

ALTER TABLE public.transacciones
    ALTER COLUMN fecha_hora SET NOT NULL;

ALTER TABLE public.transacciones
    DROP COLUMN fecha,
    DROP COLUMN hora;
