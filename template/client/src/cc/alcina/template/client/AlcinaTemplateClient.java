package cc.alcina.template.client;

import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.template.client.handshake.AlcinaTemplateHandshake;

import com.google.gwt.core.client.EntryPoint;

public class AlcinaTemplateClient extends ClientBase implements
		EntryPoint {
	public void onModuleLoad() {
		Registry.get().registerBootstrapServices(ClientReflector.get());
		Registry.registerSingleton(ClientBase.class,this);
		Registry.impl(AlcinaTemplateHandshake.class).run();
	}
}
