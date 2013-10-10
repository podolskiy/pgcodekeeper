package ru.taximaxim.codekeeper.ui.pgdbproject;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import ru.taximaxim.codekeeper.ui.UIConsts;

public class NewProjWizard extends Wizard {

	PageDb pageDb = null;
	PageSvn pageSvn = null;
	
	public NewProjWizard() {
		setWindowTitle("New PgDbProject");
		setNeedsProgressMonitor(true);
	}
	
	@Override
	public void addPages() {
		pageDb = new PageDb("DB Settings");
		addPage(pageDb);
		pageSvn = new PageSvn("SVN settings");
		addPage(pageSvn);
	}

	@Override
	public boolean performFinish() {
		String errMsg = null;
		if(pageDb.isSourceDump() && pageDb.getDumpPath().isEmpty()) {
			errMsg = "Specify dump file path if you want to create your project"
					+ " from an existing dump!";
		} else if(pageDb.getProjectPath().isEmpty()
				|| !new File(pageDb.getProjectPath()).isDirectory()) {
			errMsg = "Specify correct Project Directory!";
		} else if(pageSvn.getSvnUrl().isEmpty()) {
			errMsg = "Specify SVN repo URL to store and version"
					+ " DB schema objects!";
		}
		
		if(errMsg != null) {
			MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR);
			mb.setMessage(errMsg);
			mb.open();
			return false;
		}
		
		final PgDbProject props = new PgDbProject(pageDb.getProjectPath());
		
		props.setValue(UIConsts.PROJ_PREF_DB_NAME, pageDb.getDbName());
		props.setValue(UIConsts.PROJ_PREF_DB_USER, pageDb.getDbUser());
		props.setValue(UIConsts.PROJ_PREF_DB_PASS, pageDb.getDbPass());
		props.setValue(UIConsts.PROJ_PREF_DB_HOST, pageDb.getDbHost());
		
		props.setValue(UIConsts.PROJ_PREF_SVN_URL, pageSvn.getSvnUrl());
		props.setValue(UIConsts.PROJ_PREF_SVN_USER, pageSvn.getSvnUser());
		props.setValue(UIConsts.PROJ_PREF_SVN_PASS, pageSvn.getSvnPass());

		NewProjCreator creator = new NewProjCreator(
				props,
				pageDb.isSourceDump()? pageDb.getDumpPath() : null);
		try {
			getContainer().run(true, false, creator);
		} catch(InvocationTargetException ex) {
			throw new IllegalStateException(
					"Error in th project creator thread", ex);
		} catch(InterruptedException ex) {
			// assume run() was called as non cancelable
			throw new IllegalStateException(
					"Project creator thread cancelled. Shouldn't happen!", ex);
		}
		
		return true;
	}
}

class PageDb extends WizardPage implements Listener {
	
	private Composite container;
	
	private Button radioDb, radioDump;
	
	private Group grpDb, grpDump;
	
	private Text txtDbName, txtDbUser, txtDbPass, txtDbHost, txtDumpPath,
				txtProjectPath;
	
	private boolean checkOverwrite = true;
	
	public boolean isSourceDb() {
		return radioDb.getSelection();
	}
	
	public boolean isSourceDump() {
		return radioDump.getSelection();
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
	
	public String getDumpPath() {
		return txtDumpPath.getText();
	}
	
	public String getProjectPath() {
		return txtProjectPath.getText();
	}

	protected PageDb(String pageName) {
		super(pageName, pageName, null);
	}

	@Override
	public void createControl(Composite parent) {

		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));

