package org.jwat.tools.tasks;

import org.jwat.tools.core.CommandLine;

public abstract class Task {

	public abstract void show_help();

	public abstract void command(CommandLine.Arguments arguments);

}
