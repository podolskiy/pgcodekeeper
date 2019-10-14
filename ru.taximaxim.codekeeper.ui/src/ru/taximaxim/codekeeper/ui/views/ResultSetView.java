package ru.taximaxim.codekeeper.ui.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.part.ViewPart;

import cz.startnet.utils.pgdiff.PgDiffUtils;
import ru.taximaxim.codekeeper.apgdiff.fileutils.FileUtils;
import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.localizations.Messages;

public class ResultSetView extends ViewPart {

    private CTabFolder tabFolder;

    @Override
    public void createPartControl(Composite parent) {
        tabFolder = new CTabFolder(parent, SWT.BOTTOM);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 700;
        tabFolder.setLayoutData(gd);
        tabFolder.addListener(SWT.MenuDetect, event -> {
            createPopupMenuForTab(tabFolder.getItem(tabFolder.getDisplay()
                    .map(null, tabFolder, new Point(event.x,event.y))));
        });
    }

    private void createPopupMenuForTab(CTabItem clickedQueryTab) {
        // getting index for clicked query tab
        int idx = 0;
        CTabItem[] t = tabFolder.getItems();
        for (int i = 0; i < t.length; i++) {
            if (clickedQueryTab.equals(t[i])) {
                idx = i;
                break;
            }
        }

        // determination for displaying menu items
        boolean createCloseRightItem = false;
        boolean createCloseLeftItem = false;
        boolean createCloseOthersItem = true;
        boolean createCloseAllItem = true;
        int maxArrIdx = (t.length - 1);
        if (idx > 0 && idx < maxArrIdx) {
            createCloseRightItem = true;
            createCloseLeftItem = true;
        } else if (idx == 0 && maxArrIdx > 0) {
            createCloseRightItem = true;
            createCloseLeftItem = false;
        } else if (maxArrIdx == idx && maxArrIdx > 0) {
            createCloseRightItem = false;
            createCloseLeftItem = true;
        } else if (maxArrIdx == 0) {
            createCloseOthersItem = false;
            createCloseAllItem = false;
        }

        Menu popupMenu = new Menu(tabFolder);

        MenuItem closeItem = new MenuItem(popupMenu, SWT.NONE);
        closeItem.setText(Messages.resultSetView_close);
        closeItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                Object obj = e.getSource();
                if (obj instanceof MenuItem) {
                    clickedQueryTab.dispose();
                }
            }
        });

        if (createCloseOthersItem) {
            MenuItem closeOthersItem = new MenuItem(popupMenu, SWT.NONE);
            closeOthersItem.setText(Messages.resultSetView_close_others);
            closeOthersItem.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    Object obj = e.getSource();
                    if (obj instanceof MenuItem) {
                        Arrays.stream(tabFolder.getItems())
                        .filter(item -> !clickedQueryTab.equals(item))
                        .forEach(CTabItem::dispose);
                    }
                }
            });
        }

        if (createCloseLeftItem) {
            MenuItem closeLeftItem = new MenuItem(popupMenu, SWT.NONE);
            closeLeftItem.setText(Messages.resultSetView_close_tabs_to_the_left);
            closeLeftItem.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    Object obj = e.getSource();
                    if (obj instanceof MenuItem) {
                        boolean isReachedClickedQueryTab = false;
                        ArrayList<CTabItem> tabs = new ArrayList<>(
                                Arrays.asList(tabFolder.getItems()));
                        ListIterator<CTabItem> listIter = tabs.listIterator(tabs.size());
                        while (listIter.hasPrevious()) {
                            CTabItem previousTab = listIter.previous();
                            if (isReachedClickedQueryTab) {
                                previousTab.dispose();
                                continue;
                            }
                            if (clickedQueryTab.equals(previousTab)) {
                                isReachedClickedQueryTab = true;
                            }
                        }
                    }
                }
            });
        }

        if (createCloseRightItem) {
            MenuItem closeRightItem = new MenuItem(popupMenu, SWT.NONE);
            closeRightItem.setText(Messages.resultSetView_close_tabs_to_the_right);
            closeRightItem.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    Object obj = e.getSource();
                    if (obj instanceof MenuItem) {
                        boolean isReachedClickedQueryTab = false;
                        Iterator<CTabItem> iter = new ArrayList<>(
                                Arrays.asList(tabFolder.getItems())).iterator();
                        while (iter.hasNext()) {
                            CTabItem nextTab = iter.next();
                            if (isReachedClickedQueryTab) {
                                nextTab.dispose();
                                continue;
                            }
                            if (clickedQueryTab.equals(nextTab)) {
                                isReachedClickedQueryTab = true;
                            }
                        }
                    }
                }
            });
        }

        if (createCloseAllItem) {
            new MenuItem(popupMenu, SWT.SEPARATOR);

            MenuItem closeAllItem = new MenuItem(popupMenu, SWT.NONE);
            closeAllItem.setText(Messages.resultSetView_close_all);
            closeAllItem.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    Object obj = e.getSource();
                    if (obj instanceof MenuItem) {
                        Arrays.stream(tabFolder.getItems()).forEach(CTabItem::dispose);
                    }
                }
            });
        }

        tabFolder.setMenu(popupMenu);
    }

    public void addData(String query, List<List<Object>> results) {
        if (results.isEmpty()) {
            return;
        }

        Composite tabComposite = new Composite(tabFolder, SWT.NONE);
        tabComposite.setLayout(new GridLayout());
        tabComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        CTabItem tabItem = new CTabItem(tabFolder, SWT.CLOSE);
        tabItem.setText(FileUtils.getFileDate());
        tabItem.setControl(tabComposite);

        Label l = new Label(tabComposite, SWT.NONE);

        String preview = query.replaceAll("\\s+", " ").trim(); //$NON-NLS-1$ //$NON-NLS-2$
        l.setText(preview.length() > 60 ? preview.substring(0, 60) + " <...> " : preview); //$NON-NLS-1$
        l.setToolTipText(query);

        TableViewer viewer = new TableViewer(tabComposite);

        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
        viewer.getTable().setLinesVisible(true);
        viewer.getTable().setHeaderVisible(true);

        viewer.getTable().addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                if (selection.isEmpty() || (e.stateMask & SWT.CTRL) != SWT.CTRL || e.keyCode != 'c') {
                    return;
                }

                StringBuilder sb = new StringBuilder();
                for (Object r : selection.toList()) {
                    List<?> row = (List<?>) r;
                    for (Object val : row) {
                        sb.append(valueForCsv(val));
                        sb.append(',');
                    }
                    // remove trailing comma
                    sb.setLength(sb.length() - 1);
                    sb.append(UIConsts._NL);
                }

                final Clipboard cb = new Clipboard(viewer.getControl().getDisplay());
                cb.setContents(new Object[] {sb.toString()}, new Transfer[] {TextTransfer.getInstance()});
            }
        });


        List<Object> names = results.get(0);

        TableViewerColumn num = new TableViewerColumn(viewer, SWT.LEFT);
        num.getColumn().setResizable(true);
        num.setLabelProvider(new RowNumberLabelProvider());
        num.getColumn().setWidth(40);

        for (int i = 0; i < names.size(); i++) {
            TableViewerColumn col = new TableViewerColumn(viewer, SWT.LEFT);
            col.getColumn().setResizable(true);
            col.getColumn().setMoveable(true);
            col.getColumn().setText(names.get(i).toString());
            col.setLabelProvider(new IndexedColumnLabelProvider(i));
            col.getColumn().setWidth(150);
        }

        viewer.setInput(results.subList(1, results.size()));
        tabFolder.setSelection(tabItem);
    }

    @Override
    public void setFocus() {
        tabFolder.setFocus();
    }

    private String valueForCsv(Object val) {
        if (val == null) {
            return "NULL"; //$NON-NLS-1$
        }
        return PgDiffUtils.quoteString(val.toString());
    }

    private static class IndexedColumnLabelProvider extends ColumnLabelProvider {
        private final int i;

        private IndexedColumnLabelProvider(int i) {
            this.i = i;
        }

        @Override
        public String getText(Object element) {
            List<?> l = (List<?>) element;
            Object obj = l.get(i);
            return obj == null ? "<NULL>" : obj.toString(); //$NON-NLS-1$
        }
    }

    private static class RowNumberLabelProvider extends ColumnLabelProvider {

        private TableViewer viewer;

        @Override
        protected void initialize(ColumnViewer viewer, ViewerColumn column) {
            super.initialize(viewer, column);
            this.viewer = null;
            if (viewer instanceof TableViewer) {
                this.viewer = (TableViewer) viewer;
            }
        }

        @Override
        public void update(ViewerCell cell) {
            super.update(cell);
            if (viewer != null) {
                int index = Arrays.asList(viewer.getTable().getItems()).indexOf(cell.getItem());
                cell.setText("" + (index + 1)); //$NON-NLS-1$
            }
        }
    }
}
