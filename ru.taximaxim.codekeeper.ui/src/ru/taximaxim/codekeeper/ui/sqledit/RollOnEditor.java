package ru.taximaxim.codekeeper.ui.sqledit;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.services.IEvaluationService;

import cz.startnet.utils.pgdiff.DangerStatement;
import cz.startnet.utils.pgdiff.loader.JdbcConnector;
import cz.startnet.utils.pgdiff.loader.JdbcRunner;
import cz.startnet.utils.pgdiff.schema.PgObjLocation;
import cz.startnet.utils.pgdiff.schema.StatementActions;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts.JDBC_CONSTS;
import ru.taximaxim.codekeeper.apgdiff.fileutils.TempFile;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.IPartAdapter2;
import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.UIConsts.CONTEXT;
import ru.taximaxim.codekeeper.ui.UIConsts.DB_UPDATE_PREF;
import ru.taximaxim.codekeeper.ui.UIConsts.FILE;
import ru.taximaxim.codekeeper.ui.UIConsts.MARKER;
import ru.taximaxim.codekeeper.ui.UIConsts.PROJ_PATH;
import ru.taximaxim.codekeeper.ui.UIConsts.PROJ_PREF;
import ru.taximaxim.codekeeper.ui.UIConsts.PROP_TEST;
import ru.taximaxim.codekeeper.ui.UIConsts.XML_TAGS;
import ru.taximaxim.codekeeper.ui.UiSync;
import ru.taximaxim.codekeeper.ui.XmlHistory;
import ru.taximaxim.codekeeper.ui.consoles.ConsoleFactory;
import ru.taximaxim.codekeeper.ui.dbstore.DbInfo;
import ru.taximaxim.codekeeper.ui.dialogs.ExceptionNotifier;
import ru.taximaxim.codekeeper.ui.editors.ProjectEditorDiffer;
import ru.taximaxim.codekeeper.ui.externalcalls.utils.StdStreamRedirector;
import ru.taximaxim.codekeeper.ui.localizations.Messages;
import ru.taximaxim.codekeeper.ui.pgdbproject.PgDbProject;

public class RollOnEditor extends SQLEditor {

    private static final String SCRIPT_PLACEHOLDER = "%script"; //$NON-NLS-1$
    private static final String DB_HOST_PLACEHOLDER = "%host"; //$NON-NLS-1$
    private static final String DB_PORT_PLACEHOLDER = "%port"; //$NON-NLS-1$
    private static final String DB_NAME_PLACEHOLDER = "%db"; //$NON-NLS-1$
    private static final String DB_USER_PLACEHOLDER = "%user"; //$NON-NLS-1$
    private static final String DB_PASS_PLACEHOLDER = "%pass"; //$NON-NLS-1$

    private final XmlHistory history;

    private final IPreferenceStore mainPrefs = Activator.getDefault().getPreferenceStore();
    private Composite parentComposite;
    private RollOnEditorPartListener partListener;

    private Text txtCommand;
    private Combo cmbScript;

    private volatile boolean isRunning;
    private Thread scriptThread;

    private DbInfo lastDB;

    private volatile boolean updateDdlJobInProcessing;

    private final Listener parserListener = e -> {
        if (parentComposite == null) {
            return;
        }
        UiSync.exec(parentComposite, () -> {
            if (!getSourceViewer().getTextWidget().isDisposed()) {
                setLineBackground();
            }
        });
    };

    public RollOnEditor() {
        this.history = new XmlHistory.Builder(XML_TAGS.DDL_UPDATE_COMMANDS_MAX_STORED,
                FILE.DDL_UPDATE_COMMANDS_HIST_FILENAME,
                XML_TAGS.DDL_UPDATE_COMMANDS_HIST_ROOT,
                XML_TAGS.DDL_UPDATE_COMMANDS_HIST_ELEMENT).build();
    }

    public boolean isUpdateDdlJobInProcessing() {
        return updateDdlJobInProcessing;
    }

    public void setUpdateDdlJobInProcessing(boolean updateDdlJobInProcessing) {
        this.updateDdlJobInProcessing = updateDdlJobInProcessing;
        getSite().getService(IEvaluationService.class).requestEvaluation(PROP_TEST.UPDATE_DDL_RUNNING);
    }

    public void setLastDb(DbInfo lastDb) {
        this.lastDB = lastDb;
    }

