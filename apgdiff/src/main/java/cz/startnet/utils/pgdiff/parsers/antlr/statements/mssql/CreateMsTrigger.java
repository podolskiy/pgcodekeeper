package cz.startnet.utils.pgdiff.parsers.antlr.statements.mssql;

import java.util.Arrays;
import java.util.List;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

import cz.startnet.utils.pgdiff.parsers.antlr.TSQLParser.Batch_statementContext;
import cz.startnet.utils.pgdiff.parsers.antlr.TSQLParser.Create_or_alter_triggerContext;
import cz.startnet.utils.pgdiff.parsers.antlr.TSQLParser.IdContext;
import cz.startnet.utils.pgdiff.parsers.antlr.msexpr.MsSqlClauses;
import cz.startnet.utils.pgdiff.schema.AbstractSchema;
import cz.startnet.utils.pgdiff.schema.MsTrigger;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgTriggerContainer;
import cz.startnet.utils.pgdiff.schema.StatementActions;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;

public class CreateMsTrigger extends BatchContextProcessor {

    private final Create_or_alter_triggerContext ctx;

    private final boolean ansiNulls;
    private final boolean quotedIdentifier;

    public CreateMsTrigger(Batch_statementContext ctx, PgDatabase db,
            boolean ansiNulls, boolean quotedIdentifier, CommonTokenStream stream) {
        super(db, ctx, stream);
        this.ctx = ctx.batch_statement_body().create_or_alter_trigger();
        this.ansiNulls = ansiNulls;
        this.quotedIdentifier = quotedIdentifier;
    }

    @Override
    protected ParserRuleContext getDelimiterCtx() {
        return ctx.table_name;
    }

    @Override
    public void parseObject() {
        IdContext schemaCtx = ctx.trigger_name.schema;
        if (schemaCtx == null) {
            schemaCtx = ctx.table_name.schema;
        }
        List<IdContext> ids = Arrays.asList(schemaCtx, ctx.table_name.name);
        addFullObjReference(ids, DbObjType.TABLE, StatementActions.NONE);
        getObject(getSchemaSafe(ids));
    }

    public MsTrigger getObject(AbstractSchema schema) {
        IdContext schemaCtx = ctx.trigger_name.schema;
        if (schemaCtx == null) {
            schemaCtx = ctx.table_name.schema;
        }
        IdContext tableNameCtx = ctx.table_name.name;
        IdContext nameCtx = ctx.trigger_name.name;

        MsTrigger trigger = new MsTrigger(nameCtx.getText(), tableNameCtx.getText());
        trigger.setAnsiNulls(ansiNulls);
        trigger.setQuotedIdentified(quotedIdentifier);
        setSourceParts(trigger);

        String schemaName;
        if (schema != null) {
            schemaName = schema.getName();
        } else {
            schemaName = getSchemaNameSafe(Arrays.asList(schemaCtx, tableNameCtx));
        }

        MsSqlClauses clauses = new MsSqlClauses(schemaName);
        clauses.analyze(ctx.sql_clauses());
        trigger.addAllDeps(clauses.getDepcies());

        PgTriggerContainer cont = getSafe(AbstractSchema::getTriggerContainer,
                schema, tableNameCtx);

        addSafe(PgTriggerContainer::addTrigger, cont, trigger, schemaCtx,
                tableNameCtx, nameCtx);
        return trigger;
    }
}
