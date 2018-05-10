package cc.alcina.framework.servlet.knowns;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;

public abstract class KnownRoot extends KnownNode {
	public KnownRoot(String name) {
		super(null, name);
	}
}