    public DbInfo getLastDb() {
        if (lastDB != null) {
            return lastDB;
        }

        IEditorInput editorInput = getEditorSite().getPage().getActiveEditor().getEditorInput();
        PgDbProject proj = null;
        if(editorInput instanceof IFileEditorInput) {
            proj = new PgDbProject(((IFileEditorInput)editorInput).getFile().getProject());
            List<DbInfo> lastStore = DbInfo.preferenceToStore(proj.getPrefs().get(PROJ_PREF.LAST_DB_STORE, "")); //$NON-NLS-1$
            return lastStore.isEmpty() ? null : lastStore.get(0);
        } else {
            return null;
        }
    }

    @Override
    protected ISourceViewer createSourceViewer(Composite parent,
            IVerticalRuler ruler, int styles) {
        Layout gl = new GridLayout();
        parent.setLayout(gl);
        parent.setLayoutData(new GridData());

        createDialogArea(parent);

        SourceViewer sw = (SourceViewer) super.createSourceViewer(parent, ruler, styles);
        sw.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        sw.appendVerifyKeyListener(new VerifyKeyListener() {

            @Override
            public void verifyKey(VerifyEvent event) {
                if ((event.stateMask & SWT.MOD1) != 0 && event.keyCode == SWT.F5) {
                    updateDdl();
                }
            }
        });

        return sw;
    }

    @Override
    public void createPartControl(Composite parent) {
        parentComposite = parent;
        super.createPartControl(parent);
        setLineBackground();
        getSite().getService(IContextService.class).activateContext(CONTEXT.EDITOR);
    }

    public void setLineBackground() {
        // TODO who deletes stale annotations after editor refresh?
        List<PgObjLocation> refs = getParser().getObjsForEditor(getEditorInput());
        IAnnotationModel model = getSourceViewer().getAnnotationModel();
        for (PgObjLocation loc : refs) {
            String annotationMsg = null;
            if (loc.getAction() == StatementActions.DROP && loc.getObjType() == DbObjType.TABLE){
                annotationMsg = "DROP TABLE statement"; //$NON-NLS-1$
            } else if (loc.getAction() == StatementActions.ALTER){
                String text = loc.getText();
                if (loc.getObjType() == DbObjType.TABLE) {
                    if (DangerStatement.ALTER_COLUMN.getRegex().matcher(text).matches()) {
                        annotationMsg = "ALTER COLUMN ... TYPE statement"; //$NON-NLS-1$
                    } else if (DangerStatement.DROP_COLUMN.getRegex().matcher(text).matches()) {
                        annotationMsg = "DROP COLUMN statement"; //$NON-NLS-1$
                    }
                } else if (loc.getObjType() == DbObjType.SEQUENCE &&
                        DangerStatement.RESTART_WITH.getRegex().matcher(text).matches()) {
                    annotationMsg = "ALTER SEQUENCE ... RESTART WITH statement"; //$NON-NLS-1$
                }
            }
            if (annotationMsg != null) {
                model.addAnnotation(new Annotation(MARKER.DANGER_ANNOTATION, false, annotationMsg),
                        new Position(loc.getOffset(), loc.getObjLength()));
            }
        }
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        super.init(site, input);
        partListener = new RollOnEditorPartListener(this);
        getSite().getPage().addPartListener(partListener);
        getParser().addListener(parserListener);
    }

    @Override
    public void dispose() {
        getSite().getPage().removePartListener(partListener);
        getParser().removeListener(parserListener);
        super.dispose();
    }

