package cc.alcina.tool.agent;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.ProtectionDomain;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * An agent to track loaded classes, and write them every 1s to the filesystem
 *
 */
public class ClassTrackingAgent implements ClassFileTransformer {
	Timer timer;

	Set<String> classNames = new TreeSet<>();

	public ClassTrackingAgent() {
	}

	public synchronized byte[] transform(ClassLoader loader, String className,
			Class<?> redefiningClass, ProtectionDomain domain, byte[] bytes)
			throws IllegalClassFormatException {
		if (timer == null) {
			timer = new Timer("log-timer");
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					persist();
				}
			};
			timer.scheduleAtFixedRate(task, 1000, 1000);
		}
		classNames.add(className.replace("/", "."));
		return bytes;
	}

	void persist() {
		try {
			File file = new File("/tmp/ClassTrackingAgent.txt");
			String string = classNames.stream()
					.collect(Collectors.joining("\n"));
			Files.writeString(Paths.get(file.toURI()), string,
					StandardCharsets.UTF_8, StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void premain(String agentArgs,
			Instrumentation instrumentation) {
		System.out.println("Initialising ClassTrackingAgent");
		ClassTrackingAgent interceptingClassTransformer = new ClassTrackingAgent();
		instrumentation.addTransformer(interceptingClassTransformer);
	}
}