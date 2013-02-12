package cc.alcina.extras.dev.console;

import cc.alcina.framework.common.client.util.CommonUtils;

public class DevConsoleCommandTransforms {
	public static class CmdListClientInstances extends DevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "trcl" };
		}

		@Override
		public String getDescription() {
			return "list client instances with filters";
		}

		@Override
		public String getUsage() {
			return "trcl {id} {user_id|user_name} {days}";
		}

		@Override
		public String run(String[] argv) throws Exception {
			String sql = "select ci.*, users.username  "
					+ "from client_instance ci inner join users u on ci.user_id=u.id "
					+ "where ci.id!=-1 %s order by id desc";
			String arg0 = argv[0];
			String arg1 = argv.length < 2 ? "0" : argv[1];
			String arg2 = argv.length < 3 ? "7" : argv[2];
			String filter = "";
			filter += arg0.equals("0") ? "" : String.format(" and ci.id=%s ",
					arg0);
			if (!arg1.equals("0")) {
				filter += arg1.matches("\\d+") ? String.format(" and u.id=%s ",
						arg1) : String.format(" and u.username='%s' ", arg1);
			}
			filter += arg0.equals("0") ? "" : String.format("  age(hellodate)<'%s days'  ",
					arg0);
			console.props.idOrSet = arg0;
			console.saveConfig();
			return String.format("set id to '%s'", console.props.idOrSet);
		}
	}
}
