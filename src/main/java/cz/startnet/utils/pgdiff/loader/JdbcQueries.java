package cz.startnet.utils.pgdiff.loader;

public interface JdbcQueries {
// SONAR-OFF
    String QUERY_TABLES_PER_SCHEMA = 
          "SELECT " 
        + "        subselectColumns.oid::bigint, "
        + "        subselectColumns.relname, "
        + "        subselectColumns.relowner::bigint, "
        + "        subselectColumns.aclArray, "
        + "        subselectColumns.col_numbers, "
        + "        subselectColumns.col_names, "
        + "        subselectColumns.col_types::bigint[], "
        + "        subselectColumns.col_defaults,"
        + "        subselectColumns.col_comments, "
        + "        subselectColumns.col_typemod, "
        + "        subselectColumns.col_notnull, "
        + "        subselectColumns.seqs,"
        + "        comments.description AS table_comment, "
        + "        subselectInherits.inherited, "
        + "        subselectColumns.reloptions, "
        + "        array_agg(dep.deptype) AS deptype "
        + "FROM "
        + "         (SELECT "
        + "             columnsData.oid, "
        + "             columnsData.relname, "
        + "             columnsData.relowner, "
        + "             columnsData.aclArray, "
        + "             array_agg(columnsData.attnum) AS col_numbers, "
        + "             array_agg(columnsData.attname) AS col_names, "
        + "             array_agg(columnsData.atttypid) AS col_types, "
        + "             array_agg(columnsData.defaults) AS col_defaults, "
        + "             array_agg(columnsData.description) AS col_comments, "
        + "             array_agg(columnsData.atttypmod) AS col_typemod, "
        + "             array_agg(columnsData.attnotnull) AS col_notnull, "
        + "             array_agg(columnsData.col_seq) AS seqs, "
        + "             columnsData.reloptions "
        + "         FROM "
        + "             (SELECT "
        + "                 c.oid, "
        + "                 c.relname, "
        + "                 c.relowner::bigint, "
        + "                 c.relacl AS aclArray, "
        + "                 attr.attnum::integer, "
        + "                 attr.attname, "
        + "                 attr.atttypid::bigint, "
        + "                 pg_catalog.pg_get_expr(attrdef.adbin, attrdef.adrelid) AS defaults, "
        + "                 comments.description, "
        + "                 attr.atttypmod, "
        + "                 attr.attnotnull, "
        + "                 (SELECT oid::regclass::text FROM pg_catalog.pg_class c2 WHERE c2.oid = depseq.refobjid AND c2.relkind = 'S') col_seq, "
        + "                 c.reloptions "
        + "             FROM "
        + "                 pg_catalog.pg_class c "
        + "                 JOIN pg_catalog.pg_attribute attr "
        + "                 ON c.oid = attr.attrelid AND "
        + "                    attr.attisdropped IS FALSE "
        + "                 LEFT JOIN pg_catalog.pg_attrdef attrdef "
        + "                 ON attrdef.adnum = attr.attnum AND "
        + "                    attr.attrelid = attrdef.adrelid "
        + "                 LEFT JOIN pg_catalog.pg_description comments "
        + "                 ON comments.objoid = attr.attrelid AND "
        + "                    comments.objsubid = attr.attnum "
        + "                 LEFT JOIN pg_catalog.pg_depend depseq "
        + "                 ON attrdef.oid = depseq.objid AND "
        + "                    depseq.refobjid != c.oid "
        + "             WHERE "
        + "                 c.relnamespace = ? AND "
        + "                 c.relkind = 'r' AND "
        + "                 c.oid = attr.attrelid "
        + "             ORDER BY "
        + "                attr.attnum "
        + "             ) columnsData "
        + "         GROUP BY "
        + "             columnsData.oid, "
        + "             columnsData.relname, "
        + "             columnsData.relowner, "
        + "             columnsData.aclArray, "
        + "             columnsData.reloptions ) subselectColumns "
        + "        LEFT JOIN pg_catalog.pg_description comments "
        + "        ON comments.objoid = subselectColumns.oid AND "
        + "           comments.objsubid = 0 "
        + "        LEFT JOIN (SELECT "
        + "                         array_agg(subinh.inherits)::text[] AS inherited,"
        + "                         subinh.inhrelid"
        + "                   FROM"
        + "                         (SELECT"
        + "                                 inhrelid,"
        + "                                 inh.inhparent::regclass AS inherits,"
        + "                                 inh.inhseqno"
        + "                          FROM"
        + "                                 pg_catalog.pg_inherits inh"
        + "                          ORDER BY"
        + "                                 inhrelid,"
        + "                                 inh.inhseqno"
        + "                         ) subinh"
        + "                   GROUP BY"
        + "                         subinh.inhrelid"
        + "                  ) subselectInherits "
        + "        ON subselectInherits.inhrelid = subselectColumns.oid "
        + "        JOIN pg_catalog.pg_depend dep "
        + "        ON subselectColumns.oid = dep.objid "
        + "GROUP BY "
        + "        subselectColumns.oid, "
        + "        subselectColumns.relname, "
        + "        subselectColumns.relowner, "
        + "        subselectColumns.aclArray, "
        + "        subselectColumns.col_numbers, "
        + "        subselectColumns.col_names, "
        + "        subselectColumns.col_types, "
        + "        subselectColumns.col_defaults, "
        + "        subselectColumns.col_comments, "
        + "        subselectColumns.col_typemod, "
        + "        subselectColumns.col_notnull, "
        + "        subselectColumns.seqs, "
        + "        table_comment, "
        + "        inherited, "
        + "        subselectColumns.reloptions";
    
