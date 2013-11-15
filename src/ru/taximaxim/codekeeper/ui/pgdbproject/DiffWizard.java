package ru.taximaxim.codekeeper.ui.pgdbproject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.Set;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.safi.jface.CheckboxTreeSelectionHelper;

import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement.DbObjType;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement.DiffSide;
import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.differ.DbSource;
import ru.taximaxim.codekeeper.ui.differ.Differ;
import ru.taximaxim.codekeeper.ui.differ.TreeDiffer;

public class DiffWizard extends Wizard implements IPageChangingListener {

    private PageDiff pageDiff;
    private PagePartial pagePartial;
    
    private final PgDbProject proj;
    
    private final IPreferenceStore mainPrefs;
    
    private DbSource db1, db2;
    
    private String diffResult;
    
    public DbSource getDb1() {
        return db1;
    }
    
    public DbSource getDb2() {
        return db2;
    }
    
    public String getDiffResult() {
        return diffResult;
    }
    
    public DiffWizard(PgDbProject proj, IPreferenceStore mainPrefs) {
        setWindowTitle("Diff");
        setNeedsProgressMonitor(true);
        
        this.proj = proj;
        this.mainPrefs = mainPrefs;
    }
    
    @Override
    public void addPages() {
        pageDiff = new PageDiff("Diff parameters", mainPrefs, proj);
        pagePartial = new PagePartial("Diff tree");
        
        addPage(pageDiff);
        addPage(pagePartial);
    }
    
    // objects from handlePageChanging() needed in perfromFinish()
    DbSource dbFrom, dbTo;
    
    @Override
    public void handlePageChanging(PageChangingEvent e) {
        if(e.getCurrentPage() == pageDiff && e.getTargetPage() == pagePartial) {
            TreeDiffer treediffer = new TreeDiffer(
                    DbSource.fromProject(proj), pageDiff.getTargetDbSource(), 
                    !pageDiff.isDirectionFromThis());
            
            try {
                try {
                    getContainer().run(true, false, treediffer);
                } catch(InvocationTargetException ex) {
                    throw new IllegalStateException("Error in differ thread", ex);
                } catch(InterruptedException ex) {
                    // assume run() was called as non cancelable
                    throw new IllegalStateException(
                            "Differ thread cancelled. Shouldn't happen!", ex);
                }
            } catch(Exception ex) {
                e.doit = false;
                throw ex;
            }
            
            dbFrom = treediffer.getDbFrom();
            dbTo = treediffer.getDbTo();
            
            pagePartial.setLabels(dbFrom.getOrigin(), dbTo.getOrigin());
            pagePartial.setDiffTreeInput(treediffer.getDiffTree());
            pagePartial.layout();
        }
    }
    
    @Override
    public boolean canFinish() {
        if(getContainer().getCurrentPage() != pagePartial) {
            return false;
        }
        return super.canFinish();
    }
    
    @Override
    public boolean performFinish() {
        TreeElement filtered = filterDiffTree(pagePartial.getDiffTree(),
                (TreeElement) pagePartial.getDiffTree().getInput());
        
        db1 = DbSource.fromFilter(dbFrom, filtered, DiffSide.LEFT);
        db2 = DbSource.fromFilter(dbTo, filtered, DiffSide.RIGHT);
        
        Differ differ = new Differ(db1, db2, false);
        try {
            getContainer().run(true, false, differ);
        } catch(InvocationTargetException ex) {
            throw new IllegalStateException("Error in differ thread", ex);
        } catch(InterruptedException ex) {
            // assume run() was called as non cancelable
            throw new IllegalStateException(
                    "Differ thread cancelled. Shouldn't happen!", ex);
        }
        diffResult = differ.getDiff();
        
        return true;
    }
    
    // recursively copy only selected tree elements into a new tree
    private TreeElement filterDiffTree(CheckboxTreeViewer viewer,
            TreeElement diffTree) {
        if(diffTree.getType() != DbObjType.DATABASE 
                && !viewer.getChecked(diffTree)
                && !viewer.getGrayed(diffTree)) {
            // skip unselected non-root nodes and all their children
            return null;
        }
        
        TreeElement copy = new TreeElement(
                diffTree.getName(), diffTree.getType(), diffTree.getSide());
        
        for(TreeElement sub : diffTree.getChildren()) {
            TreeElement subCopy = filterDiffTree(viewer, sub);
            if(subCopy != null) {
                copy.addChild(subCopy);
            }
        }
        return copy;
    }
}

