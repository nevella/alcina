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
	public abstract void startup();

	public abstract void shutdown();

	@Override
	public void appShutdown() {
		// this should be called earlier than the general service shutdown - for
		// the mo' at least
		// shutdown();
	}
}
