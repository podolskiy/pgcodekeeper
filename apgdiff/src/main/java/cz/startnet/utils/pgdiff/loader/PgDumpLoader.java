/**
 * Copyright 2006 StartNet s.r.o.
 *
 * Distributed under MIT license
 */
package cz.startnet.utils.pgdiff.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import cz.startnet.utils.pgdiff.PgDiffArguments;
import cz.startnet.utils.pgdiff.PgDiffUtils;
import cz.startnet.utils.pgdiff.parsers.antlr.AntlrError;
import cz.startnet.utils.pgdiff.parsers.antlr.AntlrParser;
import cz.startnet.utils.pgdiff.parsers.antlr.CustomSQLParserListener;
import cz.startnet.utils.pgdiff.parsers.antlr.CustomTSQLParserListener;
import cz.startnet.utils.pgdiff.parsers.antlr.ReferenceListener;
import cz.startnet.utils.pgdiff.parsers.antlr.StatementBodyContainer;
import cz.startnet.utils.pgdiff.schema.AbstractSchema;
import cz.startnet.utils.pgdiff.schema.MsSchema;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgSchema;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts.WORK_DIR_NAMES;

/**
 * Loads PostgreSQL dump into classes.
 *
 * @author fordfrog
 */
public class PgDumpLoader implements AutoCloseable {

    /**
     * Loading order and directory names of the objects in exported DB schemas.
     * NOTE: constraints, triggers and indexes are now stored in tables,
     * those directories are here for backward compatibility only
     */
    protected static final String[] DIR_LOAD_ORDER = new String[] { "TYPE", "DOMAIN",
            "SEQUENCE", "FUNCTION", "TABLE", "CONSTRAINT", "INDEX", "TRIGGER", "VIEW",
            "FTS_PARSER", "FTS_TEMPLATE", "FTS_DICTIONARY", "FTS_CONFIGURATION" };

    protected static final String[] MS_DIR_LOAD_ORDER = new String[] { "Assemblies",
            "Sequences", "Stored Procedures", "Functions", "Tables", "Views"};

    private final InputStream input;
    private final String inputObjectName;
    private final PgDiffArguments args;

    private final IProgressMonitor monitor;
    private final int monitoringLevel;

    private final List<AntlrError> errors = new ArrayList<>();

    private boolean loadSchema = true;
    private boolean loadReferences;
    private List<StatementBodyContainer> statementBodyReferences;

    public List<AntlrError> getErrors() {
        return errors;
    }

    public void setLoadSchema(boolean loadSchema) {
        this.loadSchema = loadSchema;
    }

    public void setLoadReferences(boolean loadReferences) {
        this.loadReferences = loadReferences;
    }

    public List<StatementBodyContainer> getStatementBodyReferences() {
        return statementBodyReferences;
    }

    public PgDumpLoader(InputStream input, String inputObjectName,
            PgDiffArguments args, IProgressMonitor monitor, int monitoringLevel) {
        this.input = input;
        this.inputObjectName = inputObjectName;
        this.args = args;
        this.monitor = monitor;
        this.monitoringLevel = monitoringLevel;
    }

    /**
     * This constructor sets the monitoring level to the default of 1.
     */
    public PgDumpLoader(InputStream input, String inputObjectName,
            PgDiffArguments args, IProgressMonitor monitor) {
        this(input, inputObjectName, args, monitor, 1);
    }

    /**
     * This constructor uses {@link NullProgressMonitor}.
     */
    public PgDumpLoader(InputStream input, String inputObjectName,
            PgDiffArguments args) {
        this(input, inputObjectName, args, new NullProgressMonitor(), 0);
    }

    /**
     * This constructor creates {@link InputStream} using inputFile parameter.
     * Call {@link #close()} after this {@link PgDumpLoader} instance is no longer needed,
     * or wrap usage of the instance with try-with-resources.
     */
    public PgDumpLoader(File inputFile, PgDiffArguments args,
            IProgressMonitor monitor, int monitoringLevel) throws IOException {
        this(new FileInputStream(inputFile), inputFile.toString(), args,
                monitor, monitoringLevel);
    }

    /**
     * @see #PgDumpLoader(File, PgDiffArguments, IProgressMonitor, int)
     * @see #PgDumpLoader(InputStream, String, PgDiffArguments, IProgressMonitor)
     */
    public PgDumpLoader(File inputFile, PgDiffArguments args,
            IProgressMonitor monitor) throws IOException {
        this(inputFile, args, monitor, 1);
    }

