package org.jwat.tools;

import java.util.List;

import org.jwat.tools.core.CommandLine.Argument;
import org.jwat.tools.core.CommandLine.Arguments;
import org.jwat.tools.tasks.Task;

public class HelpTask extends Task {

	public static final String commandName = "help";

	public static final String commandDescription = "display help information";

	public HelpTask() {
	}

	@Override
	public void show_help() {
		System.out.println("Work in progress...");
	}

	@Override
	public void command(Arguments arguments) {
		Argument argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> files = null;
		String command = null;
		if (argument != null) {
			files = argument.values;
		}
		if (files != null && files.size() > 0) {
			command = files.get(0);
		}
		if (command != null) {
			Class<? extends Task> clazz = JWATTools.commandMap.get(command);
			if (clazz != null) {
				try {
					Task task = clazz.newInstance();
					task.show_help();
				}
				catch (InstantiationException e) {
					e.printStackTrace();
				}
				catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
			else {
				System.out.println("Unknown command -- " + command);
			}
		}
	}

}
