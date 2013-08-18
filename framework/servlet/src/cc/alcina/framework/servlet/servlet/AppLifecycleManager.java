package cc.alcina.framework.servlet.servlet;

import java.io.IOException;
import java.util.List;

import cc.alcina.framework.common.client.log.TaggedLogger;
import cc.alcina.framework.common.client.log.TaggedLoggers;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.servlet.servlet.control.ControlServletModes;
import cc.alcina.framework.servlet.servlet.control.ControlServletStatus;
import cc.alcina.framework.servlet.servlet.control.WriterService;
import cc.alcina.framework.servlet.servlet.control.WriterServiceMode;

@RegistryLocation(registryPoint = AppLifecycleManager.class, implementationType = ImplementationType.SINGLETON)
public class AppLifecycleManager implements RegistrableService {
	private AppLifecycleServletBase lifecycleServlet;

	private ControlServletStatus status = new ControlServletStatus();

	private WriterServiceMode lastMode;

	public AppLifecycleManager() {
	}

	@Override
	public void appShutdown() {
	}

	public void ensureMemcacheUpdated() {
		// TODO - jctl
	}

	public AppLifecycleServletBase getLifecycleServlet() {
		return this.lifecycleServlet;
	}

	public ControlServletStatus getStatus() {
		return status;
	}

	public void loadModesFromConfigFile(String path) throws Exception {
		String props = ResourceUtilities.readFileToString(path);
		StringMap map = StringMap.fromPropertyString(props);
		status.setModes(ControlServletModes.fromProperties(map));
	}

	public void refreshProperties() {
		lifecycleServlet.loadCustomProperties();
	}

	public void refreshWriterServices() {
		WriterServiceMode mode = getStatus().getModes().getWriterServiceMode();
		if (mode == lastMode) {
			return;
		}
		String message = String.format("Writer service delta: %s -> %s",
				lastMode, mode);
		Registry.impl(TaggedLoggers.class).log(message,
				AppLifecycleManager.class, TaggedLogger.INFO);
		lastMode = mode;
		List<WriterService> services = Registry.impls(WriterService.class);
		for (WriterService service : services) {
			if (mode == WriterServiceMode.NOT_CONTROLLER) {
				service.shutdown();
			} else {
				service.startup();
			}
		}
	}

	public void setLifecycleServlet(AppLifecycleServletBase lifecycleServlet) {
		this.lifecycleServlet = lifecycleServlet;
		getStatus().setStartupTime(lifecycleServlet.getStartupTime());
		getStatus().setAppName(lifecycleServlet.getClass().getSimpleName());
	}

	public void setStatus(ControlServletStatus status) {
		this.status = status;
	}
}
