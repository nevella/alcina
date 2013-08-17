package cc.alcina.framework.entity.parallel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.entity.ResourceUtilities;

@RegistryLocation(registryPoint = ParallelTaskPool.class, implementationType = ImplementationType.SINGLETON)
public class ParallelTaskPool implements RegistrableService{
	private ExecutorService executorService;

	public ParallelTaskPool() {
		this.executorService = Executors.newFixedThreadPool(ResourceUtilities
				.getInteger(ParallelTaskPool.class, "size", 4));
	}

	public void appShutdown() {
		executorService.shutdown();
	}

	public ExecutorService getExecutorService() {
		return this.executorService;
	}
}
