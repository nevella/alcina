package cc.alcina.template.cs.misc.search;

import java.beans.PropertyChangeListener;
import java.util.Date;

import javax.persistence.Transient;

import cc.alcina.framework.common.client.csobjects.SearchResult;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.CustomiserInfo;
import cc.alcina.framework.common.client.logic.reflection.DisplayInfo;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.VisualiserInfo;
import cc.alcina.framework.gwt.client.gwittir.customiser.ClassSimpleNameCustomiser;
import cc.alcina.framework.gwt.client.gwittir.customiser.DomainObjectIdRefCustomiser;
import cc.alcina.template.cs.persistent.AlcinaTemplateUser;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.beans.annotations.Introspectable;

//has no pcls, read-only on client 
@BeanInfo(displayNamePropertyName = "persistentId")
@Introspectable
public class DomainTransformEventInfo extends DomainTransformEvent implements
		SourcesPropertyChangeEvents, SearchResult {
	// used for returning persistent dtes
	private long id;

	// used for returning persistent dtes
	private long userId;

	private Date serverCommitDate;

	public void addPropertyChangeListener(PropertyChangeListener l) {
	}

	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener l) {
	}

	@VisualiserInfo(displayInfo = @DisplayInfo(name = "Id", orderingHint = 1))
	@Transient
	public long getId() {
		return id;
	}

	@Override
	@VisualiserInfo(displayInfo = @DisplayInfo(name = "New Value", orderingHint = 30))
	public String getNewStringValue() {
		return super.getNewStringValue();
	}

	@Override
	@VisualiserInfo(displayInfo = @DisplayInfo(name = "Object", orderingHint = 10))
	@CustomiserInfo(customiserClass = ClassSimpleNameCustomiser.class)
	public String getObjectClassName() {
		return super.getObjectClassName();
	}

	@Override
	@VisualiserInfo(displayInfo = @DisplayInfo(name = "Object id", orderingHint = 15))
	public long getObjectId() {
		return super.getObjectId();
	}

	public PropertyChangeListener[] getPropertyChangeListeners() {
		return null;
	}

	@Override
	@VisualiserInfo(displayInfo = @DisplayInfo(name = "Property", orderingHint = 20))
	public String getPropertyName() {
		return super.getPropertyName();
	}

	@VisualiserInfo(displayInfo = @DisplayInfo(name = "Date", orderingHint = 35))
	public Date getServerCommitDate() {
		return serverCommitDate;
	}

	@Override
	@VisualiserInfo(displayInfo = @DisplayInfo(name = "Transform", orderingHint = 25))
	public TransformType getTransformType() {
		return super.getTransformType();
	}

	@Transient
	@VisualiserInfo(displayInfo = @DisplayInfo(name = "User", orderingHint = 5))
	@CustomiserInfo(customiserClass = DomainObjectIdRefCustomiser.class, parameters = { @NamedParameter(name = DomainObjectIdRefCustomiser.TARGET_CLASS, classValue = AlcinaTemplateUser.class) })
	public long getUserId() {
		return userId;
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
	}

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
}
