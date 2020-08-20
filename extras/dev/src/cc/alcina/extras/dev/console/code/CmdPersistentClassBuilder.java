package cc.alcina.extras.dev.console.code;

import java.util.stream.Collectors;

import javax.persistence.Table;

import com.sun.tools.doclint.Entity;

import cc.alcina.extras.dev.console.DevConsoleCommand;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;

public class CmdPersistentClassBuilder extends DevConsoleCommand {
	@Override
	public boolean canUseProductionConn() {
		return true;
	}

	@Override
	public String[] getCommandIds() {
		return new String[] { "persistent-class" };
	}

	@Override
	public String getDescription() {
		return "update persistence.xml";
	}

	@Override
	public String getUsage() {
		return "persistent-class {spec defined by app registry}";
	}

	@Override
	public String run(String[] argv) throws Exception {
		String collect = Registry.impls(Entity.class).stream()
				.filter(e -> e.getClass().getAnnotation(Table.class) != null)
				.map(e -> e.getClass().getName()).sorted()
				.map(n -> Ax.format("<class>%s</class>", n))
				.collect(Collectors.joining("\n"));
		Ax.out(collect);
		return "hyup";
	}
}