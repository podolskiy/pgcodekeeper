package cz.startnet.utils.pgdiff.parsers.antlr.statements;

import java.util.Arrays;

import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Create_extension_statementContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.IdentifierContext;
import cz.startnet.utils.pgdiff.schema.GenericColumn;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgExtension;
import cz.startnet.utils.pgdiff.schema.StatementActions;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.apgdiff.utils.Pair;

public class CreateExtension extends ParserAbstract {

    private final Create_extension_statementContext ctx;

    public CreateExtension(Create_extension_statementContext ctx, PgDatabase db) {
        super(db);
        this.ctx = ctx;
    }

    @Override
    public void parseObject() {
        IdentifierContext nameCtx = ctx.name;
        PgExtension ext = new PgExtension(nameCtx.getText());
        if (ctx.schema_with_name() != null) {
            IdentifierContext id = ctx.schema_with_name().name;
            ext.setSchema(id.getText());
            addDepSafe(ext, Arrays.asList(id), DbObjType.SCHEMA, true);
        }

        addSafe(db, ext, Arrays.asList(nameCtx));
    }

    @Override
    protected Pair<StatementActions, GenericColumn> getActionAndObjForStmtAction() {
        return new Pair<>(StatementActions.CREATE,
                new GenericColumn(ctx.name.getText(), DbObjType.EXTENSION));
    }
}