class PageDiff extends WizardPage implements Listener {
    
    public enum DiffTargetType {
        DB, DUMP, SVN, PROJ
    }
    
    final private IPreferenceStore mainPrefs;
    
    final private PgDbProject proj;
    
    private Composite container;
    
    private Button radioDb, radioDump, radioSvn, radioProj;
    
    private Group currentTargetGrp;
    
    private Group grpDb, grpDump, grpSvn, grpProj;
    
    private Text txtDbName, txtDbUser, txtDbPass, txtDbHost, txtDbPort,
        txtDumpPath,
        txtSvnUrl, txtSvnUser, txtSvnPass, txtSvnRev,
        txtProjPath, txtProjRev;
    
    private CLabel lblWarnDbPass, lblWarnSvnPass;
    
    private Combo cmbEncoding;
    
    private Button radioFromThis, radioToThis;
    
    public DiffTargetType getTargetType() {
        if(radioDb.getSelection()) {
            return DiffTargetType.DB;
        }
        if(radioDump.getSelection()) {
            return DiffTargetType.DUMP;
        }
        if(radioSvn.getSelection()) {
            return DiffTargetType.SVN;
        }
        if(radioProj.getSelection()) {
            return DiffTargetType.PROJ;
        }
        
        throw new IllegalStateException("No target type selection found!");
    }
    
    public String getDbName() {
        return txtDbName.getText();
    }
    
    public String getDbUser() {
        return txtDbUser.getText();
    }
    
    public String getDbPass() {
        return txtDbPass.getText();
    }
    
    public String getDbHost() {
        return txtDbHost.getText();
    }
    
    public int getDbPort() {
        try {
            return Integer.parseInt(txtDbPort.getText());
        } catch(NumberFormatException ex) {
            return 0;
        }
    }
    
    public String getDumpPath() {
        return txtDumpPath.getText();
    }
    
    public String getSvnUrl() {
        return txtSvnUrl.getText();
    }
    
    public String getSvnUser() {
        return txtSvnUser.getText();
    }
    
    public String getSvnPass() {
        return txtSvnPass.getText();
    }
    
    public String getSvnRev() {
        return txtSvnRev.getText();
    }
    
    public String getProjPath() {
        return txtProjPath.getText();
    }
    
    public String getProjRev() {
        return txtProjRev.getText();
    }
    
    public String getTargetEncoding() {
        return cmbEncoding.getText();
    }
    
    public boolean isDirectionFromThis() {
        return radioFromThis.getSelection();
    }
    
    public DbSource getTargetDbSource() {
        DbSource dbs;
        
        switch(getTargetType()) {
        case DB:
            dbs = DbSource.fromDb(
                    mainPrefs.getString(UIConsts.PREF_PGDUMP_EXE_PATH),
                    getDbHost(), getDbPort(), getDbUser(), getDbPass(),
                    getDbName(), getTargetEncoding());
            break;
            
        case DUMP:
            dbs = DbSource.fromFile(getDumpPath(), getTargetEncoding());
            break;
            
        case SVN:
            dbs = DbSource.fromSvn(
                    mainPrefs.getString(UIConsts.PREF_SVN_EXE_PATH),
                    getSvnUrl(), getSvnUser(), getSvnPass(), getSvnRev(),
                    getTargetEncoding());
            break;
            
        case PROJ:
            PgDbProject fromProj = new PgDbProject(getProjPath());
            try {
                fromProj.load();
            } catch(IOException ex) {
                throw new IllegalStateException(
                        "Unexpected error while reading target project!", ex);
            }
            
            if(getProjRev().isEmpty()) {
                dbs = DbSource.fromProject(fromProj);
            } else {
                dbs = DbSource.fromSvn(
                        mainPrefs.getString(UIConsts.PREF_SVN_EXE_PATH),
                        fromProj.getString(UIConsts.PROJ_PREF_SVN_URL),
                        fromProj.getString(UIConsts.PROJ_PREF_SVN_USER),
                        fromProj.getString(UIConsts.PROJ_PREF_SVN_PASS),
                        getProjRev(),
                        fromProj.getString(UIConsts.PROJ_PREF_ENCODING));
            }
            break;
        
        default:
            throw new IllegalStateException("Unexpected target type value!");
        }
        
        return dbs;
    }
    
