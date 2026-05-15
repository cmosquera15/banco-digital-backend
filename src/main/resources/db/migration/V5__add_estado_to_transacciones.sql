-- V5__add_estado_to_transacciones.sql
-- Agrega el catalogo de estados para transacciones y asigna estado inicial a datos existentes.

CREATE TABLE IF NOT EXISTS public.estados_transaccion (
    id_estado_transaccion SERIAL PRIMARY KEY,
    nombre                VARCHAR(30) UNIQUE NOT NULL
);

INSERT INTO public.estados_transaccion (nombre)
VALUES
    ('COMPLETADA'),
    ('RECHAZADA'),
    ('PENDIENTE')
ON CONFLICT (nombre) DO NOTHING;

ALTER TABLE public.transacciones
    ADD COLUMN IF NOT EXISTS id_estado_transaccion INTEGER;

UPDATE public.transacciones
SET id_estado_transaccion = (
    SELECT id_estado_transaccion
    FROM public.estados_transaccion
    WHERE nombre = 'COMPLETADA'
)
WHERE id_estado_transaccion IS NULL;

ALTER TABLE public.transacciones
    ADD CONSTRAINT fk_transaccion_estado
        FOREIGN KEY (id_estado_transaccion)
        REFERENCES public.estados_transaccion (id_estado_transaccion);

ALTER TABLE public.transacciones
    ALTER COLUMN id_estado_transaccion SET NOT NULL;
