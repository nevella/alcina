package cc.alcina.framework.servlet.component.romcom.rebind;

import com.google.gwt.core.ext.typeinfo.JClassType;

import cc.alcina.framework.entity.gwt.reflection.ClientReflectionFilterPeer;

public class RemoteComponentReflectionFilterPeer
		implements ClientReflectionFilterPeer {
	/**
	 * Normally, use reachability. Returning non-null (generally fixing dev
	 * relection issues) is a temporary fix
	 */
	@Override
	public Boolean emitType(JClassType type, String moduleName) {
		if (type.getQualifiedSourceName()
				.startsWith("cc.alcina.framework.servlet.component")) {
			return true;
		}
		return ClientReflectionFilterPeer.super.emitType(type, moduleName);
	}
}