    public PageDiff(String pageName, IPreferenceStore mainPrefs, PgDbProject proj) {
        super(pageName, pageName, null);
        
        this.mainPrefs = mainPrefs;
        this.proj = proj;
    }
    
    @Override
    public void createControl(Composite parent) {
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout());
        
        Group grpRadio = new Group(container, SWT.NONE);
        grpRadio.setText("Diff target");
        grpRadio.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        grpRadio.setLayout(new GridLayout(4, false));
        
        SelectionListener switcher = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Button cause = (Button) e.getSource();
                
                if(cause.getSelection()) {
                    Group to = (Group) cause.getData();
                    
                    if(to != grpProj) {
                        cmbEncoding.setEnabled(true);
                        cmbEncoding.select(cmbEncoding.indexOf("UTF-8"));
                    } else {
                        cmbEncoding.setEnabled(false);
                        txtProjPath.notifyListeners(SWT.Modify, null);
                    }
                    switchTargetGrp(to);
                }
            }
        };
        
        radioDb = new Button(grpRadio, SWT.RADIO);
        radioDb.setText("DB target");
        radioDb.setSelection(true);
        radioDb.addSelectionListener(switcher);
        
        radioDump = new Button(grpRadio, SWT.RADIO);
        radioDump.setText("Dump target");
        radioDump.addSelectionListener(switcher);
        
        radioSvn = new Button(grpRadio, SWT.RADIO);
        radioSvn.setText("SVN target");
        radioSvn.addSelectionListener(switcher);
        
        radioProj = new Button(grpRadio, SWT.RADIO);
        radioProj.setText("Project target");
        radioProj.addSelectionListener(switcher);
        
        grpDb = new Group(container, SWT.NONE);
        grpDb.setText("DB target");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalIndent = 12;
        grpDb.setLayoutData(gd);
        grpDb.setLayout(new GridLayout(4, false));
        
        currentTargetGrp = grpDb;
        
        new Label(grpDb, SWT.NONE).setText("DB Name: ");
        
        txtDbName = new Text(grpDb, SWT.BORDER);
        txtDbName.setLayoutData(new GridData(
                SWT.FILL, SWT.CENTER, true, false, 3, 1));
        
        new Label(grpDb, SWT.NONE).setText("DB User: ");
        
        txtDbUser = new Text(grpDb, SWT.BORDER);
        txtDbUser.setLayoutData(new GridData(
                SWT.FILL, SWT.CENTER, true, false, 3, 1));
        
        new Label(grpDb, SWT.NONE).setText("DB Password:");
        
        txtDbPass = new Text(grpDb, SWT.BORDER | SWT.PASSWORD);
        txtDbPass.setLayoutData(new GridData(
                SWT.FILL, SWT.CENTER, true, false, 3, 1));
        txtDbPass.addModifyListener(new ModifyListener() {
            
            @Override
            public void modifyText(ModifyEvent e) {
                GridData gd = (GridData)lblWarnDbPass.getLayoutData();
                
                if((txtDbPass.getText().isEmpty() && !gd.exclude)
                        || (!txtDbPass.getText().isEmpty() && gd.exclude)) {
                    lblWarnDbPass.setVisible(!lblWarnDbPass.getVisible());
                    gd.exclude = !gd.exclude;
                    
                    Shell sh = container.getShell();
                    int width = sh.getSize().x;
                    int newht = sh.computeSize(width, SWT.DEFAULT).y;
                    sh.setSize(width, newht);
                    
                    grpDb.layout(false);
                }
            }
        });
        
        lblWarnDbPass = new CLabel(grpDb, SWT.NONE);
        lblWarnDbPass.setImage(ImageDescriptor.createFromURL(
                Activator.getContext().getBundle().getResource(
                        UIConsts.FILENAME_ICONWARNING)).createImage());
        lblWarnDbPass.setText("Warning:\n"
                + "Providing password here is insecure!\n"
                + "Consider using .pgpass file instead.");
        gd = new GridData(SWT.FILL, SWT.FILL, false, false, 4, 1);
        gd.exclude = true;
        lblWarnDbPass.setLayoutData(gd);
        lblWarnDbPass.setVisible(false);
        
        new Label(grpDb, SWT.NONE).setText("DB Host:");
        
        txtDbHost = new Text(grpDb, SWT.BORDER);
        txtDbHost.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        new Label(grpDb, SWT.NONE).setText("Port:");
        
        txtDbPort = new Text(grpDb, SWT.BORDER);
        txtDbPort.addListener(SWT.Modify, this);
        
        grpDump = new Group(container, SWT.NONE);
        grpDump.setText("Dump target");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalIndent = 12;
        grpDump.setLayoutData(gd);
        grpDump.setLayout(new GridLayout(2, false));
        
        gd.exclude = true;
        grpDump.setVisible(false);
        
        Label l = new Label(grpDump, SWT.NONE);
        l.setText("Path to DB Schema Dump:");
        gd = new GridData();
        gd.horizontalSpan = 2;
        l.setLayoutData(gd);
        
        txtDumpPath = new Text(grpDump, SWT.BORDER);
        txtDumpPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        txtDumpPath.addListener(SWT.Modify, this);
        
        Button btnBrowseDump = new Button(grpDump, SWT.PUSH);
        btnBrowseDump.setText("Browse...");
        btnBrowseDump.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(container.getShell());
                String filename = dialog.open();
                if(filename != null) {
                    txtDumpPath.setText(filename);
                }
            }
        });

        grpSvn = new Group(container, SWT.NONE);
        grpSvn.setText("SVN target");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalIndent = 12;
        grpSvn.setLayoutData(gd);
        grpSvn.setLayout(new GridLayout(2, false));
        
        gd.exclude = true;
        grpSvn.setVisible(false);
        
        new Label(grpSvn, SWT.NONE).setText("SVN Repo URL:");
        
        txtSvnUrl = new Text(grpSvn, SWT.BORDER);
        txtSvnUrl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        txtSvnUrl.addListener(SWT.Modify, this);
        
        new Label(grpSvn, SWT.NONE).setText("SVN User:");
        
        txtSvnUser = new Text(grpSvn, SWT.BORDER);
        txtSvnUser.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        new Label(grpSvn, SWT.NONE).setText("SVN Password:");
        
        txtSvnPass = new Text(grpSvn, SWT.BORDER | SWT.PASSWORD);
        txtSvnPass.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        txtSvnPass.addModifyListener(new ModifyListener() {
            
            @Override
            public void modifyText(ModifyEvent e) {
                GridData gd = (GridData)lblWarnSvnPass.getLayoutData();
                
                if((txtSvnPass.getText().isEmpty() && !gd.exclude)
                        || (!txtSvnPass.getText().isEmpty() && gd.exclude)) {
                    gd.exclude = !gd.exclude;
                    lblWarnSvnPass.setVisible(!lblWarnSvnPass.getVisible());
                    
                    Shell sh = container.getShell();
                    int width = sh.getSize().x;
                    int newht = sh.computeSize(width, SWT.DEFAULT).y;
                    sh.setSize(width, newht);
                    
                    grpSvn.layout(false);
                }
            }
        });
        
        lblWarnSvnPass = new CLabel(grpSvn, SWT.NONE);
        lblWarnSvnPass.setImage(ImageDescriptor.createFromURL(
                Activator.getContext().getBundle().getResource(
                        UIConsts.FILENAME_ICONWARNING)).createImage());
        lblWarnSvnPass.setText("Warning:\n"
                + "Providing password here is insecure!"
                + " This password WILL show up in logs!\n"
                + "Consider using SVN password store instead.");
        gd = new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1);
        gd.exclude = true;
        lblWarnSvnPass.setLayoutData(gd);
        lblWarnSvnPass.setVisible(false);
        
        new Label(grpSvn, SWT.NONE).setText("SVN Revision:");
        
        txtSvnRev = new Text(grpSvn, SWT.BORDER);
        txtSvnRev.setLayoutData(new GridData());
        txtSvnRev.addListener(SWT.Modify, this);
        
        grpProj = new Group(container, SWT.NONE);
        grpProj.setText("Project target");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalIndent = 12;
        grpProj.setLayoutData(gd);
        grpProj.setLayout(new GridLayout(2, false));
        
        gd.exclude = true;
        grpProj.setVisible(false);
        
        final Button btnThis = new Button(grpProj, SWT.CHECK);
        btnThis.setText("This project");
        btnThis.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        btnThis.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if(btnThis.getSelection()) {
                    txtProjPath.setText(proj.getProjectDir());
                    txtProjPath.setEnabled(false);
                } else {
                    txtProjPath.setEnabled(true);
                }
            }
        });
        
        Composite tmpCont = new Composite(grpProj, SWT.NONE);
        tmpCont.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        tmpCont.setLayout(new GridLayout(2, false));
        
        l = new Label(tmpCont, SWT.NONE);
        l.setText("Path to Target Project:");
        l.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        
        txtProjPath = new Text(tmpCont, SWT.BORDER);
        txtProjPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        txtProjPath.addModifyListener(new ModifyListener() {
            
            @Override
            public void modifyText(ModifyEvent e) {
                String dir = txtProjPath.getText();
                
                if(!dir.isEmpty() && new File(dir).isDirectory()) {
                    PgDbProject tmpProj = new PgDbProject(dir);
                    
                    if(tmpProj.getProjectPropsFile().isFile()) {
                        try {
                            tmpProj.load();
                        } catch(IOException ex) {
                            throw new IllegalStateException(
                                    "Unexpected error while reading targetproject", ex);
                        }
                        cmbEncoding.select(cmbEncoding.indexOf(
                                tmpProj.getString(UIConsts.PROJ_PREF_ENCODING)));
                    }
                }
            }
        });
        txtProjPath.addListener(SWT.Modify, this);
        
        Button btnBrowseProj = new Button(tmpCont, SWT.PUSH);
        btnBrowseProj.setText("Browse...");
        btnBrowseProj.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(container.getShell());
                String path = dialog.open();
                if(path != null) {
                    txtProjPath.setText(path);
                    txtProjPath.setEnabled(true);
                    btnThis.setSelection(false);
                }
            }
        });
        
        new Label(grpProj, SWT.NONE).setText("Project revision (grab from SVN):");
        
        txtProjRev = new Text(grpProj, SWT.BORDER);
        txtProjRev.setLayoutData(new GridData());
        
        radioDb.setData(grpDb);
        radioDump.setData(grpDump);
        radioSvn.setData(grpSvn);
        radioProj.setData(grpProj);
        
        Group grpEncoding = new Group(container, SWT.NONE);
        grpEncoding.setText("Target encoding");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalIndent = 12;
        grpEncoding.setLayoutData(gd);
        grpEncoding.setLayout(new GridLayout(2, false));
        
        new Label(grpEncoding, SWT.NONE).setText("Encoding of the target:");
        
        cmbEncoding = new Combo(grpEncoding, SWT.BORDER | SWT.DROP_DOWN
                | SWT.READ_ONLY);
        Set<String> charsets = Charset.availableCharsets().keySet();
        cmbEncoding.setItems(charsets.toArray(new String[charsets.size()]));
        cmbEncoding.select(cmbEncoding.indexOf("UTF-8"));
        
        Group grpDirection = new Group(container, SWT.NONE);
        grpDirection.setText("Diff direction");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalIndent = 12;
        grpDirection.setLayoutData(gd);
        grpDirection.setLayout(new GridLayout());
        
        radioFromThis = new Button(grpDirection, SWT.RADIO);
        radioFromThis.setText("This project -> Target");
        radioFromThis.setSelection(true);
        
        radioToThis = new Button(grpDirection, SWT.RADIO);
        radioToThis.setText("Target -> This project");
        
        setControl(container);
        
        ((WizardDialog) getContainer()).addPageChangingListener(
                (DiffWizard) getWizard());
    }
    
    private void switchTargetGrp(Group newActive) {
        currentTargetGrp.setVisible(false);
        newActive.setVisible(true);
        
        ((GridData) currentTargetGrp.getLayoutData()).exclude = true;
        ((GridData) newActive.getLayoutData()).exclude = false;
        
        currentTargetGrp = newActive;
        
        Shell sh = container.getShell();
        int width = sh.getSize().x;
        int newht = sh.computeSize(width, SWT.DEFAULT).y;
        sh.setSize(width, newht);
        
        container.layout(false);
        
        getWizard().getContainer().updateButtons();
        getWizard().getContainer().updateMessage();
    }
    
    @Override
    public boolean isPageComplete() {
        String errMsg = null;
        
        switch(getTargetType()) {
        case DB:
            if(!txtDbPort.getText().isEmpty()) {
                try {
                    Integer.parseInt(txtDbPort.getText());
                } catch(NumberFormatException ex) {
                    errMsg = "Port must be a number!";
                }
            }
            break;
            
        case DUMP:
            if(txtDumpPath.getText().isEmpty()
                    || !new File(txtDumpPath.getText()).isFile()) {
                errMsg = "Select a readable DB dump file!";
            }
            break;
            
        case SVN:
            if(txtSvnUrl.getText().isEmpty()) {
                errMsg = "Enter SVN Repo URL!";
            }
            break;
            
        case PROJ:
            String dir = txtProjPath.getText();
            
            if(dir.isEmpty() || !new File(dir).isDirectory()
                    || !new PgDbProject(dir).getProjectPropsFile().isFile()) {
                errMsg = "Select a valid project directory!";
            }
            
            break;
        }
        
        setErrorMessage(errMsg);
        
        if(errMsg != null) {
            return false;
        }
        return true;
    }
    
    @Override
    public void handleEvent(Event event) {
        getWizard().getContainer().updateButtons();
        getWizard().getContainer().updateMessage();
    }
}