    /**
     * @see #PgDumpLoader(File, PgDiffArguments, IProgressMonitor, int)
     * @see #PgDumpLoader(InputStream, String, PgDiffArguments)
     */
    public PgDumpLoader(File inputFile, PgDiffArguments args) throws IOException {
        this(inputFile, args, new NullProgressMonitor(), 0);
    }

    /**
     * The same as {@link #load(boolean)} with <code>false<code> argument.
     */
    public PgDatabase load() throws IOException, InterruptedException {
        PgDatabase d = new PgDatabase();

        AbstractSchema schema = args.isMsSql() ? new MsSchema(ApgdiffConsts.DBO, "") :
            new PgSchema(ApgdiffConsts.PUBLIC, "");
        d.addSchema(schema);
        d.setDefaultSchema(schema.getName());
        d.setArguments(args);
        load(d);
        d.getSchema(schema.getName()).setLocation(inputObjectName);
        FullAnalyze.fullAnalyze(d);
        return d;
    }

    protected PgDatabase load(PgDatabase intoDb) throws IOException, InterruptedException {
        PgDiffUtils.checkCancelled(monitor);
        List<ParseTreeListener> listeners = new ArrayList<>();

        if (args.isMsSql()) {
            if (loadSchema) {
                listeners.add(new CustomTSQLParserListener(intoDb, inputObjectName, errors, monitor));
            }

            /*
            if (loadReferences) {
                ReferenceListener refListener = new ReferenceListener(intoDb, inputObjectName, monitor);
                statementBodyReferences = refListener.getStatementBodies();
                listeners.add(refListener);
            } */

            AntlrParser.parseTSqlStream(input, args.getInCharsetName(), inputObjectName, errors,
                    monitor, monitoringLevel, listeners);
        } else {
            if (loadSchema) {
                listeners.add(new CustomSQLParserListener(intoDb, inputObjectName, errors, monitor));
            }
            if (loadReferences) {
                ReferenceListener refListener = new ReferenceListener(intoDb, inputObjectName, monitor);
                statementBodyReferences = refListener.getStatementBodies();
                listeners.add(refListener);
            }
            AntlrParser.parseSqlStream(input, args.getInCharsetName(), inputObjectName, errors,
                    monitor, monitoringLevel, listeners);
        }

        return intoDb;
    }

    @Override
    public void close() throws IOException {
        input.close();
    }

    /**
     * Loads database schema from a ModelExporter directory tree.
     *
     * @param dirPath path to the directory tree root
     *
     * @return database schema
     * @throws InterruptedException
     */
    public static PgDatabase loadDatabaseSchemaFromDirTree(String dirPath,
            PgDiffArguments arguments, IProgressMonitor monitor, List<AntlrError> errors)
                    throws InterruptedException, IOException {
        PgDatabase db = new PgDatabase();
        db.setArguments(arguments);
        File dir = new File(dirPath);

        // step 1
        // read files in schema folder, add schemas to db
        for (WORK_DIR_NAMES dirEnum : WORK_DIR_NAMES.values()) {
            // legacy schemas
            loadSubdir(dir, arguments, dirEnum.name(), db, monitor, errors);
        }

        File schemasCommonDir = new File(dir, WORK_DIR_NAMES.SCHEMA.name());
        // skip walking SCHEMA folder if it does not exist
        if (!schemasCommonDir.isDirectory()) {
            return db;
        }

        // new schemas + content
        // step 2
        // read out schemas names, and work in loop on each
        try (Stream<Path> schemas = Files.list(schemasCommonDir.toPath())) {
            for (Path schemaDir : PgDiffUtils.sIter(schemas)) {
                if (Files.isDirectory(schemaDir)) {
                    loadSubdir(schemasCommonDir, arguments, schemaDir.getFileName().toString(),
                            db, monitor, errors);
                    for (String dirSub : DIR_LOAD_ORDER) {
                        loadSubdir(schemaDir.toFile(), arguments, dirSub, db, monitor, errors);
                    }
                }
            }
        }

        FullAnalyze.fullAnalyze(db);
        return db;
    }

