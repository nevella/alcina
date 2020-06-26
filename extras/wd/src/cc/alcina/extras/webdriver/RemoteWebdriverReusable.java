package cc.alcina.extras.webdriver;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Optional;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;

import cc.alcina.extras.webdriver.WDDriverHandlerExt.PersistentDriverData;
import cc.alcina.extras.webdriver.WDDriverHandlerExt.PersistentDriverDataNode;
import cc.alcina.framework.common.client.WrappedRuntimeException;

public class RemoteWebdriverReusable extends RemoteWebDriver {
	public RemoteWebdriverReusable() {
		super();
	}

	public RemoteWebdriverReusable(Capabilities desiredCapabilities) {
		super(desiredCapabilities);
	}

	public RemoteWebdriverReusable(CommandExecutor executor,
			Capabilities desiredCapabilities) {
		super(executor, desiredCapabilities);
	}

	public RemoteWebdriverReusable(URL remoteAddress,
			Capabilities desiredCapabilities) {
		super(remoteAddress, desiredCapabilities);
	}

	@Override
	protected void startSession(Capabilities desiredCapabilities) {
		// try resume first
		PersistentDriverData persistentDriverData = WDDriverHandlerExt
				.persistentDriverData();
		Optional<PersistentDriverDataNode> persistent = persistentDriverData.nodes
				.stream().filter(n -> WDDriverHandlerPersist.class
						.isAssignableFrom(n.driverClass))
				.findFirst();
		if (persistent.isPresent()) {
			PersistentDriverDataNode node = persistent.get();
			try {
				{
					SessionId sessionId = new SessionId(node.sessionId);
					Field field = RemoteWebDriver.class
							.getDeclaredField("sessionId");
					field.setAccessible(true);
					field.set(this, sessionId);
				}
				{
					Field field = RemoteWebDriver.class
							.getDeclaredField("capabilities");
					field.setAccessible(true);
					field.set(this, node.capabilities);
				}
				{
					Field field = RemoteWebDriver.class
							.getDeclaredField("executor");
					field.setAccessible(true);
					HttpCommandExecutor executor = (HttpCommandExecutor) field
							.get(this);
					{
						Field field2 = HttpCommandExecutor.class
								.getDeclaredField("commandCodec");
						field2.setAccessible(true);
						field2.set(executor,
								node.commandCodecClass.newInstance());
					}
					{
						Field field2 = HttpCommandExecutor.class
								.getDeclaredField("responseCodec");
						field2.setAccessible(true);
						field2.set(executor,
								node.responseCodecClass.newInstance());
					}
				}
				String currentUrl = getCurrentUrl();
				if (currentUrl.contains("gwt.codesvr")) {
					return;
				}
			} catch (Exception e) {
				e.printStackTrace();
				{
					try {
						{
							Field field = RemoteWebDriver.class
									.getDeclaredField("sessionId");
							field.setAccessible(true);
							field.set(this, null);
						}
						{
							Field field = RemoteWebDriver.class
									.getDeclaredField("executor");
							field.setAccessible(true);
							HttpCommandExecutor executor = (HttpCommandExecutor) field
									.get(this);
							{
								Field field2 = HttpCommandExecutor.class
										.getDeclaredField("commandCodec");
								field2.setAccessible(true);
								field2.set(executor, null);
							}
							{
								Field field2 = HttpCommandExecutor.class
										.getDeclaredField("responseCodec");
								field2.setAccessible(true);
								field2.set(executor, null);
							}
						}
					} catch (Exception e2) {
						throw new WrappedRuntimeException(e2);
					}
				}
			}
		}
		super.startSession(desiredCapabilities);
	}
}