    protected Control createDialogArea(final Composite parent) {
        GridLayout lay = new GridLayout();
        parent.setLayout(lay);

        final Composite notJdbc = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        notJdbc.setLayoutData(gd);

        GridLayout gl = new GridLayout();
        gl.marginHeight = gl.marginWidth = 0;
        notJdbc.setLayout(gl);

        Label l = new Label(notJdbc, SWT.NONE);
        l.setText(MessageFormat.format(Messages.sqlScriptDialog_option_is_enabled,
                Messages.dbUpdatePrefPage_use_command_for_ddl_update,
                Messages.sqlScriptDialog_update_db));
        l.setForeground(getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
        l.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        gd = new GridData(GridData.FILL_HORIZONTAL);
        l.setLayoutData(gd);

        l = new Label(notJdbc, SWT.HORIZONTAL | SWT.SEPARATOR);
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        gd = new GridData(GridData.FILL_HORIZONTAL);
        l.setLayoutData(gd);

        l = new Label(notJdbc, SWT.NONE);
        l.setText(Messages.sqlScriptDialog_Enter_cmd_to_update_ddl_with_sql_script
                + SCRIPT_PLACEHOLDER + ' '
                + DB_NAME_PLACEHOLDER + ' '
                + DB_HOST_PLACEHOLDER + ' ' + DB_PORT_PLACEHOLDER + ' '
                + DB_USER_PLACEHOLDER + ' ' + DB_PASS_PLACEHOLDER + ')' + ':');
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalIndent = 12;
        l.setLayoutData(gd);

        cmbScript = new Combo(notJdbc, SWT.DROP_DOWN);
        cmbScript.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        cmbScript.setToolTipText(
                DB_NAME_PLACEHOLDER + '=' + getReplacedString(DB_NAME_PLACEHOLDER, lastDB) + UIConsts._NL +
                DB_HOST_PLACEHOLDER + '=' + getReplacedString(DB_HOST_PLACEHOLDER, lastDB) + UIConsts._NL +
                DB_PORT_PLACEHOLDER + '=' + getReplacedString(DB_PORT_PLACEHOLDER, lastDB) + UIConsts._NL +
                DB_USER_PLACEHOLDER + '=' + getReplacedString(DB_NAME_PLACEHOLDER, lastDB) + UIConsts._NL +
                DB_PASS_PLACEHOLDER + '=' + getReplacedString(DB_USER_PLACEHOLDER, lastDB));

        List<String> prev = null;
        try {
            prev = history.getHistory();
        } catch (IOException e1) {
            ExceptionNotifier.notifyDefault(Messages.SqlScriptDialog_error_loading_command_history, e1);
        }
        if (prev == null) {
            prev = new ArrayList<>();
        }
        if (prev.isEmpty()) {
            prev.add(UIConsts.DDL_DEFAULT_CMD);
        }
        for (String el : prev) {
            cmbScript.add(el);
        }
        cmbScript.select(0);

        cmbScript.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                txtCommand.setText(getReplacedString());
            }
        });

        l = new Label(notJdbc, SWT.NONE);
        l.setText(Messages.SqlScriptDialog_command_to_execute + SCRIPT_PLACEHOLDER
                + Messages.SqlScriptDialog_will_be_replaced);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalIndent = 12;
        l.setLayoutData(gd);

        txtCommand = new Text(notJdbc, SWT.BORDER | SWT.READ_ONLY);
        txtCommand.setText(getReplacedString());
        txtCommand.setBackground(parent.getShell().getDisplay()
                .getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        txtCommand.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        l = new Label(notJdbc, SWT.HORIZONTAL | SWT.SEPARATOR);
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalIndent = 12;
        l.setLayoutData(gd);

        mainPrefs.addPropertyChangeListener(new IPropertyChangeListener(){
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if(!notJdbc.isDisposed()
                        && DB_UPDATE_PREF.COMMAND_LINE_DDL_UPDATE.equals(event.getProperty())) {
                    boolean isCmd = mainPrefs.getBoolean(DB_UPDATE_PREF.COMMAND_LINE_DDL_UPDATE);
                    notJdbc.setVisible(isCmd);
                    ((GridData)notJdbc.getLayoutData()).exclude = !isCmd;

                    parent.layout();
                }
            }
        });

        mainPrefs.firePropertyChangeEvent(DB_UPDATE_PREF.COMMAND_LINE_DDL_UPDATE,
                mainPrefs.getBoolean(DB_UPDATE_PREF.COMMAND_LINE_DDL_UPDATE),
                mainPrefs.getBoolean(DB_UPDATE_PREF.COMMAND_LINE_DDL_UPDATE));

        return parent;
    }

    private String getReplacedString() {
        return getReplacedString(cmbScript.getText(), lastDB);
    }

    private static String getReplacedString(String dbInfo, DbInfo externalDbInfo) {
        String s = dbInfo;
        if (externalDbInfo != null) {
            if (externalDbInfo.getDbHost() != null) {
                s = s.replace(DB_HOST_PLACEHOLDER, externalDbInfo.getDbHost());
            }
            if (externalDbInfo.getDbName() != null) {
                s = s.replace(DB_NAME_PLACEHOLDER, externalDbInfo.getDbName());
            }
            if (externalDbInfo.getDbUser() != null) {
                s = s.replace(DB_USER_PLACEHOLDER, externalDbInfo.getDbUser());
            }
            if (externalDbInfo.getDbPass() != null) {
                s = s.replace(DB_PASS_PLACEHOLDER, externalDbInfo.getDbPass());
            }
            int port = externalDbInfo.getDbPort();
            if (port == 0) {
                port = JDBC_CONSTS.JDBC_DEFAULT_PORT;
            }
            s = s.replace(DB_PORT_PLACEHOLDER, "" + port); //$NON-NLS-1$
        }
        return s;
    }

    private void afterScriptFinished(final String scriptOutput) {
        UiSync.exec(parentComposite, new Runnable() {

            @Override
            public void run() {
                if (!isUpdateDdlJobInProcessing()) {
                    parentComposite.setCursor(null);

                    if (mainPrefs.getBoolean(DB_UPDATE_PREF.SHOW_SCRIPT_OUTPUT_SEPARATELY)) {
                        new ScriptRunResultDialog(parentComposite.getShell(), scriptOutput).open();
                    }
                }
                isRunning = false;
            }
        });
    }

    public String getEditorText() {
        return getSourceViewer().getTextWidget().getText();
    }

    public void updateDdl() {
        if (!isRunning) {
            final String textRetrieved;
            Point point = getSourceViewer().getSelectedRange();
            IDocument document = getSourceViewer().getDocument();
            if (point.y == 0){
                textRetrieved = document.get();
            } else {
                try {
                    textRetrieved = document.get(point.x, point.y);
                } catch (BadLocationException ble){
                    Log.log(Log.LOG_WARNING, ble.getMessage());
                    ExceptionNotifier.notifyDefault(Messages.RollOnEditor_selected_text_error, ble);
                    return;
                }
            }
            // new runnable to unlock the UI thread
            Runnable launcher;

            if (!mainPrefs.getBoolean(DB_UPDATE_PREF.COMMAND_LINE_DDL_UPDATE)){
                Log.log(Log.LOG_INFO, "Running DDL update using JDBC"); //$NON-NLS-1$

                DbInfo dbInfo = getLastDb();
                if (dbInfo == null){
                    ExceptionNotifier.notifyDefault(Messages.sqlScriptDialog_script_select_storage, null);
                    return;
                }

                final String jdbcHost = dbInfo.getDbHost();
                final int jdbcPort = dbInfo.getDbPort();
                final String jdbcUser = dbInfo.getDbUser();
                final String jdbcPass = dbInfo.getDbPass();
                final String jdbcDbName = dbInfo.getDbName();

                launcher = new Runnable() {

                    @Override
                    public void run() {
                        String output = Messages.sqlScriptDialog_script_has_not_been_run_yet;
                        try{
                            setUpdateDdlJobInProcessing(true);

                            JdbcConnector connector = new JdbcConnector(
                                    jdbcHost, jdbcPort, jdbcUser, jdbcPass, jdbcDbName,
                                    ApgdiffConsts.UTC);
                            output = new JdbcRunner(connector).runScript(textRetrieved);
                            if (JDBC_CONSTS.JDBC_SUCCESS.equals(output)) {
                                output = Messages.RollOnEditor_jdbc_success;
                                ProjectEditorDiffer.notifyDbChanged(dbInfo);
                            }
                        } catch (IOException e) {
                            throw new IllegalStateException(e.getLocalizedMessage(), e);
                        } finally {
                            setUpdateDdlJobInProcessing(false);

                            // request UI change: button label changed
                            afterScriptFinished(output);
                        }
                    }
                };
            } else {
                Log.log(Log.LOG_INFO, "Running DDL update using external command"); //$NON-NLS-1$
                try {
                    history.addHistoryEntry(cmbScript.getText());
                } catch (IOException e) {
                    ExceptionNotifier.notifyDefault(
                            Messages.SqlScriptDialog_error_adding_command_history, e);
                }
                final List<String> command = new ArrayList<>(Arrays.asList(
                        getReplacedString().split(" "))); //$NON-NLS-1$

                launcher = new RunScriptExternal(textRetrieved, command);
            }
            // run thread that calls StdStreamRedirector.launchAndRedirect()
            scriptThread = new Thread(launcher);
            scriptThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    ExceptionNotifier.notifyDefault(
                            Messages.sqlScriptDialog_exception_during_script_execution,e);
                }
            });
            scriptThread.start();
            isRunning = true;
            parentComposite.setCursor(parentComposite.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
            // case Stop script
        } else {
            ConsoleFactory.write(Messages.sqlScriptDialog_script_execution_interrupted);
            Log.log(Log.LOG_INFO, "Script execution interrupted by user"); //$NON-NLS-1$

            scriptThread.interrupt();
            isRunning = false;
        }
    }

    private class RunScriptExternal implements Runnable {

        private final String textRetrieved;
        private final List<String> command;

        RunScriptExternal(String textRetrieved, List<String> command) {
            this.textRetrieved = textRetrieved;
            this.command = command;
        }

        @Override
        public void run() {
            final StdStreamRedirector sr = new StdStreamRedirector();
            try (TempFile tempFile = new TempFile("tmp_migration_", ".sql")) { //$NON-NLS-1$ //$NON-NLS-2$
                File outFile = tempFile.get().toFile();
                try (PrintWriter writer = new PrintWriter(outFile, ApgdiffConsts.UTF_8)) {
                    writer.write(textRetrieved);
                }

                String filepath = outFile.getAbsolutePath();
                ListIterator<String> it = command.listIterator();
                while (it.hasNext()) {
                    it.set(it.next().replace(SCRIPT_PLACEHOLDER, filepath));
                }

                ProcessBuilder pb = new ProcessBuilder(command);
                sr.launchAndRedirect(pb);
            } catch (IOException ex) {
                throw new IllegalStateException(ex.getLocalizedMessage(), ex);
            } finally {
                // request UI change: button label changed
                afterScriptFinished(sr.getStorage());
            }
        }
    }

    private static class ScriptRunResultDialog extends TrayDialog {

        private final String text;

        ScriptRunResultDialog(Shell shell, String text) {
            super(shell);
            this.text = text;
            setShellStyle(getShellStyle() | SWT.RESIZE);
        }

        @Override
        protected void configureShell(Shell newShell) {
            super.configureShell(newShell);
            newShell.setText(Messages.sqlScriptDialog_script_output);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite comp = (Composite) super.createDialogArea(parent);
            Text filed = new Text(comp, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL
                    | SWT.READ_ONLY | SWT.MULTI);
            filed.setText(text);
            filed.setBackground(getShell().getDisplay().getSystemColor(
                    SWT.COLOR_LIST_BACKGROUND));
            filed.setFont(JFaceResources.getTextFont());
            PixelConverter pc = new PixelConverter(filed);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = pc.convertWidthInCharsToPixels(80);
            gd.heightHint = pc.convertHeightInCharsToPixels(30);
            filed.setLayoutData(gd);
            return comp;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        }
    }

    public void askDeleteScript(IFile f) {
        String mode = mainPrefs.getString(DB_UPDATE_PREF.DELETE_SCRIPT_AFTER_CLOSE);
        // if select "YES" with toggle
        if (mode.equals(MessageDialogWithToggle.ALWAYS)){
            deleteFile(f);
            // if not select "NO" with toggle, show choice message dialog
        } else if (!mode.equals(MessageDialogWithToggle.NEVER)){
            MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(getSite().getShell(),
                    Messages.RollOnEditor_script_delete_dialog_title, MessageFormat.format(
                            Messages.RollOnEditor_script_delete_dialog_message, f.getName()),
                    Messages.remember_choice_toggle, false, mainPrefs, DB_UPDATE_PREF.DELETE_SCRIPT_AFTER_CLOSE);
            if(dialog.getReturnCode() == IDialogConstants.YES_ID){
                deleteFile(f);
            }
        }
    }

    private void deleteFile(IFile f) {
        try {
            Log.log(Log.LOG_INFO, "Deleting file " + f.getName()); //$NON-NLS-1$
            f.delete(true, null);
        } catch (CoreException ex) {
            Log.log(ex);
        }
    }
}

class RollOnEditorPartListener extends IPartAdapter2 {
    private final RollOnEditor rollOnEditor;

    public RollOnEditorPartListener(RollOnEditor rollOnEditor) {
        this.rollOnEditor = rollOnEditor;
    }

    @Override
    public void partClosed(IWorkbenchPartReference partRef) {
        if (partRef.getPart(false) == rollOnEditor && !PlatformUI.getWorkbench().isClosing()
                && rollOnEditor.getEditorInput() instanceof IFileEditorInput) {
            IFile f = ((IFileEditorInput) rollOnEditor.getEditorInput()).getFile();
            if (PROJ_PATH.MIGRATION_DIR.equals(f.getProjectRelativePath().segment(0))) {
                rollOnEditor.askDeleteScript(f);
            }
        }
    }
}