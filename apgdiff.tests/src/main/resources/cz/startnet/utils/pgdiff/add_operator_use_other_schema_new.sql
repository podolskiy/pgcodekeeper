CREATE SCHEMA tester;

CREATE SCHEMA tester2;

CREATE OR REPLACE FUNCTION tester.nonull_append_strings(text, text) RETURNS boolean
    LANGUAGE sql IMMUTABLE
    AS $_$
    SELECT CASE WHEN $1 IS NULL THEN FALSE
            WHEN $2 IS NULL THEN FALSE
            ELSE TRUE
            END;
    $_$;

ALTER FUNCTION tester.nonull_append_strings(text, text) OWNER TO shamsutdinov_lr;

CREATE OPERATOR public.||++ (
    PROCEDURE = tester.nonull_append_strings,
    LEFTARG = text,
    RIGHTARG = text,
    COMMUTATOR = OPERATOR(tester2.||+++),
    NEGATOR = OPERATOR(tester2.||+-+),
    MERGES,
    HASHES,
    RESTRICT = neqsel,
    JOIN = neqjoinsel
);

ALTER OPERATOR public.||++(text, text) OWNER TO shamsutdinov_lr;

COMMENT ON OPERATOR public.||++(text, text) IS 'Тестовый комментарий';