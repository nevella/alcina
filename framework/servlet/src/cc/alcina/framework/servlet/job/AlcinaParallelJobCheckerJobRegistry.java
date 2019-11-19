package cc.alcina.framework.servlet.job;

import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.entity.util.AlcinaParallel.AlcinaParallelJobChecker;

@RegistryLocation(registryPoint = AlcinaParallelJobChecker.class, implementationType = ImplementationType.INSTANCE, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
public class AlcinaParallelJobCheckerJobRegistry
        extends AlcinaParallelJobChecker {
    private JobTracker jobTracker;

    public AlcinaParallelJobCheckerJobRegistry() {
        jobTracker = JobRegistry.get().getContextTracker();
    }

    @Override
    public boolean isCancelled() {
        return jobTracker != null && jobTracker.isCancelled();
    }
}