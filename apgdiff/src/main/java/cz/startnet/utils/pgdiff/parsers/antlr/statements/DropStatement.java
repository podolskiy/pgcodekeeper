package cz.startnet.utils.pgdiff.parsers.antlr.statements;

import java.util.Arrays;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;

import cz.startnet.utils.pgdiff.DangerStatement;
import cz.startnet.utils.pgdiff.parsers.antlr.QNameParser;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Drop_function_statementContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Drop_operator_statementContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Drop_rule_statementContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Drop_statementsContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Drop_trigger_statementContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.IdentifierContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Operator_nameContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Schema_dropContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Schema_qualified_nameContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Target_operatorContext;
import cz.startnet.utils.pgdiff.schema.GenericColumn;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgObjLocation;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.apgdiff.utils.Pair;

public class DropStatement extends ParserAbstract {

    private final Schema_dropContext ctx;

    public DropStatement(Schema_dropContext ctx, PgDatabase db) {
        super(db);
        this.ctx = ctx;
    }

    @Override
    public void parseObject() {
        if (ctx.drop_function_statement() != null) {
            dropFunction(ctx.drop_function_statement());
        } else if (ctx.drop_trigger_statement() != null) {
            dropTrigger(ctx.drop_trigger_statement());
        } else if (ctx.drop_rule_statement() != null) {
            dropRule(ctx.drop_rule_statement());
        } else if (ctx.drop_statements() != null) {
            drop(ctx.drop_statements());
        } else if (ctx.drop_operator_statement() != null) {
            dropOperator(ctx.drop_operator_statement());
        }
    }

    public void dropFunction(Drop_function_statementContext ctx) {
        DbObjType type;
        if (ctx.PROCEDURE() != null) {
            type = DbObjType.PROCEDURE;
        } else if (ctx.FUNCTION() != null) {
            type = DbObjType.FUNCTION;
        } else {
            type = DbObjType.AGGREGATE;
        }
        addObjReference(ctx.name.identifier(), type, ACTION_DROP);
    }

    public void dropOperator(Drop_operator_statementContext ctx) {
        for (Target_operatorContext targetOperCtx : ctx.target_operator()) {
            Operator_nameContext nameCtx = targetOperCtx.operator_name();
            addObjReference(Arrays.asList(nameCtx.schema_name, nameCtx.operator),
                    DbObjType.OPERATOR, ACTION_DROP);
        }
    }

    public void dropTrigger(Drop_trigger_statementContext ctx) {
        dropChild(ctx.table_name.identifier(), ctx.name, DbObjType.TRIGGER);
    }

    public void dropRule(Drop_rule_statementContext ctx) {
        dropChild(ctx.schema_qualified_name().identifier(), ctx.name, DbObjType.RULE);
    }

    public void dropChild(List<IdentifierContext> tableIds, IdentifierContext nameCtx, DbObjType type) {
        tableIds.add(nameCtx);
        addObjReference(tableIds, type, ACTION_DROP);
    }

    public void drop(Drop_statementsContext ctx) {
        DbObjType type = getTypeOfDropStmt(ctx);

        if (type == null) {
            return;
        }

        for (Schema_qualified_nameContext objName :
            ctx.if_exist_names_restrict_cascade().names_references().schema_qualified_name()) {
            List<IdentifierContext> ids = objName.identifier();
            PgObjLocation loc = addObjReference(ids, type, ACTION_DROP);

            if (type == DbObjType.TABLE) {
                loc.setWarning(DangerStatement.DROP_TABLE);
            }
        }
    }

