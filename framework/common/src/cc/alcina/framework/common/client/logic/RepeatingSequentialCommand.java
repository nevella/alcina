package cc.alcina.framework.common.client.logic;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;

public class RepeatingSequentialCommand implements RepeatingCommand {
	private List<RepeatingCommand> tasks = new ArrayList<RepeatingCommand>();
	public void cancel(){
		tasks.clear();
	}
	public void add(RepeatingCommand task){
		if(tasks.isEmpty()){
			Scheduler.get().scheduleIncremental(this);
		}
		tasks.add(task);
		
	}
	@Override
	public boolean execute() {
		if(tasks.isEmpty()){
			return false;
		}
		boolean result = tasks.get(0).execute();
		if(!result){
			tasks.remove(0);
		}
		return !tasks.isEmpty();
	}
}
