package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.Date;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.DomainTransformPersistable;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;

@MappedSuperclass
@DomainTransformPersistable
@ObjectPermissions(create = @Permission(access = AccessLevel.ROOT), read = @Permission(access = AccessLevel.ROOT), write = @Permission(access = AccessLevel.ROOT), delete = @Permission(access = AccessLevel.ROOT))
public abstract class LoginAttempt extends Entity<LoginAttempt> {
	private String userNameLowerCase;

	private Date date;

	private boolean success;

	private String ipAddress;

	private String userAgent;

	public LoginAttempt() {
		int debug = 3;
	}

	public Date getDate() {
		return this.date;
	}

	public String getIpAddress() {
		return this.ipAddress;
	}

	@Lob
	@Transient
	public String getUserAgent() {
		return this.userAgent;
	}

	public String getUserNameLowerCase() {
		return this.userNameLowerCase;
	}

	public boolean isSuccess() {
		return this.success;
	}

	public void setDate(Date date) {
		Date old_date = this.date;
		this.date = date;
		propertyChangeSupport().firePropertyChange("date", old_date, date);
	}

	public void setIpAddress(String ipAddress) {
		String old_ipAddress = this.ipAddress;
		this.ipAddress = ipAddress;
		propertyChangeSupport().firePropertyChange("ipAddress", old_ipAddress,
				ipAddress);
	}

	public void setSuccess(boolean success) {
		boolean old_success = this.success;
		this.success = success;
		propertyChangeSupport().firePropertyChange("success", old_success,
				success);
	}

	public void setUserAgent(String userAgent) {
		String old_userAgent = this.userAgent;
		this.userAgent = userAgent;
		propertyChangeSupport().firePropertyChange("userAgent", old_userAgent,
				userAgent);
	}

	public void setUserNameLowerCase(String userNameLowerCase) {
		String old_userNameLowerCase = this.userNameLowerCase;
		this.userNameLowerCase = userNameLowerCase;
		propertyChangeSupport().firePropertyChange("userNameLowerCase",
				old_userNameLowerCase, userNameLowerCase);
	}
}
