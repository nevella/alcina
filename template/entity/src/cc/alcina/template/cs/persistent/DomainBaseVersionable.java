package cc.alcina.template.cs.persistent;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.IVersionable;
import cc.alcina.framework.common.client.logic.reflection.DisplayInfo;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.VisualiserInfo;



@MappedSuperclass
public abstract class DomainBaseVersionable extends DomainBase implements IVersionable {
	Date lastModificationDate;

	AlcinaTemplateUser lastModificationUser;

	Date creationDate;

	AlcinaTemplateUser creationUser;
	public Date getCreationDate() {
		return this.creationDate;
	}

	@ManyToOne(fetch = FetchType.LAZY,targetEntity=AlcinaTemplateUser.class)
	@JoinColumn(name = "creation_user_id")
	public IUser getCreationUser() {
		return this.creationUser;
	}

	@VisualiserInfo( displayInfo = @DisplayInfo(name = "Last modified",orderingHint=999))
	@PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.ROOT))
	public Date getLastModificationDate() {
		return this.lastModificationDate;
	}

	@ManyToOne( fetch = FetchType.LAZY,targetEntity=AlcinaTemplateUser.class)
	@JoinColumn(name = "modification_user_id")
	public IUser getLastModificationUser() {
		return this.lastModificationUser;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public void setCreationUser(IUser creationUser) {
		this.creationUser = (AlcinaTemplateUser) creationUser;
	}

	public void setLastModificationDate(Date lastModificationDate) {
		this.lastModificationDate = lastModificationDate;
	}

	public void setLastModificationUser(IUser lastModificationUser) {
		this.lastModificationUser = (AlcinaTemplateUser) lastModificationUser;
	}
}