		Group radioGrp = new Group(container, SWT.NONE);
		radioGrp.setText("DB Schema Source");
		radioGrp.setLayoutData(
				new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		radioGrp.setLayout(new GridLayout(2, false));
		
		radioDb = new Button(radioGrp, SWT.RADIO);
		radioDb.setText("DB Source");
		radioDb.setLayoutData(new GridData());
		radioDb.setSelection(true);
		
		radioDb.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				grpDump.setVisible(false);
				grpDb.setVisible(true);
				
				((GridData)grpDump.getLayoutData()).exclude = true;
				((GridData)grpDb.getLayoutData()).exclude = false;
				
				container.layout(false);
			}
		});
		radioDb.addListener(SWT.Selection, this);
		
		radioDump = new Button(radioGrp, SWT.RADIO);
		radioDump.setText("Dump File Source");
		radioDump.setLayoutData(new GridData());
		
		radioDump.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				grpDb.setVisible(false);
				grpDump.setVisible(true);
				
				((GridData)grpDb.getLayoutData()).exclude = true;
				((GridData)grpDump.getLayoutData()).exclude = false;
				
				container.layout(false);
			}
		});
		radioDb.addListener(SWT.Selection, this);
		
		grpDb = new Group(container, SWT.NONE);
		grpDb.setText("DB Source Settings");
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd.verticalIndent = 12;
		grpDb.setLayoutData(gd);
		grpDb.setLayout(new GridLayout(2, false));
		
		new Label(grpDb, SWT.NONE).setText("DB Name: ");
		
		txtDbName = new Text(grpDb, SWT.BORDER);
		txtDbName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		new Label(grpDb, SWT.NONE).setText("DB User: ");
		
		txtDbUser = new Text(grpDb, SWT.BORDER);
		txtDbUser.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		new Label(grpDb, SWT.NONE).setText("DB Password:");
		
		txtDbPass = new Text(grpDb, SWT.BORDER | SWT.PASSWORD);
		txtDbPass.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		new Label(grpDb, SWT.NONE).setText("DB Host:");
		
		txtDbHost = new Text(grpDb, SWT.BORDER);
		txtDbHost.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		grpDump = new Group(container, SWT.NONE);
		grpDump.setText("Dump File Source Settings");
		gd = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd.exclude = true;
		gd.verticalIndent = 12;
		grpDump.setLayoutData(gd);
		grpDump.setLayout(new GridLayout(2, false));
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
		
		l = new Label(container, SWT.NONE);
		l.setText("Project Directory (used for settings storage,"
				+ " SVN operations, temp storage, etc):");
		gd = new GridData();
		gd.horizontalSpan = 2;
		gd.verticalIndent = 12;
		l.setLayoutData(gd);
		
		txtProjectPath = new Text(container, SWT.BORDER);
		txtProjectPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		txtProjectPath.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				checkOverwrite = true;
			}
		});
		txtProjectPath.addListener(SWT.Modify, this);
		
		Button btnBrowseProj = new Button(container, SWT.PUSH);
		btnBrowseProj.setText("Browse...");
		btnBrowseProj.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = 
						new DirectoryDialog(container.getShell());
				String path = dialog.open();
				if(path != null) {
					txtProjectPath.setText(path);
				}
			}
		});
		
		setControl(container);
	}
	
	@Override
	public boolean isPageComplete() {
		String errMsg = null;
		if(radioDump.getSelection() &&
				(txtDumpPath.getText().isEmpty()
						|| !new File(txtDumpPath.getText()).isFile())) {
			errMsg = "Select a readable DB dump file!";
		} else if(txtProjectPath.getText().isEmpty()
				|| !new File(txtProjectPath.getText()).isDirectory()) {
			errMsg = "Select Project Directory!";
		}

		setErrorMessage(errMsg);
		if(errMsg != null) {
			return false;
		}
		
		if(checkOverwrite) {
			File proj = new File(txtProjectPath.getText(),
					UIConsts.PROJ_PREF_STORE_FILENAME);
			if(proj.isFile()) {
				if(MessageDialog.openQuestion(getShell(), "Overwrite existing?",
						"Overwrite existing project?\n"
									+ txtProjectPath.getText())) {
					checkOverwrite = false;
				} else {
					txtProjectPath.setText("");
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public void handleEvent(Event event) {
		getWizard().getContainer().updateButtons();
		getWizard().getContainer().updateMessage();
	}
}

class PageSvn extends WizardPage implements Listener {
	
	Text txtSvnUrl, txtSvnUser, txtSvnPass;

	public String getSvnUrl() {
		return txtSvnUrl.getText();
	}
	
	public String getSvnUser() {
		return txtSvnUser.getText();
	}
	
	public String getSvnPass() {
		return txtSvnPass.getText();
	}
	
	protected PageSvn(String pageName) {
		super(pageName, pageName, null);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));

		new Label(container, SWT.NONE).setText("SVN Repo URL:");
		
		txtSvnUrl = new Text(container, SWT.BORDER);
		txtSvnUrl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		txtSvnUrl.addListener(SWT.Modify, this);
		
		new Label(container, SWT.NONE).setText("SVN User:");
		
		txtSvnUser = new Text(container, SWT.BORDER);
		txtSvnUser.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		new Label(container, SWT.NONE).setText("SVN Password:");
		
		txtSvnPass = new Text(container, SWT.BORDER | SWT.PASSWORD);
		txtSvnPass.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		setControl(container);
	}
	
	@Override
	public boolean isPageComplete() {
		if(txtSvnUrl.getText().isEmpty()) {
			setErrorMessage("Enter URL of SVN Repo!");
			return false;
		}
		
		setErrorMessage(null);
		return true;
	}

	@Override
	public void handleEvent(Event event) {
		getWizard().getContainer().updateButtons();
		getWizard().getContainer().updateMessage();
	}
}