class PagePartial extends WizardPage {
    
    private Composite container;
    
    private Label lblFrom, lblTo;
    
    private CheckboxTreeViewer diffTree;
    
    public void setLabels(String from, String to) {
        lblFrom.setText(from);
        lblTo.setText(to);
    }
    
    public CheckboxTreeViewer getDiffTree() {
        return diffTree;
    }
    
    public void setDiffTreeInput(TreeElement root) {
        diffTree.setInput(root);
    }
    
    public PagePartial(String pageName) {
        super(pageName, pageName, null);
    }
    
    public void layout() {
        container.getShell().setSize(
                container.getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
        container.layout(false);
    }
    
    @Override
    public void createControl(Composite parent) {
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout());
        
        new Label(container, SWT.NONE).setText("From (LEFT):");
        lblFrom = new Label(container, SWT.WRAP);
        new Label(container, SWT.NONE).setText("To (RIGHT):");
        lblTo = new Label(container, SWT.WRAP);
        
        final Button btnSelectAll = new Button(container, SWT.CHECK);
        btnSelectAll.setText("Select all");
        GridData gd = new GridData();
        gd.verticalIndent = 12;
        btnSelectAll.setLayoutData(gd);
        btnSelectAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TreeElement root = (TreeElement) diffTree.getInput();
                boolean checked = btnSelectAll.getSelection();
                for(TreeElement sub : root.getChildren()) {
                    diffTree.setSubtreeChecked(sub, checked);
                }
            }
        });
        
        diffTree = new CheckboxTreeViewer(container);
        diffTree.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        
        diffTree.setContentProvider(new ITreeContentProvider() {
            
            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }
            
            @Override
            public void dispose() {
            }
            
            @Override
            public boolean hasChildren(Object element) {
                return ((TreeElement) element).hasChildren();
            }
            
            @Override
            public Object getParent(Object element) {
                return ((TreeElement) element).getParent();
            }
            
            @Override
            public Object[] getElements(Object inputElement) {
                return ((TreeElement) inputElement).getChildren().toArray();
            }
            
            @Override
            public Object[] getChildren(Object parentElement) {
                return getElements(parentElement);
            }
        });
        diffTree.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                TreeElement el = (TreeElement) element;
                
                return String.format("%s:%s:%s",
                        el.getType(), el.getName(), el.getSide());
            }
        });
        CheckboxTreeSelectionHelper.attach(
                diffTree, (ITreeContentProvider) diffTree.getContentProvider());
        
        setControl(container);
    }
    
    @Override
    public IWizardPage getPreviousPage() {
        return null;
    }
}