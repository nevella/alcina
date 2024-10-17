package cc.alcina.framework.servlet.component.romcom.rebind;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;

import cc.alcina.framework.entity.gwt.reflection.ClientReflectionFilterPeer;

public class RemoteComponentReflectionFilterPeer
		implements ClientReflectionFilterPeer {
	/**
	 * Normally, use reachability. Returning non-null (generally fixing dev
	 * reflection issues) is a temporary fix
	 */
	@Override
	public Boolean emitType(JClassType type, String moduleName) {
		if (type.getQualifiedSourceName()
				.startsWith("cc.alcina.framework.servlet.component")) {
			return true;
		}
		if (type.getQualifiedSourceName().equals(
				"cc.alcina.framework.common.client.util.CollectionCreators.HashSetCreator")) {
			return true;
		}
		return ClientReflectionFilterPeer.super.emitType(type, moduleName);
	}

	@Override
	public boolean isWhitelistReflectable(JClassType type) {
		return type.getQualifiedSourceName()
				.matches("com.google.gwt.dom.client.Style([$.].+)?");
	}

	@Override
	public boolean isVisibleType(JType type) {
		// allow invoke on element + document bean methods
		if (type.getQualifiedSourceName()
				.equals("com.google.gwt.dom.client.Element")) {
			return true;
		}
		if (type.getQualifiedSourceName()
				.equals("com.google.gwt.dom.client.Document")) {
			return true;
		}
		// and style
		if (type.getQualifiedSourceName()
				.matches("com.google.gwt.dom.client.Style([$.].+)?")) {
			// and Style.Unit etc
			return true;
		}
		return ClientReflectionFilterPeer.super.isVisibleType(type);
	}
}