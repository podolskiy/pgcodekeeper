package ru.taximaxim.codekeeper.ui.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.Version;

import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts.WORK_DIR_NAMES;
import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.UIConsts.NATURE;
import ru.taximaxim.codekeeper.ui.localizations.Messages;
import ru.taximaxim.codekeeper.ui.pgdbproject.PgDbProject;

public final class OpenProjectUtils {

    static PgDbProject getProject(ExecutionEvent event){
        try{
            ISelection sel = HandlerUtil.getActiveMenuSelection(event);
            IStructuredSelection selection = (IStructuredSelection) sel;
            if (selection == null){
                return null;
            }
            Object firstElement = selection.getFirstElement();
            if (firstElement instanceof IProject) {
                IProject proj = (IProject)firstElement;
                if (proj.getNature(NATURE.ID) != null) {
                    return new PgDbProject(proj);
                }
            }
        } catch (CoreException ce){
            Log.log(Log.LOG_ERROR, ce.getMessage());
        }
        return null;
    }

    public static boolean checkVersionAndWarn(IProject proj, Shell parent,
            boolean warnNonBlockers) {
        StringBuilder err = new StringBuilder();
        final boolean canContinue = checkVersion(proj, err);
        final boolean isError = !canContinue;
        if (err.length() != 0 && (isError || warnNonBlockers)) {
            warnVersion(parent, err.toString(), isError);
        }
        return canContinue;
    }

    private static boolean checkVersion(IProject proj, StringBuilder message) {
        message.setLength(0);

        File markerFile = new File(proj.getLocation().toFile(),
                ApgdiffConsts.FILENAME_WORKING_DIR_MARKER);
        try (FileInputStream stream = new FileInputStream(markerFile)) {
            Properties props = new Properties();
            props.load(stream);

            String verStr = props.getProperty(
                    ApgdiffConsts.VERSION_PROP_NAME, "").trim(); //$NON-NLS-1$
            if (verStr.isEmpty()) {
                message.append(Messages.OpenProjectUtils_unknown_proj_version);
                return true;
            }

            Version ver;
            try {
                ver = new Version(verStr);
            } catch (IllegalArgumentException ex) {
                message.append(Messages.OpenProjectUtils_unknown_proj_version);
                return true;
            }
            Version curVer = new Version(ApgdiffConsts.EXPORT_CURRENT_VERSION);
            if (ver.compareTo(curVer) > 0) {
                message.append(Messages.OpenProjectUtils_high_proj_version)
                .append(verStr)
                .append(" > ") //$NON-NLS-1$
                .append(ApgdiffConsts.EXPORT_CURRENT_VERSION);
                return false;
            }
            if (ver.compareTo(curVer) < 0) {
                message.append(Messages.OpenProjectUtils_low_proj_version);
                return true;
            }

        } catch (FileNotFoundException ex) {
            message.append(MessageFormat.format(Messages.OpenProjectUtils_file,
                    markerFile.getAbsolutePath()));
            return false;
        } catch (IOException ex) {
            Log.log(ex);
            message.append(Messages.OpenProjectUtils_unexpected_version_error);
            return false;
        }
        return true;
    }

    /**
     * Shows a message box with version warning.
     * @param isError does the error block the project from opening
     */
    private static void warnVersion(Shell parent, String error, boolean isError) {
        MessageBox mb = new MessageBox(
                parent, isError? SWT.ICON_ERROR : SWT.ICON_WARNING);
        mb.setText(Messages.OpenProjectUtils_version_error);

        String msg = isError? Messages.OpenProjectUtils_proj_version_unsupported :
            Messages.OpenProjectUtils_proj_version_warn;
        mb.setMessage(msg + error);
        mb.open();
    }

    /**
     * @throws CoreException only when user chose to convert project and the following process failed
     */
    public static void checkLegacySchemas(IProject proj, Shell shell) throws CoreException {
        IFolder schemasDir = proj.getFolder(WORK_DIR_NAMES.SCHEMA.name());
        if (!schemasDir.exists()) {
            return;
        }

        List<IFile> schemas;
        try {
            schemas = Arrays.stream(schemasDir.members())
                    .filter(r -> r.getType() == IResource.FILE && "sql".equals(r.getFileExtension()))
                    .map(r -> (IFile) r)
                    .collect(Collectors.toList());
        } catch (CoreException ex) {
            Log.log(ex);
            // we don't know whether user actually wants to convert
            // or even whether there's anything to convert
            // fail silently, this shouldn't happen anyway
            return;
        }

        if (!schemas.isEmpty()) {
            MessageBox mb = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            mb.setText("Update project format?");
            mb.setMessage(MessageFormat.format(
                    "Project '{0}' contains legacy schema files.\nIt is recommended to update the project by moving schema files into their respective directories.\n\nDo you want to perform this now?",
                    proj.getName()));
            if (mb.open() != SWT.YES) {
                return;
            }
        } else {
            return;
        }

        IRunnableWithProgress runnable = monitor -> {
            SubMonitor m = SubMonitor.convert(monitor, "Updating project", schemas.size() + 1);
            try {
                proj.refreshLocal(IResource.DEPTH_INFINITE, m.newChild(1));
                for (IResource r : schemas) {
                    SubMonitor sm = m.newChild(1);
                    IPath schemaPath = r.getProjectRelativePath().removeFileExtension();
                    if (!proj.exists(schemaPath)) {
                        proj.getFolder(schemaPath).create(false, true, sm.newChild(1));
                    }
                    // move relative to original
                    IPath newPath = schemaPath
                            .removeFirstSegments(schemaPath.segmentCount() - 1)
                            .append(r.getName());
                    r.move(newPath, false, sm.newChild(1));
                }
            } catch (CoreException e) {
                throw new InvocationTargetException(e, e.getLocalizedMessage());
            } finally {
                monitor.done();
            }
        };
        try {
            new ProgressMonitorDialog(shell).run(true, false, runnable);
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (t instanceof CoreException) {
                throw (CoreException) t;
            } else {
                throw new IllegalStateException(t.getLocalizedMessage(), e);
            }
        } catch (InterruptedException e) {
            // can't be cancelled
        }
    }

    private OpenProjectUtils() {
    }
}
