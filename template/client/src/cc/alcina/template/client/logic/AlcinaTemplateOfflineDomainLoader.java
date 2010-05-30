package cc.alcina.template.client.logic;

import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.gears.client.OfflineDomainLoader;
import cc.alcina.template.client.AlcinaTemplateClient;
import cc.alcina.template.cs.csobjects.AlcinaTemplateObjects;
import cc.alcina.template.cs.csobjects.AlcinaTemplateObjectsSerializationHelper;

public class AlcinaTemplateOfflineDomainLoader extends OfflineDomainLoader {
	@Override
	public DomainModelHolder createDummyModel() {
		return new AlcinaTemplateObjects();
	}

	public ClientInstance beforeEventReplay() {
		AlcinaTemplateObjectsSerializationHelper h = TransformManager
				.get()
				.getObject(AlcinaTemplateObjectsSerializationHelper.class, 1, 0);
		AlcinaTemplateObjects co = h.postDeserialization();
		ClassRef.add(co.getClassRefs());
		ClientLayerLocator.get().getClientHandshakeHelper().registerDomainModel(
				co, h.getLoginState());
		ClientInstance clientInstance = h.getClientInstance();
		return clientInstance;
	}

	@Override
	public void afterEventReplay() {
		PermissionsManager.get().setLoginState(LoginState.LOGGED_IN);
		AlcinaTemplateClient.theApp.afterDomainModelRegistration();
	}
}
