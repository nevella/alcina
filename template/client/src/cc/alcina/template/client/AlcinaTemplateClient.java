package cc.alcina.template.client;

import cc.alcina.framework.common.client.logic.StateChangeListener;
import cc.alcina.framework.gwt.client.ClientBaseWithLayout;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.ClientHandshakeHelper;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents;
import cc.alcina.template.client.widgets.LayoutManager;
import cc.alcina.template.cs.remote.AlcinaTemplateRemoteService;
import cc.alcina.template.cs.remote.AlcinaTemplateRemoteServiceAsync;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class AlcinaTemplateClient extends ClientBaseWithLayout implements
		EntryPoint {
	public static AlcinaTemplateClient theApp;

	private AlcinaTemplateRemoteServiceAsync alcinaTemplateRemoteService;

	private AlcinaTemplateConfiguration config = new AlcinaTemplateConfiguration();

	public void onModuleLoad() {
		theApp = this;
		initServices();
		GWT.setUncaughtExceptionHandler(ClientLayerLocator.get()
				.exceptionHandler());
		ClientHandshakeHelper loginHandshakeHelper = ClientLayerLocator.get()
				.getClientHandshakeHelper();
		loginHandshakeHelper.addStateChangeListener(hanshakeListener);
		loginHandshakeHelper.beginHandshake();
	}

	public void showMustBeLoggedInWarning() {
		ClientLayerLocator.get().notifications().showWarning(
				"You are not logged in");
	}

	public AlcinaTemplateRemoteServiceAsync getAppRemoteService() {
		return this.alcinaTemplateRemoteService;
	}

	private void setAlcinaTemplateRemoteService(
			AlcinaTemplateRemoteServiceAsync alcinaTemplateRemoteService) {
		this.alcinaTemplateRemoteService = alcinaTemplateRemoteService;
	}

	@Override
	protected boolean isLayoutInitialising() {
		return LayoutManager.get().isInitialising();
	}

	@Override
	protected boolean onWindowResized(int clientWidth, int clientHeight,
			boolean fromBrowser) {
		if (!super.onWindowResized(clientWidth, clientHeight, fromBrowser)) {
			return false;
		}
		LayoutManager.get().resize(clientWidth, clientHeight);
		return true;
	}

	@Override
	protected void redrawLayout() {
		Element statusVariable = Document.get().getElementById("loading");
		statusVariable.getStyle().setProperty("display", "none");
		setDisplayInitialised(true);
		Window.setMargin("0px");
		LayoutManager.get().redraw();
		Window.addCloseHandler(this);
		Window.addResizeHandler(this);
		Window.addWindowClosingHandler(this);
	}

	private StateChangeListener hanshakeListener = new StateChangeListener() {
		public void stateChanged(Object source, String newState) {
			if (newState
					.equals(ClientHandshakeHelper.STATE_AFTER_DOMAIN_MODEL_REGISTERED)) {
				registerClientObjectListeners(true);
			}
		}
	};

	private void registerClientObjectListeners(boolean register) {
	}

	void initServices() {
		setAlcinaTemplateRemoteService((AlcinaTemplateRemoteServiceAsync) GWT
				.create(AlcinaTemplateRemoteService.class));
		((ServiceDefTarget) getAppRemoteService())
				.setServiceEntryPoint("/alcinaTemplateService.do");
		ClientLayerLocator.get().registerCommonRemoteServiceAsync(
				getAppRemoteService());
		config.initServices();
		ClientLayerLocator.get().registerClientBase(this);
		LayoutEvents.get().addLayoutEventListener(this);
	}
}
