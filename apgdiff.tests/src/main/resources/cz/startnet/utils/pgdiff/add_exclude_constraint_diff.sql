SET search_path = public, pg_catalog;

ALTER TABLE testtable
	ADD CONSTRAINT testtable2_c_excl EXCLUDE USING gist (c WITH &&);

ALTER TABLE testtable
	ADD CONSTRAINT test EXCLUDE USING id(test WITH =) INITIALLY DEFERRED;