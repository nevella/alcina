package cc.alcina.framework.common.client.logic.domaintransform;

import java.beans.PropertyChangeListener;
import java.util.Date;

import javax.persistence.Transient;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.csobjects.SearchResult;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.gwt.client.gwittir.customiser.ClassSimpleNameCustomiser;
import cc.alcina.framework.gwt.client.gwittir.customiser.Customiser;
import cc.alcina.framework.gwt.client.gwittir.customiser.ExpandableLabelCustomiser;
import cc.alcina.framework.gwt.client.gwittir.customiser.RenderedLabelCustomiser;
import cc.alcina.framework.gwt.client.gwittir.renderer.ClassSimpleNameRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.RenderingLabel;

// has no pcls, read-only on client
@Bean
public class DomainTransformEventView extends DomainTransformEvent
		implements SourcesPropertyChangeEvents, SearchResult {
	private long id;

	// used for returning persistent dtes
	// used for returning persistent dtes
	private long userId;

	private String userName;

	private Date serverCommitDate;

	@Override
	public void addPropertyChangeListener(PropertyChangeListener l) {
	}

	@Override
	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener l) {
	}

	@Override
	public void firePropertyChange(String propertyName, Object oldValue,
			Object newValue) {
		// NOOP
	}

	@Display(orderingHint = 1)
	@Transient
	public long getId() {
		return id;
	}

	@Override
	@Display(name = "New Value", orderingHint = 30)
	@Custom(
		customiserClass = ExpandableLabelCustomiser.class,
		parameters = {
				@NamedParameter(
					name = ExpandableLabelCustomiser.SHOW_AS_POPUP,
					booleanValue = true),
				@NamedParameter(
					name = ExpandableLabelCustomiser.MAX_WIDTH,
					intValue = 30) })
	public String getNewStringValue() {
		return super.getNewStringValue();
	}

	@Override
	@Display(name = "Object", orderingHint = 10)
	@Custom(customiserClass = ClassSimpleNameCustomiser.class)
	public String getObjectClassName() {
		return super.getObjectClassName();
	}

	@Override
	@Display(orderingHint = 15)
	public long getObjectId() {
		return super.getObjectId();
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
	@Custom(
		customiserClass = RenderedLabelCustomiser.class,
		parameters = { @NamedParameter(
			name = RenderedLabelCustomiser.RENDERER_CLASS,
			classValue = ShortTransformTypeRenderer.class) })
	public TransformType getTransformType() {
		return super.getTransformType();
	}

	@Transient
	@Display(name = "User Id", orderingHint = 5)
	public // classValue = IUser.class) })
	long getUserId() {
		return userId;
	}

	@Transient
	@Display(name = "User name", orderingHint = 6)
	public String getUserName() {
		return this.userName;
	}

	@Override
	@Display(name = "Reference", orderingHint = 31)
	@Custom(customiserClass = LongBlankZeroCustomiser.class)
	public long getValueId() {
		return super.getValueId();
	}

	@Override
	public PropertyChangeListener[] propertyChangeListeners() {
		return null;
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

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@Reflected
	public static class LongBlankZeroCustomiser implements Customiser {
		public static final BoundWidgetProvider INSTANCE = new BoundWidgetProvider() {
			@Override
			public BoundWidget get() {
				RenderingLabel label = new RenderingLabel();
				label.setWordWrap(false);
				label.setRenderer(ClassSimpleNameRenderer.INSTANCE);
				return label;
			}
		};

		@Override
		public BoundWidgetProvider getProvider(boolean editable,
				Class objectClass, boolean multiple, Custom info) {
			return INSTANCE;
		}
	}

	@Reflected
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
