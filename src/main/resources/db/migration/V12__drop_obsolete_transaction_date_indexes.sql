-- V12__drop_obsolete_transaction_date_indexes.sql
-- Elimina indices obsoletos que dependian de la columna fecha reemplazada por fecha_hora.

DROP INDEX IF EXISTS public.idx_transacciones_origen_fecha;

DROP INDEX IF EXISTS public.idx_transacciones_destino_fecha;
