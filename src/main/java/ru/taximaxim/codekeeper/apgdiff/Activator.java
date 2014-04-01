package ru.taximaxim.codekeeper.apgdiff;

import org.eclipse.equinox.log.ExtendedLogService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

    private static ServiceTracker<ExtendedLogService, ExtendedLogService> logTracker;
    
    public static ServiceTracker<ExtendedLogService, ExtendedLogService> getLogTracker() {
        return logTracker;
    }
    
    @Override
    public void start(BundleContext context) throws Exception {
        logTracker = new ServiceTracker<>(context, ExtendedLogService.class, null);
        logTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if(logTracker != null) {
            logTracker.close();
        }
    }
}
