package cc.alcina.framework.classmeta.rdb;

import java.lang.reflect.Field;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;

public class Message {
	private Class commandSet;

	private Class command;

	private int commandSetId;

	private int commandId;

	public Message(Class commandSet, Class command) {
		this.commandSet = commandSet;
		this.command = command;
		try {
			Field field = commandSet.getDeclaredField("COMMAND_SET");
			field.setAccessible(true);
			commandSetId = (int) field.get(null);
			field = command.getDeclaredField("COMMAND");
			field.setAccessible(true);
			commandId = (int) field.get(null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	int getCommandSetId() {
		return commandSetId;
	}

	int getCommandId() {
		return commandId;
	}

	String name() {
		return command.getSimpleName();
	}

	@Override
	public String toString() {
		return Ax.format("message: %s", name());
	}
}
