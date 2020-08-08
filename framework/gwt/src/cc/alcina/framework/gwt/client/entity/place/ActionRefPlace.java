package cc.alcina.framework.gwt.client.entity.place;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef;
import cc.alcina.framework.gwt.client.dirndl.annotation.Ref;
import cc.alcina.framework.gwt.client.dirndl.annotation.Reference;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;

public class ActionRefPlace extends BasePlace {
	private Class<? extends ActionRef> ref;

	public Class<? extends ActionRef> getRef() {
		return this.ref;
	}

	public ActionRefPlace() {
	}

	public ActionRefPlace(Class<? extends ActionRef> ref) {
		this.ref = ref;
	}

	public static class ActionRefPlaceTokenizer
			extends BasePlaceTokenizer<ActionRefPlace> {
		@Override
		public Class<ActionRefPlace> getTokenizedClass() {
			return ActionRefPlace.class;
		}

		@Override
		protected ActionRefPlace getPlace0(String token) {
			ActionRefPlace place = new ActionRefPlace();
			if (parts.length == 1) {
				return place;
			}
			place.ref = ActionRef.forId(parts[1]);
			return place;
		}

		@Override
		protected void getToken0(ActionRefPlace place) {
			addTokenPart(Reference.id(place.ref));
		}
	}

	@Override
	public String toString() {
		Ref refRef = Reflections.classLookup().getAnnotationForClass(ref,
				Ref.class);
		if (refRef.displayName().length() > 0) {
			return refRef.displayName();
		}
		return CommonUtils.titleCase(refRef.value());
	}
}
