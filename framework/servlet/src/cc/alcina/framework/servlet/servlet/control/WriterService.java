package cc.alcina.framework.servlet.servlet.control;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
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
public interface WriterService extends RegistrableService {
	void startup();

	void shutdown();
}
