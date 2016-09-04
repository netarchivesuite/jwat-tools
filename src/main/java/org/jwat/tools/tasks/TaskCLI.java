package org.jwat.tools.tasks;

import com.antiaction.common.cli.CommandLine;

public abstract class TaskCLI {

	public abstract void show_help();

	public abstract void runtask(CommandLine cmdLine);

}
