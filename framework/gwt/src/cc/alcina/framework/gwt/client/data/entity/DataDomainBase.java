package cc.alcina.framework.gwt.client.data.entity;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import com.totsp.gwittir.client.beans.annotations.Introspectable;

import cc.alcina.framework.common.client.entity.VersioningEntityListener;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.IVersionable;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;

@Introspectable
@ClientInstantiable
@ObjectPermissions(create = @Permission(access = AccessLevel.ADMIN), read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.ADMIN), delete = @Permission(access = AccessLevel.ADMIN))
@MappedSuperclass
@javax.persistence.EntityListeners(VersioningEntityListener.class)
@RegistryLocation(registryPoint = DataDomainBase.class)
public abstract class DataDomainBase<T extends DataDomainBase> extends Entity<T>
		implements IVersionable, DataApiDecorators {
	public static final String CONTEXT_FIRE_CREATION_DATE_EVENTS = DataDomainBase.class
			.getName() + ".CONTEXT_FIRE_CREATION_DATE_EVENTS";

	protected long id = 0;

	private Date lastModificationDate;

	private Date creationDate;

	public DataDomainBase(boolean exDomain) {
	}

	protected DataDomainBase() {
	}

	@Override
	@Display(name = "Creation date", displayMask = Display.DISPLAY_RO_PROPERTY)
	public Date getCreationDate() {
		return this.creationDate;
	}

	@Override
	@XmlTransient
	@Transient
	public IUser getCreationUser() {
		return null;
	}

	@Override
	@Display(name = "Last modified", orderingHint = 999)
	@PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.ROOT))
	public Date getLastModificationDate() {
		return this.lastModificationDate;
	}

	@Override
	@XmlTransient
	@Transient
	public IUser getLastModificationUser() {
		return null;
	}

	public boolean provideWasCreatedBefore(int i, TimeUnit unit) {
		long time = getCreationDate() == null && Ax.isTest()
				? System.currentTimeMillis()
				: getCreationDate().getTime();
		return System.currentTimeMillis() - unit.toMillis(i) > time;
	}

	@Override
	public void setCreationDate(Date creationDate) {
		if (LooseContext.is(CONTEXT_FIRE_CREATION_DATE_EVENTS)) {
			Date old_creationDate = this.creationDate;
			this.creationDate = creationDate;
			propertyChangeSupport().firePropertyChange("creationDate",
					old_creationDate, creationDate);
		} else {
			this.creationDate = creationDate;
		}
	}

	@Override
	public void setCreationUser(IUser creationUser) {
		// ignore
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	@Override
	public void setLastModificationDate(Date lastModificationDate) {
		this.lastModificationDate = lastModificationDate;
	}

	@Override
	public void setLastModificationUser(IUser lastModificationUser) {
		// ignore
	}

	@Override
	public String toString() {
		return new EntityLocator(this).toString();
	}
}