    private DbObjType getTypeOfDropStmt(Drop_statementsContext ctx) {
        if (ctx.DATABASE()!= null) {
            return DbObjType.DATABASE;
        } else if (ctx.TABLE() != null) {
            return DbObjType.TABLE;
        } else if (ctx.EXTENSION() != null) {
            return DbObjType.EXTENSION;
        } else if (ctx.SCHEMA() != null) {
            return DbObjType.SCHEMA;
        } else if (ctx.SEQUENCE() != null) {
            return DbObjType.SEQUENCE;
        } else if (ctx.VIEW() != null) {
            return DbObjType.VIEW;
        } else if (ctx.INDEX() != null) {
            return DbObjType.INDEX;
        } else if (ctx.DOMAIN() != null) {
            return DbObjType.DOMAIN;
        } else if (ctx.TYPE() != null) {
            return DbObjType.TYPE;
        } else if (ctx.DICTIONARY() != null) {
            return DbObjType.FTS_DICTIONARY;
        } else if (ctx.TEMPLATE() != null) {
            return DbObjType.FTS_TEMPLATE;
        } else if (ctx.PARSER() != null) {
            return DbObjType.FTS_PARSER;
        } else if (ctx.CONFIGURATION() != null) {
            return DbObjType.FTS_CONFIGURATION;
        }
        return null;
    }

    @Override
    protected PgObjLocation fillQueryLocation(ParserRuleContext ctx) {
        PgObjLocation loc = super.fillQueryLocation(ctx);
        Drop_statementsContext dropSt = ((Schema_dropContext) ctx).drop_statements();
        if (dropSt != null && dropSt.TABLE() != null) {
            loc.setWarning(DangerStatement.DROP_TABLE);
        }
        return loc;
    }

    @Override
    protected Pair<String, GenericColumn> getActionAndObjForStmtAction() {
        DbObjType type;
        List<IdentifierContext> ids;
        GenericColumn descrObj = null;
        if (ctx.drop_function_statement() != null) {
            Drop_function_statementContext dropFuncCtx = ctx.drop_function_statement();
            ids = dropFuncCtx.name.identifier();
            if (dropFuncCtx.PROCEDURE() != null) {
                type = DbObjType.PROCEDURE;
            } else if (dropFuncCtx.FUNCTION() != null) {
                type = DbObjType.FUNCTION;
            } else {
                type = DbObjType.AGGREGATE;
            }
            descrObj = new GenericColumn(QNameParser.getSchemaName(ids),
                    QNameParser.getFirstNameCtx(ids).getText(), type);
        } else if (ctx.drop_trigger_statement() != null) {
            Drop_trigger_statementContext dropTrigCtx = ctx.drop_trigger_statement();
            ids = dropTrigCtx.table_name.identifier();
            descrObj = new GenericColumn(QNameParser.getSchemaName(ids),
                    QNameParser.getFirstNameCtx(ids).getText(),
                    dropTrigCtx.name.getText(), DbObjType.TRIGGER);
        } else if (ctx.drop_rule_statement() != null) {
            Drop_rule_statementContext dropRuleCtx = ctx.drop_rule_statement();
            ids = dropRuleCtx.schema_qualified_name().identifier();
            descrObj = new GenericColumn(QNameParser.getSchemaName(ids),
                    QNameParser.getFirstNameCtx(ids).getText(),
                    dropRuleCtx.name.getText(), DbObjType.RULE);
        } else if (ctx.drop_statements() != null) {
            Drop_statementsContext dropStmtCtx = ctx.drop_statements();
            type = getTypeOfDropStmt(dropStmtCtx);
            if (type != null) {
                List<Schema_qualified_nameContext> objNames = dropStmtCtx
                        .if_exist_names_restrict_cascade().names_references().schema_qualified_name();
                ids = objNames.get(0).identifier();
                String schemaName = "";
                String objName = "";
                if (objNames.size() == 1) {
                    schemaName = QNameParser.getSchemaName(ids);
                    objName = QNameParser.getFirstNameCtx(ids).getText();
                }
                descrObj = new GenericColumn(schemaName, objName, type);
            }
        } else if (ctx.drop_operator_statement() != null) {
            Drop_operator_statementContext dropRuleCtx = ctx.drop_operator_statement();
            List<Target_operatorContext> targetOpers = dropRuleCtx.target_operator();
            Operator_nameContext nameCtx = targetOpers.get(0).operator_name();
            String schemaName = "";
            String objName = "";
            if (targetOpers.size() == 1) {
                schemaName = nameCtx.schema_name.getText();
                objName = nameCtx.operator.getText();
            }
            descrObj = new GenericColumn(schemaName, objName, DbObjType.OPERATOR);
        }

        return descrObj != null ? new Pair<>(ACTION_DROP, descrObj) : null;
    }
}
