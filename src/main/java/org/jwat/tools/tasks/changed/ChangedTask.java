package org.jwat.tools.tasks.changed;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jwat.tools.tasks.AbstractTask;

public class ChangedTask extends AbstractTask {

	public List<Long> data = new ArrayList<Long>();

	public List<Interval> intervals = new ArrayList<Interval>();

	public ChangedTask() {
	}

	public void runtask(ChangedOptions options) {
		filelist_feeder(options.filesList, this);

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
			// TODO Save to supplied output file parameter!
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
