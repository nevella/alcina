package cc.alcina.template.client.handshake;

import cc.alcina.framework.common.client.csobjects.LoadObjectsRequest;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.logic.handshake.objectdata.LoadObjectsFromRemotePlayer;
import cc.alcina.template.cs.remote.AlcinaTemplateRemoteServiceAsync;

public class AlcinaTemplateLoadObjectsFromRemotePlayer extends
		LoadObjectsFromRemotePlayer {
	@Override
	protected void loadObjects(LoadObjectsRequest request) {
		Registry.impl(AlcinaTemplateRemoteServiceAsync.class).loadInitial(request, this);
	}
}
