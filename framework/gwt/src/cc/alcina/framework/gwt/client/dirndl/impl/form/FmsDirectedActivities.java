package cc.alcina.framework.gwt.client.dirndl.impl.form;

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Permission.SimplePermissions;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedBindableSearchActivity;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedCategoriesActivity;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.DirectedCategoriesActivityTransformer;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.DirectedEntitySearchActivityTransformer;
import cc.alcina.framework.gwt.client.place.CategoryNamePlace;

public class FmsDirectedActivities {
	public static class DirectedCategoriesActivityTransformerFms
			extends DirectedCategoriesActivityTransformer {
		@Override
		protected boolean isPermitted(CategoryNamePlace place) {
			return PermissionsManager.get().isPermittedClass(place,
					SimplePermissions.getPermission(AccessLevel.ADMIN));
		}
	}

	public static class FmsDirectedCategoriesActivityTransformer extends
			AbstractContextSensitiveModelTransform<DirectedCategoriesActivity, TableModel> {
		@Override
		public TableModel apply(DirectedCategoriesActivity activity) {
			TableModel tableModel = new DirectedCategoriesActivityTransformerFms()
					.withContextNode(node).apply(activity);
			return tableModel;
		}
	}

	public static class FmsDirectedEntitySearchActivityTransformer extends
			AbstractContextSensitiveModelTransform<DirectedBindableSearchActivity, DirectedEntitySearchActivityTransformer.TableContainer> {
		@Override
		public DirectedEntitySearchActivityTransformer.TableContainer
				apply(DirectedBindableSearchActivity multipleActivity) {
			return new DirectedEntitySearchActivityTransformer()
					.withContextNode(node).apply(multipleActivity);
		}
	}
}
