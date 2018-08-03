package cz.startnet.utils.pgdiff.loader.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import cz.startnet.utils.pgdiff.MsDiffUtils;
import cz.startnet.utils.pgdiff.loader.SupportedVersion;
import cz.startnet.utils.pgdiff.schema.GenericColumn;
import cz.startnet.utils.pgdiff.schema.MsConstraint;
import cz.startnet.utils.pgdiff.schema.PgSchema;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;

public class MsFKReader extends JdbcMsReader {

    public static class MsFKReaderFactory extends JdbcReaderFactory {

        public MsFKReaderFactory(Map<SupportedVersion, String> queries) {
            super(0, "", queries);
        }

        @Override
        public JdbcReader getReader(JdbcLoaderBase loader) {
            return new MsFKReader(this, loader);
        }
    }

    public MsFKReader(JdbcReaderFactory factory, JdbcLoaderBase loader) {
        super(factory, loader);
    }

    @Override
    protected void processResult(ResultSet res, PgSchema schema) throws SQLException, JsonReaderException {
        loader.monitor.worked(1);
        String name = res.getString("name");
        boolean isSystemNamed = res.getBoolean("is_system_named");
        loader.setCurrentObject(new GenericColumn(schema.getName(), name, DbObjType.CONSTRAINT));

        MsConstraint con = new MsConstraint(isSystemNamed ? "" : name, "");
        // TODO with no check

        StringBuilder sb = new StringBuilder();
        sb.append("FOREIGN KEY (");
        sb.append(MsDiffUtils.quoteName(res.getString("field_name")));
        sb.append(") REFERENCES ");
        sb.append(MsDiffUtils.quoteName(res.getString("referenced_schema_name")));
        sb.append('.');
        sb.append(MsDiffUtils.quoteName(res.getString("referenced_table_name")));
        sb.append(" (");
        sb.append(MsDiffUtils.quoteName(res.getString("referenced_field_name")));
        sb.append(")");

        int del = res.getInt("delete_referential_action");
        if (del > 0) {
            sb.append(" ON DELETE ");
            if (del == 1) {
                sb.append("CASCADE");
            } else {
                sb.append("SET ").append(del == 2 ? "NULL" : "DEFAULT");
            }
        }

        int upd = res.getInt("update_referential_action");
        if (upd > 0) {
            sb.append(" ON UPDATE ");
            if (upd == 1) {
                sb.append("CASCADE");
            } else {
                sb.append("SET ").append(upd == 2 ? "NULL" : "DEFAULT");
            }
        }

        if (res.getBoolean("is_not_for_replication")) {
            sb.append(" NOT FOR REPLICATION");
        }

        // TODO disabled

        con.setDefinition(sb.toString());

        schema.getTable(res.getString("table_name")).addConstraint(con);
    }

    @Override
    protected DbObjType getType() {
        return DbObjType.CONSTRAINT;
    }
}