    String QUERY_FUNCTIONS_PER_SCHEMA = 
            "SELECT "
            + "     proname, "
            + "     proowner, "
            + "     (SELECT lanname FROM pg_catalog.pg_language l WHERE l.oid = prolang) lang_name, "
            + "     prosrc, "
            + "     proiswindow, "
            + "     provolatile, "
            + "     proleakproof, "
            + "     proisstrict, "
            + "     prosecdef, "
            + "     procost::real, "
            + "     prorows::real, "
            + "     proconfig, "
            + "     probin, "
            + "     prorettype::bigint, "
            + "     proallargtypes::bigint[], "
            + "     proargmodes, "
            + "     proargnames, "
            + "     pg_get_function_arguments(p.oid) AS proarguments, "
            + "     pg_get_function_identity_arguments(p.oid) AS proarguments_without_default, "
            + "     proargdefaults, "
            + "     proacl AS aclArray,"
            + "     d.description AS comment,"
            + "     proretset, "
            + "     array_agg(dep.deptype) AS deps "
            + "FROM "
            + "     pg_catalog.pg_proc p "
            + "     LEFT JOIN "
            + "         pg_catalog.pg_description d "
            + "     ON "
            + "         d.objoid = p.oid "
            + "     LEFT JOIN "
            + "         pg_catalog.pg_depend dep "
            + "     ON "
            + "         p.oid = dep.objid "
            + "WHERE "
            + "     pronamespace = ? AND "
            + "     proisagg = FALSE "
            + "GROUP BY "
            + "     p.oid, "
            + "     proname, "
            + "     proowner, "
            + "     lang_name, "
            + "     prosrc, "
            + "     proiswindow, provolatile, proleakproof, proisstrict, prosecdef, procost, prorows, proconfig, probin, "
            + "     prorettype, "
            + "     proowner, "
            + "     proallargtypes, "
            + "     proargmodes, "
            + "     proargnames, "
            + "     proarguments, "
            + "     proargdefaults,"
            + "     comment,"
            + "     aclArray,"
            + "     proretset";
    
    String QUERY_SEQUENCES_PER_SCHEMA = 
            "SELECT "
            + "     c.oid AS sequence_oid,"
            + "     c.relowner,"
            + "     c.relname,"
            + "     p.start_value::bigint AS start_value,"
            + "     p.minimum_value::bigint AS minimum_value,"
            + "     p.maximum_value::bigint AS maximum_value,"
            + "     p.increment::bigint AS increment, "
            + "     p.cycle_option AS cycle_option,"
            + "     d.refobjsubid AS referenced_column,"
            + "     d.refobjid::regclass::text referenced_table_name,"
            + "     a.attname AS ref_col_name,"
            + "     c.relacl AS aclArray "
            + "FROM "
            + "     pg_catalog.pg_class c "
            + "     LEFT JOIN "
            + "         pg_catalog.pg_depend d "
            + "     ON "
            + "         d.objid = c.oid AND "
            + "         d.refobjsubid != 0"
            + "     LEFT JOIN "
            + "         pg_catalog.pg_attribute a "
            + "     ON "
            + "         a.attrelid = d.refobjid AND "
            + "         a.attnum = d.refobjsubid AND "
            + "         a.attisdropped IS FALSE, "
            + "     pg_sequence_parameters(c.oid) p(start_value, minimum_value, maximum_value, increment, cycle_option)"
            + "WHERE "
            + "     c.relnamespace = ? "
            + "     AND c.relkind = 'S' "
            + "GROUP BY "
            + "     sequence_oid,"
            + "     relowner,"
            + "     relname,"
            + "     start_value,"
            + "     minimum_value,"
            + "     maximum_value,"
            + "     increment,"
            + "     cycle_option,"
            + "     referenced_column,"
            + "     referenced_table_name,"
            + "     ref_col_name,"
            + "     aclArray";
    
