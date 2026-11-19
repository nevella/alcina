package cc.alcina.framework.gwt.client.logic.domain;

import com.google.gwt.user.client.ui.SuggestOracle.HasSuggestedObject;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.MvccAccess;
import cc.alcina.framework.common.client.logic.domain.MvccAccess.MvccAccessType;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.search.EntityCriterion;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringRepresentable.RepresentableToStringTransform.ToStringRepresentation;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestOracleResponseElement;

@Bean(PropertySource.FIELDS)
public class EntitySuggestion implements BoundSuggestOracleResponseElement,
		Comparable<EntitySuggestion>, HasSuggestedObject {
	public Entity entity;

	public String suggestionString;

	public transient EntityCriterion entityCriterion;

	public EntitySuggestion() {
	}

	public EntityCriterion toEntityCriterion() {
		return entityCriterion;
	}

	public EntitySuggestion(Entity entity) {
		this.entity = entity;
		if (entity instanceof HasDisplayName) {
			suggestionString = ((HasDisplayName) entity).displayName();
		} else {
			suggestionString = entity.toStringEntity();
		}
	}

	public EntitySuggestion(Entity entity, String suggestionString) {
		this.entity = entity;
		this.suggestionString = suggestionString;
	}

	public EntitySuggestion(EntityCriterion<?> entityCriterion) {
		this.entity = entityCriterion.getValue();
		this.entityCriterion = entityCriterion;
		this.suggestionString = entityCriterion.getDisplayText();
	}

	@Override
	public String toSuggestionString() {
		return suggestionString;
	}

	@Override
	public int compareTo(EntitySuggestion o) {
		return suggestionString.compareTo(o.suggestionString);
	}

	@Override
	public String toString() {
		return toSuggestionString();
	}

	@Registration({ ToStringRepresentation.class, EntitySuggestion.class })
	public static class ToStringRepresentationImpl
			extends Binding.AbstractContextSensitiveTransform<EntitySuggestion>
			implements ToStringRepresentation<EntitySuggestion> {
		@Override
		@MvccAccess(type = MvccAccessType.VERIFIED_CORRECT)
		public String apply(EntitySuggestion t) {
			return t.entity.toLocator().toRecoverableNumericString();
		}
	}

	@Override
	public Object provideSuggestedObject() {
		return entity;
	}
}