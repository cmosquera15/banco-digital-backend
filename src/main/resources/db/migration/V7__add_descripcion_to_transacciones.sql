-- V7__add_descripcion_to_transacciones.sql
-- Agrega una descripcion opcional para trazabilidad de transacciones.

ALTER TABLE public.transacciones
    ADD COLUMN descripcion VARCHAR(255);
