SET search_path = pg_catalog;

REVOKE ALL(c1) ON TABLE public.t1 FROM maindb;
REVOKE ALL(c1) ON TABLE public.t1 FROM PUBLIC;
REVOKE ALL(c1) ON TABLE public.t1 FROM botov_av;
REVOKE ALL(c1) ON TABLE public.t1 FROM botov_av;