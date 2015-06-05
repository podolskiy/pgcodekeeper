SET search_path = public, pg_catalog;

CREATE TABLE t1 (
    c1 bigInt,
    c2 integer
);

CREATE TABLE t2 (
    c3 integer,
    c4 bigInt
);

CREATE VIEW v1 AS
    select t1.*, t2.c3 from t1, t2;