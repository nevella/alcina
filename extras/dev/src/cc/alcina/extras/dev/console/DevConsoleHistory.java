package cc.alcina.extras.dev.console;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.StringEscapeUtils;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.util.IntPair;

@RegistryLocation(registryPoint = JaxbContextRegistration.class)
@XmlRootElement

public class DevConsoleHistory {
	public List<String> commands=new ArrayList<String>();
	public int index=-1;
	static final int MAX_CMD_HISTORY = 2000;
	public void addCommand(String cmd){
		commands.add(cmd);
		if(commands.size()>MAX_CMD_HISTORY){
			commands.remove(0);
		}
		goToEnd();
	}
	public void goToEnd(){
		index=commands.size();
	}
	public String getCommand(int indexDelta){
		index+=indexDelta;
		index=new IntPair(0, commands.size()).trimToRange(index);
		if(index==-1){
			return "";
		}
		return commands.get(index);
	}
	public Set<String> getMatches(String regex) {
		Set<String> matches=new LinkedHashSet<String>();
		Pattern p = Pattern.compile(String.format(".*%s.*",regex),Pattern.CASE_INSENSITIVE);
		for (int i =  commands.size()-1;i>=0; i--) {
			String cmd=commands.get(i);
			if(p.matcher(cmd).matches()){
				matches.add(cmd);
			}
		}
		return matches;
	}
}
