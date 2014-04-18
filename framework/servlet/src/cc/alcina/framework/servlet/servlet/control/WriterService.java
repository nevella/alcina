package cc.alcina.framework.servlet.servlet.control;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;

/**
 * A service which writes to the db (so requires startup/shutdown)
 * 
 * @author nick@alcina.cc
 * 
 *         All implementations must also have a per-class singleton location ::
 * 
 *         <pre>
 * @RegistryLocation(registryPoint=Bruce.class,implementationType=ImplementationType.SINGLETON)
 * public class Bruce
 * </pre>
 */
@RegistryLocation(registryPoint = WriterService.class)
public abstract class WriterService implements RegistrableService {
	public void onApplicationStartup() {
		startService();
	}

	public void onApplicationShutdown() {
		stopService();
	}

	public abstract void startService();

	public abstract void stopService();

	@Override
	public void appShutdown() {
		// this should be called earlier than the general service shutdown - for
		// the mo' at least
		// shutdown();
	}
}
