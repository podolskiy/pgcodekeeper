CREATE OR REPLACE FUNCTION public.f1() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
  RETURN NEW;
END;
$$;

CREATE TABLE public.t1 (
    c1 bigint NOT NULL,
    c2 integer NOT NULL
);

CREATE TRIGGER tr
    AFTER INSERT OR UPDATE ON public.t1
    FOR EACH ROW
    EXECUTE PROCEDURE public.f1();