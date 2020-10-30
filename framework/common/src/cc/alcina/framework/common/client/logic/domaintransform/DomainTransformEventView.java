package cc.alcina.framework.common.client.logic.domaintransform;

import java.beans.PropertyChangeListener;
import java.util.Date;

import javax.persistence.Transient;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.beans.annotations.Introspectable;
import com.totsp.gwittir.client.ui.Renderer;

import cc.alcina.framework.common.client.csobjects.SearchResult;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.gwittir.customiser.ClassSimpleNameCustomiser;
import cc.alcina.framework.gwt.client.gwittir.customiser.ExpandableLabelCustomiser;
import cc.alcina.framework.gwt.client.gwittir.customiser.RenderedLabelCustomiser;

// has no pcls, read-only on client
@Bean(displayNamePropertyName = "persistentId")
@Introspectable
public class DomainTransformEventView extends DomainTransformEvent
		implements SourcesPropertyChangeEvents, SearchResult {
	private long id;
	// used for returning persistent dtes

	// used for returning persistent dtes
	private long userId;

	private Date serverCommitDate;

	@Override
	public void addPropertyChangeListener(PropertyChangeListener l) {
	}

	@Override
	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener l) {
	}

	@Display(name = "Id", orderingHint = 1)
	@Transient
	public long getId() {
		return id;
	}

	@Override
	@Display(name = "New Value", orderingHint = 30)
	@Custom(customiserClass = ExpandableLabelCustomiser.class, parameters = {
			@NamedParameter(name = ExpandableLabelCustomiser.SHOW_AS_POPUP, booleanValue = true),
			@NamedParameter(name = ExpandableLabelCustomiser.MAX_WIDTH, intValue = 30) })
	public String getNewStringValue() {
		if (getValueId() != 0 || getValueLocalId() != 0) {
			return Ax.format("id:%s/%s", getValueId(), getValueLocalId());
		} else {
			return super.getNewStringValue();
		}
	}

	@Override
	@Display(name = "Object", orderingHint = 10)
	@Custom(customiserClass = ClassSimpleNameCustomiser.class)
	public String getObjectClassName() {
		return super.getObjectClassName();
	}

	@Override
	@Display(name = "Object id", orderingHint = 15)
	public long getObjectId() {
		return super.getObjectId();
	}

	@Override
	public PropertyChangeListener[] getPropertyChangeListeners() {
		return null;
	}

	@Override
	@Display(name = "Property", orderingHint = 20)
	public String getPropertyName() {
		return super.getPropertyName();
	}

	@Display(name = "Date", orderingHint = 35)
	public Date getServerCommitDate() {
		return serverCommitDate;
	}

	@Override
	@Display(name = "Transform", orderingHint = 25, styleName = "nowrap")
	@Custom(customiserClass = RenderedLabelCustomiser.class, parameters = {
			@NamedParameter(name = RenderedLabelCustomiser.RENDERER_CLASS, classValue = ShortTransformTypeRenderer.class) })
	public TransformType getTransformType() {
		return super.getTransformType();
	}

	@Transient
	@Display(name = "User", orderingHint = 5)
	/*
	 * FIXME - directedlayout.2 - this sort of customisation should be in code,
	 * not annotation
	 */
	// @Custom(customiserClass = DomainObjectIdRefCustomiser.class, parameters =
	// {
	// @NamedParameter(name = DomainObjectIdRefCustomiser.TARGET_CLASS,
	// classValue = IUser.class) })
	public long getUserId() {
		return userId;
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener l) {
	}

	@Override
	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener l) {
	}

	public void setId(long persistentId) {
		this.id = persistentId;
	}

	public void setServerCommitDate(Date serverCommitDate) {
		this.serverCommitDate = serverCommitDate;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	@ClientInstantiable
	public static class ShortTransformTypeRenderer
			implements Renderer<TransformType, String> {
		@Override
		public String render(TransformType o) {
			if (o == null) {
				return "(null)";
			}
			switch (o) {
			case ADD_REF_TO_COLLECTION:
				return "add-ref";
			case CHANGE_PROPERTY_REF:
				return "change-ref";
			case CHANGE_PROPERTY_SIMPLE_VALUE:
				return "change-value";
			case CREATE_OBJECT:
				return "create";
			case DELETE_OBJECT:
				return "delete";
			case NULL_PROPERTY_REF:
				return "set-null-ref";
			case REMOVE_REF_FROM_COLLECTION:
				return "remove-ref";
			}
			return null;
		}
	}
}
