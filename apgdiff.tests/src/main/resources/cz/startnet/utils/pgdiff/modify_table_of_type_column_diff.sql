-- DEPCY: This CONSTRAINT depends on the TYPE: testtype

ALTER TABLE testtable
	DROP CONSTRAINT testtable_pkey;

-- DEPCY: This TABLE depends on the TYPE: testtype

DROP TABLE testtable;

ALTER TYPE testtype
	ADD ATTRIBUTE field4new numeric, 
	DROP ATTRIBUTE field4;

-- DEPCY: This TABLE is a dependency of COLUMN: testtable.field4new

CREATE TABLE testtable OF public.testtype (
	field1 WITH OPTIONS NOT NULL,
	field2 WITH OPTIONS DEFAULT 1000,
	field3 WITH OPTIONS DEFAULT 'word'::text,
	field4new WITH OPTIONS DEFAULT 2000
);

ALTER TABLE testtable OWNER TO shamsutdinov_lr;

ALTER TABLE testtable
	ADD CONSTRAINT testtable_pkey PRIMARY KEY (field1);
