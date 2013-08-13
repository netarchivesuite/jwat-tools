package org.jwat.tools.tasks.changed;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jwat.tools.JWATTools;
import org.jwat.tools.core.CommandLine;
import org.jwat.tools.tasks.ProcessTask;

public class ChangedTask extends ProcessTask {

	public static final String commandName = "changed";

	public static final String commandDescription = "changed files grouped by intervals";

	public ChangedTask() {
	}

	@Override
	public void show_help() {
		System.out.println("jwattools changed <filepattern>...");
		System.out.println("");
		System.out.println("group files by similar last modified dates");
		System.out.println("");
		System.out.println("\tUseful command for identifying when and if files where modified");
		System.out.println("\tin close proximity of others.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -o<file>  output intervals and files to file");
	}

	public List<Long> data = new ArrayList<Long>();

	public List<Interval> intervals = new ArrayList<Interval>();

	@Override
	public void command(CommandLine.Arguments arguments) {
		CommandLine.Argument argument;

		// Output file.
		File outputFile = null;
		argument = arguments.idMap.get( JWATTools.A_OUTPUT );
		if ( argument != null && argument.value != null ) {
			outputFile = new File(argument.value);
			if (outputFile.isDirectory()) {
				System.out.println("Can not output to a directory!");
				System.exit(1);
			} else if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
				if (!outputFile.getParentFile().mkdirs()) {
					System.out.println("Could not create parent directories!");
					System.exit(1);
				}
			}
		}

		// Files
		argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;

		filelist_feeder(filesList, this);

		Collections.sort(data);

		Interval interval;
		long l;
		int idx;

		if (data.size() > 0) {
			idx = 0;
			interval = new Interval();
			l = data.get(idx++);
			interval.from = l;
			interval.to = l;
			intervals.add(interval);
			while (idx < data.size()) {
				l = data.get( idx++ );
				if (l < (interval.to + 60000)) {
					interval.to = l;
					++interval.count;
				} else {
					interval = new Interval();
					interval.from = l;
					interval.to = l;
					intervals.add(interval);
				}
			}
		}

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		idx = 0;
		while (idx < intervals.size()) {
			interval = intervals.get( idx++ );
			System.out.println(dateFormat.format(new Date(interval.from)) + " -> " + dateFormat.format(new Date(interval.to)) + " : " + interval.count );
		}
	}

	@Override
	public void process(File srcFile) {
		data.add( srcFile.lastModified() );
	}

	public static class Interval {
		public long from;
		public long to;
		public int count = 1;
	}

}
