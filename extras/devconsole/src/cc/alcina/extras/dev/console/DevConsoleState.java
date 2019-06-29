package cc.alcina.extras.dev.console;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.log.ILogRecord;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class DevConsoleState implements Serializable {
	public List<ILogRecord> logRecords;

	private Map<Long, IUser> users = new LinkedHashMap<Long, IUser>();

	public IUser ensureUser(long id, String username) {
		if (!users.containsKey(id)) {
			IUser u = Registry.impl(IUser.class);
			u.setId(id);
			u.setUserName(username);
			users.put(id, u);
		}
		return users.get(id);
	}

	public IUser getUser(Long userId) {
		return users.get(userId);
	}

	public ILogRecord logRecordById(long id) {
		if (logRecords == null) {
			return null;
		}
		for (ILogRecord l : logRecords) {
			if (l.getId() == id) {
				return l;
			}
		}
		return null;
	}
}