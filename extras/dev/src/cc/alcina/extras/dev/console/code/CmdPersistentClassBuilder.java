package cc.alcina.extras.dev.console.code;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Table;

import cc.alcina.extras.dev.console.DevConsoleCommand;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.transform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;

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
		Stream<Class<?>> entities = Registry.query().withKeys(Entity.class)
				.untypedRegistrations()
				.filter(e -> e.getAnnotation(Table.class) != null);
		Stream<Class> transformPersistent = Stream.of(
				PersistentImpl.getImplementation(
						DomainTransformRequestPersistent.class),
				PersistentImpl.getImplementation(
						DomainTransformEventPersistent.class));
		String collect = Stream.concat(entities, transformPersistent)
				.map(e -> e.getName()).sorted()
				.map(n -> Ax.format("<class>%s</class>", n))
				.collect(Collectors.joining("\n"));
		console.setClipboardContents(collect);
		Ax.out(collect);
		return "hyup";
	}
}