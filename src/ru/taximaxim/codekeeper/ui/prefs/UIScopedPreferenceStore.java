package ru.taximaxim.codekeeper.ui.prefs;

import org.eclipse.core.runtime.preferences.InstanceScope;

import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.copiedclasses.ScopedPreferenceStore;

/**
 * Extends {@link ScopedPreferenceStore} with app's default values.
 * 
 * @author Alexander Levsha
 */
public class UIScopedPreferenceStore extends ScopedPreferenceStore {
    
    public UIScopedPreferenceStore() {
        super(InstanceScope.INSTANCE, UIConsts.PLUGIN_ID);
        setDefaultValues();
    }
    
    private void setDefaultValues() {
        setDefault(UIConsts.PREF_SVN_EXE_PATH, "svn");
        setDefault(UIConsts.PREF_GIT_EXE_PATH, "git");
        setDefault(UIConsts.PREF_PGDUMP_EXE_PATH, "pg_dump");
        setDefault(UIConsts.PREF_DB_STORE, "default\t\t\t\t\t0");
    }
}
