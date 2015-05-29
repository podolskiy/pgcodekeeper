package ru.taximaxim.codekeeper.ui.views;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.core.viewers.EntityConnectionData;
import org.eclipse.zest.core.viewers.IEntityStyleProvider;
import org.eclipse.zest.core.viewers.IFigureProvider;

import cz.startnet.utils.pgdiff.schema.PgConstraint;
import cz.startnet.utils.pgdiff.schema.PgSchema;
import cz.startnet.utils.pgdiff.schema.PgStatement;

class DepcyGraphLabelProvider extends LabelProvider implements IFigureProvider, IEntityStyleProvider{
    
    private boolean isSource = true;
    
    private Color colorLBlue = new Color(Display.getDefault(), 216, 228, 248);
    private Color colorLGreen = new Color(Display.getDefault(), 204, 255, 204);
    private Color colorDBlue = new Color(Display.getDefault(), 1, 70, 122);
    
    private static final Color HIGHLIGHT_COLOR = ColorConstants.yellow;
    
    public DepcyGraphLabelProvider(boolean isSource) {
        this.isSource  = isSource;
    }
    
    public void setIsSource(boolean isSource) {
        this.isSource = isSource;
    }
    
    @Override
    public String getText(Object element) {
        if (element instanceof PgStatement){
            PgStatement st = (PgStatement) element;
            switch (st.getStatementType()) {
            case COLUMN:
                break;
            case CONSTRAINT:
                if (!((PgConstraint)st).getRefs().isEmpty()) {
                    return "FK " + st.getBareName(); //$NON-NLS-1$
                }
                return "CONSTR " + st.getBareName(); //$NON-NLS-1$
            case DATABASE:
                return "DB"; //$NON-NLS-1$
            case DOMAIN:
                return "DOM " + st.getBareName(); //$NON-NLS-1$
            case EXTENSION:
                return "EXT " + st.getBareName(); //$NON-NLS-1$
            case FUNCTION:
                return "FUNC " + st.getBareName(); //$NON-NLS-1$
            case INDEX:
                return "IDX " + st.getBareName(); //$NON-NLS-1$
            case SCHEMA:
                return st.getBareName();
            case SEQUENCE:
                return "SEQ " + st.getBareName(); //$NON-NLS-1$
            case TABLE:
                return "TBL " + st.getBareName(); //$NON-NLS-1$
            case TRIGGER:
                return "TRG " + st.getBareName(); //$NON-NLS-1$
            case TYPE:
                return "TYPE " + st.getBareName(); //$NON-NLS-1$
            case VIEW:
                return "VIEW " + st.getBareName(); //$NON-NLS-1$
            }
            return st.getClass() + " " + st.getBareName(); //$NON-NLS-1$
        }else if (element instanceof EntityConnectionData){
            return ""; //$NON-NLS-1$
        }else{
            return "error"; //$NON-NLS-1$
        }
    }

    @Override
    public IFigure getFigure(Object element) {
        return null;
    }

    @Override
    public Color getNodeHighlightColor(Object entity) {
        return HIGHLIGHT_COLOR;
    }

    @Override
    public Color getBorderColor(Object entity) {
        if (entity instanceof PgSchema){
            return ColorConstants.black;
        }else{
            return ColorConstants.lightGray;
        }
    }

    @Override
    public Color getBorderHighlightColor(Object entity) {
        return ColorConstants.blue;
    }

    @Override
    public int getBorderWidth(Object entity) {
        if (entity instanceof PgSchema){
            return 2;
        }else{
            return 1;
        }
    }

    @Override
    public Color getBackgroundColour(Object entity) {
        return isSource ? colorLBlue : colorLGreen;
    }

    @Override
    public Color getForegroundColour(Object entity) {
        if (entity instanceof PgSchema){
            return ColorConstants.black;
        }else{
            return colorDBlue;
        }
    }

    @Override
    public IFigure getTooltip(Object entity) {
        return null;
    }

    @Override
    public boolean fisheyeNode(Object entity) {
        return false;
    }
    
    @Override
    public void dispose() {
        colorLBlue.dispose();
        colorLGreen.dispose();
        colorDBlue.dispose();
        
        super.dispose();
    }
}