    /**
     * Loads database schema from a MS SQL ModelExporter directory tree.
     */
    public static PgDatabase loadMsDatabaseSchemaFromDirTree(String dirPath,
            PgDiffArguments arguments, IProgressMonitor monitor, List<AntlrError> errors)
                    throws InterruptedException, IOException {
        PgDatabase db = new PgDatabase();
        db.addSchema(new MsSchema(ApgdiffConsts.DBO, ""));
        db.setDefaultSchema(ApgdiffConsts.DBO);
        db.setArguments(arguments);
        File dir = new File(dirPath);

        File securityFolder = new File(dir, "Security");
        loadSubdir(securityFolder, arguments, "Roles", db, monitor, errors);
        loadSubdir(securityFolder, arguments, "Users", db, monitor, errors);
        loadSubdir(securityFolder, arguments, "Schemas", db, monitor, errors);

        for (String dirSub : MS_DIR_LOAD_ORDER) {
            loadSubdir(dir, arguments, dirSub, db, monitor, errors);
        }

        return db;
    }

    public static void loadLibraries(PgDatabase db, PgDiffArguments arguments,
            boolean isIgnorePriv, Collection<String> paths) throws InterruptedException, IOException, URISyntaxException {
        for (String path : paths) {
            loadLibrary(db, arguments, isIgnorePriv, path);
        }
    }

    protected static void loadLibrary(PgDatabase db, PgDiffArguments arguments,
            boolean isIgnorePriv, String path) throws InterruptedException, IOException, URISyntaxException {
        db.addLib(getLibrary(path, arguments, isIgnorePriv));
    }

    private static PgDatabase getLibrary(String path, PgDiffArguments arguments,
            boolean isIgnorePriv) throws InterruptedException, IOException, URISyntaxException {

        PgDiffArguments args = arguments.clone();
        args.setIgnorePrivileges(isIgnorePriv);

        if (path.startsWith("jdbc:")) {
            String timezone = args.getTimeZone() == null ? ApgdiffConsts.UTC : args.getTimeZone();
            PgDatabase db = new JdbcLoader(JdbcConnector.fromUrl(path, timezone), args).getDbFromJdbc();
            db.getDescendants().forEach(st -> st.setLocation(path));
            return db;
        }

        Path p = Paths.get(path);

        if (Files.isDirectory(p)) {
            if (Files.exists(p.resolve(ApgdiffConsts.FILENAME_WORKING_DIR_MARKER))) {
                return PgDumpLoader.loadDatabaseSchemaFromDirTree(path,  args, null, null);
            } else {
                PgDatabase db = new PgDatabase();
                db.setArguments(args);
                readStatementsFromDirectory(p, db, args);
                return db;
            }
        }

        try (PgDumpLoader loader = new PgDumpLoader(new File(path), args)) {
            return loader.load();
        }
    }

    private static void readStatementsFromDirectory(final Path f, PgDatabase db, PgDiffArguments args)
            throws IOException, InterruptedException, URISyntaxException {
        if (Files.isDirectory(f)) {
            try (Stream<Path> stream = Files.list(f)) {
                for (Path sub : (Iterable<Path>) stream::iterator) {
                    readStatementsFromDirectory(sub, db, args);
                }
            }
        } else {
            try (PgDumpLoader loader = new PgDumpLoader(f.toFile(), args)) {
                db.addLib(loader.load());
            }
        }
    }

    private static void loadSubdir(File dir, PgDiffArguments arguments, String sub, PgDatabase db,
            IProgressMonitor monitor, List<AntlrError> errors)
                    throws InterruptedException, IOException {
        File subDir = new File(dir, sub);
        if (subDir.exists() && subDir.isDirectory()) {
            File[] files = subDir.listFiles();
            loadFiles(files, arguments, db, monitor, errors);
        }
    }

    private static void loadFiles(File[] files, PgDiffArguments arguments,
            PgDatabase db, IProgressMonitor monitor, List<AntlrError> errors)
                    throws IOException, InterruptedException {
        Arrays.sort(files);
        for (File f : files) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".sql")) {
                List<AntlrError> errList = null;
                try (PgDumpLoader loader = new PgDumpLoader(f, arguments, monitor)) {
                    errList = loader.getErrors();
                    loader.load(db);
                } finally {
                    if (errors != null && errList != null && !errList.isEmpty()) {
                        errors.addAll(errList);
                    }
                }
            }
        }
    }
}