package cc.alcina.template.client.handshake;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;
import cc.alcina.framework.gwt.client.logic.handshake.InitLoaderUiPlayer;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;
import cc.alcina.template.client.widgets.AlcinaTemplateLayoutManager;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

@RegistryLocation(registryPoint = InitLoaderUiPlayer.class, implementationType = ImplementationType.SINGLETON)
public class AlcinaTemplateInitLoaderUiPlayerDesktop extends InitLoaderUiPlayer {
	public AlcinaTemplateInitLoaderUiPlayerDesktop() {
	}

	@Override
	public void run() {
		AlcinaTemplateLayoutManager.get();
		Element statusVariable = Document.get().getElementById(
				"loading-status-variable");
		statusVariable.setClassName("status-2");
		ModalNotifier modalNotifier = Registry
				.impl(HandshakeConsortModel.class).ensureLoadObjectsNotifier(
						"Starting up");
		modalNotifier.modalOn();
		modalNotifier.setMasking(false);
	}
}