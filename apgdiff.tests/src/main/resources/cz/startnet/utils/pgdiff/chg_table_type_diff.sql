SET search_path = pg_catalog;

ALTER TABLE public.cities
	DETACH PARTITION public.cities_ab;

ALTER TABLE public.cities_ab OF public.comp;
