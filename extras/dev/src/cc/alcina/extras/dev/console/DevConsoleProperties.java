package cc.alcina.extras.dev.console;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.xml.bind.annotation.XmlRootElement;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;

@RegistryLocation(registryPoint = JaxbContextRegistration.class)
@XmlRootElement
public class DevConsoleProperties {
	@SetPropInfo(key = "id-or-set", description = "id or id set")
	public String idOrSet;

	public String logLevel = "DEBUG";

	@SetPropInfo(key = "dump")
	public boolean dump = false;

	public boolean useMountSshfsFs;

	@SetPropInfo(key = "preferred-height", description = "Preferred console height")
	public int preferredHeight = 850;

	@SetPropInfo(key = "max-id", description = "Max id for id list creation")
	public long maxId = 999999;

	public String diffFile1 = "";

	public String diffFile2 = "";

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	@Target({ ElementType.FIELD })
	public static @interface SetPropInfo {
		String key();

		String description() default "";
	}

	@SetPropInfo(key = "last-command")
	public String lastCommand = "";

	@SetPropInfo(key = "conn-use-prod")
	public boolean connection_useProduction = false;

	@SetPropInfo(key = "conn-local")
	public String connection_local = "jdbc:postgresql://127.0.0.1:5432/jade,jade,jade";

	@SetPropInfo(key = "conn-production", description = "obviously never write - also, you'll need to tunnel (pg_hba.conf): \n"
			+ "ssh -A nreddel@nat2.barnet.com.au -L 5434:jade.int.barnet.com.au:5432\n" +
			"ssh -A nreddel@nat2.barnet.com.au -L 5434:cosa.int.barnet.com.au:5432")
	public String connection_production = "jdbc:postgresql://127.0.0.1:5434/jade,jade,jade";

	@SetPropInfo(key = "remote-home-dir")
	public String remoteHomeDir = "/root";

	@SetPropInfo(key = "remote-ssh")
	public String remoteSsh = "root@jade.int.barnet.com.au:";

	@SetPropInfo(key = "font-name")
	/*
	 * Mac - "Prestige Elite Std"
	 */
	public String fontName = "Courier New";
}
