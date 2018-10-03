package cz.startnet.utils.pgdiff.parsers.antlr.statements.mssql;

import java.util.List;

import cz.startnet.utils.pgdiff.MsDiffUtils;
import cz.startnet.utils.pgdiff.parsers.antlr.QNameParser;
import cz.startnet.utils.pgdiff.parsers.antlr.TSQLParser.Create_or_alter_viewContext;
import cz.startnet.utils.pgdiff.parsers.antlr.TSQLParser.IdContext;
import cz.startnet.utils.pgdiff.parsers.antlr.TSQLParser.Select_statementContext;
import cz.startnet.utils.pgdiff.parsers.antlr.TSQLParser.View_attributeContext;
import cz.startnet.utils.pgdiff.parsers.antlr.statements.ParserAbstract;
import cz.startnet.utils.pgdiff.schema.AbstractSchema;
import cz.startnet.utils.pgdiff.schema.AbstractView;
import cz.startnet.utils.pgdiff.schema.MsView;
import cz.startnet.utils.pgdiff.schema.PgDatabase;

public class CreateMsView extends ParserAbstract {

    private final Create_or_alter_viewContext ctx;

    private final boolean ansiNulls;
    private final boolean quotedIdentifier;

    public CreateMsView(Create_or_alter_viewContext ctx, PgDatabase db, boolean ansiNulls, boolean quotedIdentifier) {
        super(db);
        this.ctx = ctx;
        this.ansiNulls = ansiNulls;
        this.quotedIdentifier = quotedIdentifier;
    }

    @Override
    public MsView getObject() {
        List<IdContext> ids = ctx.simple_name().id();
        AbstractSchema schema = getSchemaSafe(ids, db.getDefaultSchema());
        return getObject(schema);
    }

    public MsView getObject(AbstractSchema schema) {
        IdContext name = QNameParser.getFirstNameCtx(ctx.simple_name().id());
        MsView view = new MsView(name.getText(), getFullCtxText(ctx.getParent().getParent()));
        view.setAnsiNulls(ansiNulls);
        view.setQuotedIdentified(quotedIdentifier);

        Select_statementContext vQuery = ctx.select_statement();
        if (vQuery != null) {
            view.setQuery(getFullCtxText(vQuery));
        }

        if (ctx.column_name_list() != null) {
            for (IdContext column : ctx.column_name_list().id()) {
                view.addColumnName(column.getText());
            }
        }

        List<View_attributeContext> options = ctx.view_attribute();
        if (options != null){
            for (View_attributeContext option: options) {
                view.addOption(MsDiffUtils.getQuotedName(option.getText()), "");
            }
        }

        if (ctx.with_check_option() != null){
            view.addOption(AbstractView.CHECK_OPTION, "");
        }

        schema.addView(view);
        return view;
    }
}