    String QUERY_INDECIES_PER_SCHEMA = 
            "SELECT "
            + "     ccc.relname AS table_name, "
            + "     i.indisunique, "
            + "     c.relname, "
            + "     (SELECT n.nspname FROM pg_catalog.pg_namespace n WHERE c.relnamespace = n.oid) namespace, "
            + "     c.relowner, "
            + "     definition "
            + "FROM "
            + "     pg_catalog.pg_class ccc "
            + "     RIGHT JOIN pg_catalog.pg_index i "
            + "     ON ccc.oid = i.indrelid "
            + "     JOIN pg_catalog.pg_class c ON c.oid = i.indexrelid "
            + "     LEFT JOIN pg_catalog.pg_constraint cons ON cons.conindid = i.indexrelid, "
            + "     pg_get_indexdef(c.oid) definition "
            + "WHERE "
            + "     ccc.relkind = 'r' AND "
            + "     ccc.relnamespace = ? AND "
            + "     i.indisprimary = FALSE AND "
            + "     i.indisexclusion = FALSE AND"
            + "     cons.conindid is NULL";
    
    String QUERY_CONSTRAINTS_PER_SCHEMA = 
            "SELECT "
            + "     ccc.relname,"
            + "     conname,"
            + "     contype,"
            + "     conrelid,"
            + "     consrc,"
            + "     conkey::integer[],"
            + "     confrelid,"
            + "     confrelid::regclass::text AS confrelid_name,"
            + "     confkey::integer[],"
            + "     confupdtype,"
            + "     confdeltype, "
            + "     confmatchtype,"
            + "     description "
            + "FROM "
            + "     pg_catalog.pg_class ccc"
            + "     RIGHT JOIN pg_catalog.pg_constraint c"
            + "     ON ccc.oid = c.conrelid"
            + "     LEFT JOIN pg_catalog.pg_description d"
            + "     ON c.oid = d.objoid "
            + "WHERE "
            + "     ccc.relkind = 'r' AND ccc.relnamespace = ?";
    
    String QUERY_COLUMNS_PER_SCHEMA = 
            "SELECT "
            + "     a.attname, "
            + "     a.attnum, "
            + "     a.attrelid "
            + "FROM "
            + "     pg_catalog.pg_attribute a "
            + "     JOIN "
            + "         pg_catalog.pg_class c "
            + "     ON "
            + "         c.oid = a.attrelid AND "
            + "         a.attisdropped IS FALSE "
            + "WHERE "
            + "     c.relnamespace = ? AND "
            + "     c.relkind IN ('i', 'r') "
            + "ORDER BY "
            + "     a.attrelid";
    
    String QUERY_EXTENSIONS = 
            "SELECT "
            + "     e.extname, "
            + "     e.extowner, "
            + "     (SELECT n.nspname FROM pg_catalog.pg_namespace n WHERE e.extnamespace = n.oid) namespace,"
            + "     d.description "
            + "FROM "
            + "     pg_catalog.pg_extension e "
            + "     LEFT JOIN "
            + "         pg_catalog.pg_description d "
            + "     ON "
            + "         e.oid = d.objoid";
    
