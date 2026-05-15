-- V13__seed_transferencia_tipo_transaccion.sql
-- Asegura el tipo de transaccion requerido para transferencias bancarias.

INSERT INTO public.tipos_transaccion (nombre)
VALUES ('TRANSFERENCIA')
ON CONFLICT (nombre) DO NOTHING;
