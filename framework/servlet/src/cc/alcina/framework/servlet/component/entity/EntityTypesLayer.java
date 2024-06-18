package cc.alcina.framework.servlet.component.entity;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.entity.ClassUtil;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.servlet.component.entity.RootLayer.DomainGraphSelection;
import cc.alcina.framework.servlet.component.traversal.SelectionTableArea;

class EntityTypesLayer extends Layer<DomainGraphSelection> {
	@Override
	public void process(DomainGraphSelection selection) throws Exception {
		Registry.query(Entity.class).registrations()
				.filter(c -> DomainStore.stores().storeFor(c) != null)
				.sorted(new ClassUtil.SimpleNameComparator())
				.map(c -> new TypeSelection(selection, c))
				.forEach(this::select);
	}

	public static class TypeSelection
			extends AbstractSelection<Class<? extends Entity>>
			implements SelectionTableArea.HasTableRepresentation.Children {
		public TypeSelection(Selection parent, Class<? extends Entity> value) {
			super(parent, value, NestedName.get(value).toLowerCase());
		}
	}
}
