--
-- PostgreSQL database dump
--

SET client_encoding = 'UTF8';
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA public IS 'Standard public schema';


--
-- Name: plpgsql; Type: PROCEDURAL LANGUAGE; Schema: -; Owner: 
--

CREATE PROCEDURAL LANGUAGE plpgsql;


SET search_path = pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: mode_bool_final(integer[]); Type: FUNCTION; Schema: public; Owner: shamsutdinov_lr
--

CREATE FUNCTION public.mode_bool_final(integer[]) RETURNS boolean
    LANGUAGE sql
    AS $_$
SELECT CASE WHEN ( $1[1] = 0 AND $1[2] = 0 )
THEN NULL
ELSE $1[1] >= $1[2]
END;
$_$;


ALTER FUNCTION public.mode_bool_final(integer[]) OWNER TO shamsutdinov_lr;

--
-- Name: mode_bool_final(integer[], boolean, integer, text); Type: FUNCTION; Schema: public; Owner: shamsutdinov_lr
--

CREATE FUNCTION public.mode_bool_final(integer[], boolean, integer, text) RETURNS boolean
    LANGUAGE sql
    AS $_$
SELECT CASE WHEN ( $1[1] = 0 AND $1[2] = 0 )
THEN NULL
ELSE $1[1] >= $1[2]
END;
$_$;


ALTER FUNCTION public.mode_bool_final(integer[], boolean, integer, text) OWNER TO shamsutdinov_lr;

--
-- Name: mode_bool_state(integer[]); Type: FUNCTION; Schema: public; Owner: shamsutdinov_lr
--

CREATE FUNCTION public.mode_bool_state(integer[]) RETURNS integer[]
    LANGUAGE sql
    AS $_$
SELECT CASE 1 > 2
WHEN TRUE THEN
    array[ $1[1] + 1, $1[2] ]
WHEN FALSE THEN
    array[ $1[1], $1[2] + 1 ]
ELSE
    $1
END;
$_$;


ALTER FUNCTION public.mode_bool_state(integer[]) OWNER TO shamsutdinov_lr;

--
-- Name: mode_bool_state(integer[], boolean); Type: FUNCTION; Schema: public; Owner: shamsutdinov_lr
--

CREATE OR REPLACE FUNCTION public.mode_bool_state(integer[], boolean) RETURNS integer[]
    LANGUAGE sql
    AS $_$
SELECT CASE $2
WHEN TRUE THEN
    array[ $1[1] + 1, $1[2] ]
WHEN FALSE THEN
    array[ $1[1], $1[2] + 1 ]
ELSE
    $1
END;
$_$;


ALTER FUNCTION public.mode_bool_state(integer[], boolean) OWNER TO shamsutdinov_lr;

--
-- Name: mode_bool_state(integer[], boolean, text); Type: FUNCTION; Schema: public; Owner: shamsutdinov_lr
--

CREATE FUNCTION public.mode_bool_state(integer[], boolean, text) RETURNS integer[]
    LANGUAGE sql
    AS $_$
SELECT CASE 1 > 2
WHEN TRUE THEN
    array[ $1[1] + 1, $1[2] ]
WHEN FALSE THEN
    array[ $1[1], $1[2] + 1 ]
ELSE
    $1
END;
$_$;


ALTER FUNCTION public.mode_bool_state(integer[], boolean, text) OWNER TO shamsutdinov_lr;

--
-- Name: mode1(boolean); Type: AGGREGATE; Schema: public; Owner: shamsutdinov_lr
--

CREATE AGGREGATE public.mode1(boolean) (
    SFUNC = public.mode_bool_state,
    STYPE = integer[],
    INITCOND = '{0,0}',
    FINALFUNC = public.mode_bool_final
);


ALTER AGGREGATE public.mode1(boolean) OWNER TO shamsutdinov_lr;

-- AGGREGATE public.mode1(boolean) GRANT

GRANT ALL ON FUNCTION public.mode1(boolean) TO test_user;

--
-- Name: mode3(*); Type: AGGREGATE; Schema: public; Owner: shamsutdinov_lr
--

CREATE AGGREGATE public.mode3(*) (
    SFUNC = public.mode_bool_state,
    STYPE = integer[],
    INITCOND = '{0,0}',
    FINALFUNC = public.mode_bool_final
);


ALTER AGGREGATE public.mode3(*) OWNER TO shamsutdinov_lr;

-- AGGREGATE public.mode3() GRANT

GRANT ALL ON FUNCTION public.mode3() TO test_user;

--
-- Name: mode4(boolean); Type: AGGREGATE; Schema: public; Owner: shamsutdinov_lr
--

CREATE AGGREGATE public.mode4(ORDER BY boolean) (
    SFUNC = public.mode_bool_state,
    STYPE = integer[],
    INITCOND = '{0,0}',
    FINALFUNC = public.mode_bool_final
);


ALTER AGGREGATE public.mode4(ORDER BY boolean) OWNER TO shamsutdinov_lr;

-- AGGREGATE public.mode4(boolean) GRANT

GRANT ALL ON FUNCTION public.mode4(boolean) TO test_user;

--
-- Name: mode11(boolean, integer, text, boolean, text); Type: AGGREGATE; Schema: public; Owner: shamsutdinov_lr
--

CREATE AGGREGATE public.mode11(boolean, integer, text ORDER BY boolean, text) (
    SFUNC = public.mode_bool_state,
    STYPE = integer[],
    INITCOND = '{0,0}',
    FINALFUNC = public.mode_bool_final
);


ALTER AGGREGATE public.mode11(boolean, integer, text ORDER BY boolean, text) OWNER TO shamsutdinov_lr;

-- AGGREGATE public.mode11(boolean, integer, text, boolean, text) GRANT

GRANT ALL ON FUNCTION public.mode11(boolean, integer, text, boolean, text) TO test_user;

