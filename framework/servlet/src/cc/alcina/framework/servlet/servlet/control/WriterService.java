package cc.alcina.framework.servlet.servlet.control;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.LifecycleService;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;

/**
 * 
 * 
 * @author nick@alcina.cc
 * 
 *         All implementations must also have a per-class singleton location ::
 * 
 *         <pre>
 * &#64;RegistryLocation(registryPoint=Bruce.class,implementationType=ImplementationType.SINGLETON)
 * public class Bruce
 *         </pre>
 */
@RegistryLocation(registryPoint = WriterService.class)
public abstract class WriterService
		implements RegistrableService, LifecycleService {
	@Override
	public void appShutdown() {
		// this should be called earlier than the general service shutdown - for
		// the mo' at least
		// shutdown();
	}

	public void onApplicationShutdown() {
		stopService();
	}

	public void onApplicationStartup() {
		startService();
	}
}
