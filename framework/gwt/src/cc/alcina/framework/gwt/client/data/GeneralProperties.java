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

package cc.alcina.framework.gwt.client.data;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.entity.FromClientWrapperPersistable;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocations;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.logic.reflection.misc.PerUserProperties;
import cc.alcina.framework.gwt.client.gwittir.customiser.TextAreaCustomiser;

@Bean(displayInfo = @Display(name = "Developer"), displayNamePropertyName = "id")
@XmlRootElement
@RegistryLocations(value = {
		@RegistryLocation(registryPoint = JaxbContextRegistration.class),
		@RegistryLocation(registryPoint = PerUserProperties.class) })
/**
 *
 * @author Nick Reddel
 */

 public class GeneralProperties extends WrapperPersistable  implements FromClientWrapperPersistable{
	public static final transient int DEFAULT_FILTER_DELAY = 500;

	public static final transient String PROPERTY_TRANSIENT_CSS = "transientCss";

	public static final transient String PROPERTY_PERSISTENT_CSS = "persistentCss";

	private boolean autoSave;

	private int filterDelayMs = DEFAULT_FILTER_DELAY;

	private String transientCss = "";

	private String persistentCss = "";

	private boolean allowAdminInvalidObjectWrite = true;
	
	public GeneralProperties() {
	}

	@Display(name = "ui.filterComponentActuationDelay")
	@PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.DEVELOPER))
	public int getFilterDelayMs() {
		return filterDelayMs;
	}

	@Display(helpText = "CSS which will be saved on the server, and reapplied each time you log in", name = "designer.persistentCss")
	@PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.DEVELOPER))
	@Custom(customiserClass = TextAreaCustomiser.class)
	public String getPersistentCss() {
		return this.persistentCss;
	}

	@Display(helpText = "CSS which will be applied in this session, but not saved on the server", name = "designer.transientCss",focus=true)
	@PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.DEVELOPER))
	@Custom(customiserClass = TextAreaCustomiser.class)
	@XmlTransient
	public String getTransientCss() {
		return this.transientCss;
	}

	@Display(name = "admin.allowAdminInvalidObjectWrite")
	@PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.DEVELOPER))
	public boolean isAllowAdminInvalidObjectWrite() {
		return allowAdminInvalidObjectWrite;
	}

	@Display(name = "domain.autoSaveChanges")
	@PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.DEVELOPER))
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

	public void setTransientCss(String transientCss) {
		String old_transientCss = this.transientCss;
		this.transientCss = transientCss;
		propertyChangeSupport().firePropertyChange("transientCss",
				old_transientCss, transientCss);
	}
}