    String QUERY_SCHEMAS = 
            "SELECT "
            + "     n.oid::bigint, "
            + "     n.nspname, "
            + "     n.nspacl, "
            + "     r.rolname AS owner, "
            + "     d.description AS comment "
            + "FROM "
            + "     pg_catalog.pg_namespace n "
            + "     JOIN "
            + "         pg_catalog.pg_roles r "
            + "     ON "
            + "         n.nspowner = r.oid AND "
            + "         n.nspname NOT LIKE ('pg_%') AND "
            + "         n.nspname != 'information_schema'"
            + "     LEFT JOIN "
            + "         pg_catalog.pg_description d "
            + "     ON "
            + "         n.oid = d.objoid";
    
    String QUERY_TRIGGERS_PER_SCHEMA = 
            "SELECT "
            + "     ccc.relname, "
            + "     p.proname, "
            + "     nsp.nspname, "
            + "     tgname, "
            + "     tgfoid, "
            + "     tgtype, "
            + "     tgrelid::regclass::text "
            + "FROM "
            + "     pg_catalog.pg_class ccc "
            + "     RIGHT JOIN pg_catalog.pg_trigger t "
            + "     ON ccc.oid = t.tgrelid "
            + "     JOIN pg_catalog.pg_proc p "
            + "     ON p.oid = tgfoid "
            + "     JOIN pg_catalog.pg_namespace nsp "
            + "     ON p.pronamespace = nsp.oid "
            + "WHERE "
            + "     ccc.relkind = 'r' AND "
            + "     ccc.relnamespace = ? AND "
            + "     tgisinternal = FALSE";

    String QUERY_VIEWS_PER_SCHEMA = 
            "SELECT "
            + "     relname, "
            + "     relacl, "
            + "     relowner::bigint, "
            + "     pg_get_viewdef(c.oid) AS definition, "
            + "     d.description AS comment, "
            + "     array_agg(dep.deptype) AS deptype, "
            + "     subselect.column_names, "
            + "     subselect.column_comments, "
            + "     subselect.column_defaults "
            + "FROM "
            + "     pg_catalog.pg_class c "
            + "     LEFT JOIN "
            + "     (SELECT "
            + "         attrelid, "
            + "         array_agg(columnsData.attname) AS column_names, "
            + "         array_agg(columnsData.description) AS column_comments, "
            + "         array_agg(columnsData.adsrc) AS column_defaults "
            + "     FROM "
            + "         (SELECT "
            + "             attrelid, "
            + "             attr.attname, "
            + "             des.description, "
            + "             def.adsrc "
            + "         FROM "
            + "             pg_catalog.pg_attribute attr "
            + "             LEFT JOIN pg_catalog.pg_attrdef def "
            + "             ON def.adnum = attr.attnum "
            + "             AND attr.attrelid = def.adrelid "
            + "             AND attr.attisdropped IS FALSE "
            + "             LEFT JOIN pg_catalog.pg_description des "
            + "             ON des.objoid = attr.attrelid AND "
            + "             des.objsubid = attr.attnum "
            + "         ORDER BY "
            + "             attr.attnum "
            + "         ) columnsData "
            + "     GROUP BY "
            + "         attrelid) subselect "
            + "     ON subselect.attrelid = c.oid "
            + "     LEFT JOIN "
            + "         pg_catalog.pg_description d "
            + "     ON "
            + "         c.oid = d.objoid AND d.objsubid = 0 "
            + "     LEFT JOIN "
            + "         pg_catalog.pg_depend dep"
            + "     ON "
            + "         dep.objid = c.oid "
            + "WHERE "
            + "     relkind = 'v' AND "
            + "     relnamespace = ? "
            + "GROUP BY "
            + "     relname, "
            + "     relowner, "
            + "     definition, "
            + "     comment, "
            + "     relacl, "
            + "     subselect.column_names, "
            + "     subselect.column_comments, "
            + "     subselect.column_defaults";
    
    String QUERY_TOTAL_OBJECTS_COUNT = 
            "SELECT "
            + "     COUNT(c.oid)::integer "
            + "FROM "
            + "     pg_catalog.pg_class c "
            + "WHERE "
            + "     c.relnamespace IN (SELECT "
            + "                             nsp.oid "
            + "                        FROM "
            + "                             pg_catalog.pg_namespace nsp "
            + "                        WHERE "
            + "                             nsp.nspname NOT LIKE ('pg_%') "
            + "                             AND nsp.nspname != 'information_schema') "
            + "     AND c.relkind IN ('r', 'i', 'S', 'v')";
// SONAR-ON
}
