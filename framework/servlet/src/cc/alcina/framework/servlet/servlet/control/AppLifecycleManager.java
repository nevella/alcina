package cc.alcina.framework.servlet.servlet.control;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.user.server.rpc.RPCRequest;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.servlet.servlet.AppLifecycleServletBase;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;
import cc.alcina.framework.servlet.servlet.control.ControlServletHandlers.ModeEnum;

@RegistryLocation(registryPoint = AppLifecycleManager.class, implementationType = ImplementationType.SINGLETON)
public class AppLifecycleManager implements RegistrableService {
	public static final String TOPIC_APP_CONFIGURATION_RELOADED = AppLifecycleManager.class
			.getName() + ".TOPIC_APP_CONFIGURATION_RELOADED";

	public static void notifyAppConfigurationReloaded(Void v) {
		GlobalTopicPublisher.get()
				.publishTopic(TOPIC_APP_CONFIGURATION_RELOADED, v);
	}

	public static void notifyAppConfigurationReloadedDelta(
			TopicListener<Void> listener, boolean add) {
		GlobalTopicPublisher.get()
				.listenerDelta(TOPIC_APP_CONFIGURATION_RELOADED, listener, add);
	}

	private AppLifecycleServletBase lifecycleServlet;

	private ControlServletState state = ControlServletState.standaloneModes();

	private ControlServletModes targetModes = new ControlServletModes();

	private boolean clusterMember;

	final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass());

	public AppLifecycleManager() {
	}

	@Override
	public void appShutdown() {
	}

	public boolean canWriteOrRelay() {
		return state.getModes().getWriterRelayMode() != WriterRelayMode.REJECT;
	}

	public void earlyShutdown() {
		targetModes = new ControlServletModes();
		refreshWriterServices();
	}

	public AppLifecycleServletBase getLifecycleServlet() {
		return this.lifecycleServlet;
	}

	public ControlServletState getState() {
		return state;
	}

	public ControlServletModes getTargetModes() {
		return this.targetModes;
	}

	public void initWriterServices() {
		for (ModeEnum e : ModeEnum.values()) {
			e.getDeltaHandler(this).init();
		}
	}

	public boolean isClusterMember() {
		return this.clusterMember;
	}

	public boolean isWriter() {
		return state.getModes().getWriterMode() == WriterMode.CLUSTER_WRITER;
	}

	public String proxy(RPCRequest rpcRequest,
			CommonRemoteServiceServlet remoteServlet) {
		return new AppWriterProxy().proxy(this, rpcRequest, remoteServlet);
	}

	public void refreshClusterRoleFromProperties() {
		targetModes = ControlServletModes.fromProperties();
		state.setWriterHost(ResourceUtilities.get("writerHost"));
		state.setApiKey(ResourceUtilities.get("apiKey"));
	}

	public void refreshProperties() throws Exception {
		lifecycleServlet.refreshProperties();
		ResourceUtilities.loadSystemPropertiesFromCustomProperties();
		refreshClusterRoleFromProperties();
		refreshWriterServices();
		notifyAppConfigurationReloaded(null);
		EntityLayerLogging.setLogLevelsFromCustomProperties();
	}

	public void refreshWriterServices() {
		WriterRelayMode[] nonWriterStates = { WriterRelayMode.REJECT,
				WriterRelayMode.RELAY, WriterRelayMode.PAUSE };
		ModeEnum.WRITER_SERVICE_MODE.getDeltaHandler(this).handleDelta(
				WriterServiceMode.CONTROLLER, WriterServiceMode.NOT_CONTROLLER);
		ModeEnum.WRITER_RELAY_MODE.getDeltaHandler(this)
				.handleDeltas(WriterRelayMode.values(), nonWriterStates);
		ModeEnum.WRITER_MODE.getDeltaHandler(this)
				.handleDelta(WriterMode.CLUSTER_WRITER, WriterMode.READ_ONLY);
		// midpoint
		ModeEnum.WRITER_MODE.getDeltaHandler(this)
				.handleDelta(WriterMode.READ_ONLY, WriterMode.CLUSTER_WRITER);
		ModeEnum.WRITER_RELAY_MODE.getDeltaHandler(this).handleDeltas(
				nonWriterStates,
				new WriterRelayMode[] { WriterRelayMode.WRITE });
		ModeEnum.WRITER_SERVICE_MODE.getDeltaHandler(this).handleDelta(
				WriterServiceMode.NOT_CONTROLLER, WriterServiceMode.CONTROLLER);
	}

	public void setClusterMember(boolean clusterMember) {
		this.clusterMember = clusterMember;
		if (clusterMember) {
			setState(ControlServletState.memberModes());
		}
	}

	public void setLifecycleServlet(AppLifecycleServletBase lifecycleServlet) {
		this.lifecycleServlet = lifecycleServlet;
		getState().setStartupTime(lifecycleServlet.getStartupTime());
		getState().setAppName(lifecycleServlet.getClass().getSimpleName());
	}

	public void setState(ControlServletState state) {
		this.state = state;
	}

	void debug(String message) {
		logger.debug(message);
	}
}
