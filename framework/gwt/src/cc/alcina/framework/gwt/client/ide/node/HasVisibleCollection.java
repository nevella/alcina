package cc.alcina.framework.gwt.client.ide.node;

import java.util.Collection;

public interface HasVisibleCollection {
	Class getCollectionMemberClass();

	Collection getVisibleCollection();
}
