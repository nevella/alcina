/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.gwt.client.entity;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.UserPropertyPersistable;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registrations;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.logic.reflection.misc.PerUserProperties;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.TimezoneData;
import cc.alcina.framework.gwt.client.gwittir.customiser.TextAreaCustomiser;

@Bean
@Display(name = "Developer")
@XmlRootElement
@Registrations({ @Registration(JaxbContextRegistration.class),
		@Registration(PerUserProperties.class) })
public class GeneralProperties extends Bindable
		implements UserPropertyPersistable, TimezoneData.Provider {
	public static final transient int DEFAULT_FILTER_DELAY = 500;

	public static final transient String PROPERTY_TRANSIENT_CSS = "transientCss";

	public static final transient String PROPERTY_PERSISTENT_CSS = "persistentCss";

	public static GeneralProperties get() {
		return Registry.impl(Holder.class).getInstance();
	}

	private UserPropertyPersistable.Support userPropertySupport;

	private boolean autoSave;

	private int filterDelayMs = DEFAULT_FILTER_DELAY;

	private String transientCss = "";

	private String persistentCss = "";

	private String clientProperties;

	private boolean allowAdminInvalidObjectWrite = true;

	private TimezoneData timezoneData;

	public GeneralProperties() {
	}

	@Display(
		helpText = "User configuration flags (xxx=yyy, newline separated)",
		name = "Configuration")
	@PropertyPermissions(
		read = @Permission(access = AccessLevel.EVERYONE),
		write = @Permission(access = AccessLevel.ADMIN))
	@Custom(customiserClass = TextAreaCustomiser.class)
	public String getClientProperties() {
		return this.clientProperties;
	}

	@Display(name = "ui.filterComponentActuationDelay")
	@PropertyPermissions(
		read = @Permission(access = AccessLevel.EVERYONE),
		write = @Permission(access = AccessLevel.DEVELOPER))
	public int getFilterDelayMs() {
		return filterDelayMs;
	}

	@Display(
		helpText = "CSS which will be saved on the server, and reapplied each time you log in",
		name = "designer.persistentCss")
	@PropertyPermissions(
		read = @Permission(access = AccessLevel.EVERYONE),
		write = @Permission(access = AccessLevel.DEVELOPER))
	@Custom(customiserClass = TextAreaCustomiser.class)
	public String getPersistentCss() {
		return this.persistentCss;
	}

	@Override
	public TimezoneData getTimezoneData() {
		return this.timezoneData;
	}

	@Display(
		helpText = "CSS which will be applied in this session, but not saved on the server",
		name = "designer.transientCss",
		focus = true,
		styleName = "transientCss")
	@PropertyPermissions(
		read = @Permission(access = AccessLevel.EVERYONE),
		write = @Permission(access = AccessLevel.EVERYONE))
	@Custom(customiserClass = TextAreaCustomiser.class)
	@XmlTransient
	public String getTransientCss() {
		return this.transientCss;
	}

	@Override
	/*
	 * Serialize when sending to client, otherwise not
	 */
	@AlcinaTransient(unless = AlcinaTransient.TransienceContext.CLIENT)
	@XmlTransient
	public UserPropertyPersistable.Support getUserPropertySupport() {
		if (this.userPropertySupport != null) {
			this.userPropertySupport.ensureListeners();
		}
		return this.userPropertySupport;
	}

	@Display(name = "admin.allowAdminInvalidObjectWrite")
	@PropertyPermissions(
		read = @Permission(access = AccessLevel.EVERYONE),
		write = @Permission(access = AccessLevel.DEVELOPER))
	public boolean isAllowAdminInvalidObjectWrite() {
		return allowAdminInvalidObjectWrite;
	}

	@Display(name = "domain.autoSaveChanges")
	@PropertyPermissions(
		read = @Permission(access = AccessLevel.EVERYONE),
		write = @Permission(access = AccessLevel.DEVELOPER))
	public boolean isAutoSave() {
		return autoSave;
	}

	public void setAllowAdminInvalidObjectWrite(
			boolean allowAdminInvalidObjectWrite) {
		boolean old_allowAdminInvalidObjectWrite = this.allowAdminInvalidObjectWrite;
		this.allowAdminInvalidObjectWrite = allowAdminInvalidObjectWrite;
		propertyChangeSupport().firePropertyChange(
				"allowAdminInvalidObjectWrite",
				old_allowAdminInvalidObjectWrite, allowAdminInvalidObjectWrite);
	}

	public void setAutoSave(boolean autoSave) {
		boolean old_autoSave = this.autoSave;
		this.autoSave = autoSave;
		propertyChangeSupport().firePropertyChange("autoSave", old_autoSave,
				autoSave);
	}

	public void setClientProperties(String clientProperties) {
		String old_clientProperties = this.clientProperties;
		this.clientProperties = clientProperties;
		propertyChangeSupport().firePropertyChange("clientProperties",
				old_clientProperties, clientProperties);
	}

	public void setFilterDelayMs(int filterDelayMs) {
		int old_filterDelayMs = this.filterDelayMs;
		this.filterDelayMs = filterDelayMs;
		propertyChangeSupport().firePropertyChange("filterDelayMs",
				old_filterDelayMs, filterDelayMs);
	}

	public void setPersistentCss(String persistentCss) {
		String old_persistentCss = this.persistentCss;
		this.persistentCss = persistentCss;
		propertyChangeSupport().firePropertyChange("persistentCss",
				old_persistentCss, persistentCss);
	}

	public void setTimezoneData(TimezoneData timezoneData) {
		this.timezoneData = timezoneData;
	}

	public void setTransientCss(String transientCss) {
		String old_transientCss = this.transientCss;
		this.transientCss = transientCss;
		propertyChangeSupport().firePropertyChange("transientCss",
				old_transientCss, transientCss);
	}

	@Override
	public void setUserPropertySupport(
			UserPropertyPersistable.Support userPropertySupport) {
		this.userPropertySupport = userPropertySupport;
	}

	@Registration.Singleton
	public static class Holder {
		private GeneralProperties instance;

		public GeneralProperties getInstance() {
			return this.instance;
		}

		public void setInstance(GeneralProperties instance) {
			this.instance = instance;
		}
	}
}
