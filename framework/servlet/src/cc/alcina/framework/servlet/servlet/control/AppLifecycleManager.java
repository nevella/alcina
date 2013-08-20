package cc.alcina.framework.servlet.servlet.control;

import java.util.List;

import cc.alcina.framework.common.client.log.TaggedLogger;
import cc.alcina.framework.common.client.log.TaggedLoggers;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.servlet.servlet.AppLifecycleServletBase;
import cc.alcina.framework.servlet.servlet.control.ControlServletHandlers.ModeEnum;

@RegistryLocation(registryPoint = AppLifecycleManager.class, implementationType = ImplementationType.SINGLETON)
public class AppLifecycleManager implements RegistrableService {
	private AppLifecycleServletBase lifecycleServlet;

	private ControlServletState state = new ControlServletState();

	private ControlServletModes targetModes = new ControlServletModes();

	private String clusterRoleConfigFilePath;

	public AppLifecycleManager() {
	}

	@Override
	public void appShutdown() {
	}

	public void earlyShutdown() {
		targetModes = new ControlServletModes();
		refreshWriterServices();
	}

	public void ensureMemcacheUpdated() {
		// TODO - jctlå
	}

	public String getClusterRoleConfigFilePath() {
		return this.clusterRoleConfigFilePath;
	}

	public AppLifecycleServletBase getLifecycleServlet() {
		return this.lifecycleServlet;
	}

	public ControlServletState getState() {
		return state;
	}

	public boolean isWriter() {
		return state.getModes().getWriterMode() == WriterMode.CLUSTER_WRITER;
	}

	public void refreshClusterRoleFromConfigFile() throws Exception {
		String props = ResourceUtilities
				.readFileToString(clusterRoleConfigFilePath);
		StringMap map = StringMap.fromPropertyString(props);
		targetModes = ControlServletModes.fromProperties(map);
		state.setWriterHost(map.get("writerUrl"));
		state.setApiKey(map.get("apiKey"));
	}

	public void refreshProperties() {
		lifecycleServlet.refreshProperties();
	}

	public void refreshWriterServices() {
		WriterRelayMode[] nonWriterStates = { WriterRelayMode.REJECT,
				WriterRelayMode.RELAY, WriterRelayMode.PAUSE };
		ModeEnum.WRITER_SERVICE_MODE.getDeltaHandler(this).handleDelta(
				WriterServiceMode.CONTROLLER, WriterServiceMode.NOT_CONTROLLER);
		ModeEnum.WRITER_RELAY_MODE.getDeltaHandler(this).handleDeltas(
				WriterRelayMode.values(), nonWriterStates);
		ModeEnum.WRITER_MODE.getDeltaHandler(this).handleDelta(
				WriterMode.CLUSTER_WRITER, WriterMode.READ_ONLY);
		// midpoint
		ModeEnum.WRITER_MODE.getDeltaHandler(this).handleDelta(
				WriterMode.READ_ONLY, WriterMode.CLUSTER_WRITER);
		ModeEnum.WRITER_RELAY_MODE.getDeltaHandler(this).handleDeltas(
				nonWriterStates,
				new WriterRelayMode[] { WriterRelayMode.WRITE });
		ModeEnum.WRITER_SERVICE_MODE.getDeltaHandler(this).handleDelta(
				WriterServiceMode.NOT_CONTROLLER, WriterServiceMode.CONTROLLER);
	}

	void log(String message) {
		Registry.impl(TaggedLoggers.class).log(message,
				AppLifecycleManager.class, TaggedLogger.INFO);
	}

	public void setClusterRoleConfigFilePath(String configFilePath) {
		this.clusterRoleConfigFilePath = configFilePath;
	}

	public void setLifecycleServlet(AppLifecycleServletBase lifecycleServlet) {
		this.lifecycleServlet = lifecycleServlet;
		getState().setStartupTime(lifecycleServlet.getStartupTime());
		getState().setAppName(lifecycleServlet.getClass().getSimpleName());
	}

	public void setState(ControlServletState state) {
		this.state = state;
	}

	public ControlServletModes getTargetModes() {
		return this.targetModes;
	}
}
