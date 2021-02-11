package cc.alcina.framework.gwt.client.entity.place;

import java.util.Optional;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionHandler;
import cc.alcina.framework.gwt.client.dirndl.annotation.ActionRef.ActionRefHandler;
import cc.alcina.framework.gwt.client.dirndl.annotation.Behaviour.TopicBehaviour;
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

	public Optional<ActionHandler> getActionHandler() {
		Optional<ActionHandler> handler = Registry.optional(ActionHandler.class,
				ref);
		if (handler.isPresent()) {
			return handler;
		}
		/*
		 * FIXME - dirndl1.1 - should annotation resolution be via context?
		 */
		return Optional
				.ofNullable(Reflections.classLookup().getAnnotationForClass(ref,
						ActionRefHandler.class))
				.map(ann -> Reflections.newInstance(ann.value()));
	}

	public Optional<TopicBehaviour> getActionTopic() {
		return Optional.ofNullable(Reflections.classLookup()
				.getAnnotationForClass(ref, TopicBehaviour.class));
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
