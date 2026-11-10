package cc.alcina.framework.common.client.csobjects;

import cc.alcina.framework.common.client.actions.RemoteParameters;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;

@Bean
@ObjectPermissions(
	read = @Permission(access = AccessLevel.ADMIN),
	write = @Permission(access = AccessLevel.ADMIN))
public class RemoteDebugParams extends Bindable implements RemoteParameters {
	private String command;

	@Display(orderingHint = 20, styleName = "wide-text", focus = true)
	public String getCommand() {
		return this.command;
	}

	public void setCommand(String command) {
		String old_command = this.command;
		this.command = command;
		propertyChangeSupport().firePropertyChange("command", old_command,
				command);
	}
}
