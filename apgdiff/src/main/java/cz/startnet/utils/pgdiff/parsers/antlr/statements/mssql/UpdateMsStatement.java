package cz.startnet.utils.pgdiff.parsers.antlr.statements.mssql;

import java.util.Arrays;

import org.antlr.v4.runtime.ParserRuleContext;

import cz.startnet.utils.pgdiff.DangerStatement;
import cz.startnet.utils.pgdiff.parsers.antlr.TSQLParser.Qualified_nameContext;
import cz.startnet.utils.pgdiff.parsers.antlr.TSQLParser.Update_statementContext;
import cz.startnet.utils.pgdiff.parsers.antlr.statements.ParserAbstract;
import cz.startnet.utils.pgdiff.schema.GenericColumn;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgObjLocation;
import cz.startnet.utils.pgdiff.schema.StatementActions;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.apgdiff.utils.Pair;

public class UpdateMsStatement extends ParserAbstract {

    private final Update_statementContext ctx;

    public UpdateMsStatement(Update_statementContext ctx, PgDatabase db) {
        super(db);
        this.ctx = ctx;
    }

    @Override
    public void parseObject() {
        Qualified_nameContext qname = ctx.qualified_name();
        if (qname != null) {
            PgObjLocation loc = addObjReference(Arrays.asList(qname.schema, qname.name),
                    DbObjType.TABLE, StatementActions.UPDATE);
            loc.setWarning(DangerStatement.UPDATE);
        }
    }

    @Override
    protected PgObjLocation fillQueryLocation(ParserRuleContext ctx) {
        PgObjLocation loc = super.fillQueryLocation(ctx);
        loc.setWarning(DangerStatement.UPDATE);
        return loc;
    }

    @Override
    protected Pair<StatementActions, GenericColumn> getActionAndObjForStmtAction() {
        Qualified_nameContext qname = ctx.qualified_name();
        return new Pair<>(StatementActions.UPDATE, new GenericColumn(
                qname.schema.getText(), qname.name.getText(), DbObjType.TABLE));
    }
}
