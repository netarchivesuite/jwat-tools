package org.jwat.tools.tasks.interval;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.List;

import org.jwat.tools.JWATTools;
import org.jwat.tools.core.CommandLine;
import org.jwat.tools.tasks.ProcessTask;

public class IntervalTask extends ProcessTask {

	public static final String commandName = "interval";

	public static final String commandDescription = "interval extract";

	public IntervalTask() {
	}

	@Override
	public void show_help() {
		System.out.println("jwattools [-o<file>] interval offset1 offset2 srcfile dstfile");
		System.out.println("");
		System.out.println("extract the byte interval from offset1 to offset2 from a file");
		System.out.println("");
		System.out.println("\tSkips data up to offset1 and save data to file until offset2 is reached.");
		System.out.println("\tOffset1/2 can be decimal or hexadecimal ($<x> or 0x<x>).");
		System.out.println("\tOffset2 can also be a length-ofsset (+<x>).");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -o<file>  output data filename");
	}

	protected long sIdx;

	protected boolean bPlusEIdx;

	protected long eIdx;

	protected String dstName;

	@Override
	public void command(CommandLine.Arguments arguments) {
		CommandLine.Argument argument;
		String tmpStr;

		// Files
		argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;

		/*
		System.out.println(filesList.size());
		for (int i=0; i<filesList.size(); ++i) {
			System.out.println(filesList.get(i));
		}
		*/

		if (filesList.size() == 4) {
			tmpStr = filesList.remove(0).toLowerCase();
			try {
				if (tmpStr.startsWith("$")) {
					sIdx = Long.parseLong(tmpStr.substring(1), 16);
				}
				else if (tmpStr.startsWith("0x")) {
					sIdx = Long.parseLong(tmpStr.substring(2), 16);
				}
				else {
					sIdx = Long.parseLong(tmpStr);
				}
			}
			catch (NumberFormatException e) {
				System.out.println("Incorrect sidx!");
				System.exit(1);
			}
			tmpStr = filesList.remove(0).toLowerCase();
			bPlusEIdx = tmpStr.startsWith("+");
			if (bPlusEIdx) {
				tmpStr = tmpStr.substring(1);
			}
			try {
				if (tmpStr.startsWith("$")) {
					eIdx = Long.parseLong(tmpStr.substring(1), 16);
				}
				else if (tmpStr.startsWith("0x")) {
					eIdx = Long.parseLong(tmpStr.substring(2), 16);
				}
				else {
					eIdx = Long.parseLong(tmpStr);
				}
				if (bPlusEIdx) {
					eIdx += sIdx;
				}
			}
			catch (NumberFormatException e) {
				System.out.println("Incorrect sidx!");
				System.exit(1);
			}
			dstName = filesList.remove(1);
		}
		else {
			System.out.println("Incorrect arguments!");
		}
		filelist_feeder( filesList, this );
	}

	@Override
	public void process(File srcFile) {
		RandomAccessFile raf = null;
		OutputStream out = null;
		byte[] buffer = new byte[8192];
		try {
			out = new BufferedOutputStream(new FileOutputStream(dstName, false), 8192);
			raf = new RandomAccessFile( srcFile, "r" );
			raf.seek(sIdx);
			long remaining = eIdx - sIdx;
			int read = 0;
			System.out.println(sIdx);
			System.out.println(eIdx);
			while (remaining > 0 && read != -1) {
				read = Math.min(buffer.length, (int)Math.min(Integer.MAX_VALUE, remaining));
				read = raf.read(buffer, 0, read);
				if (read > 0) {
					remaining -= read;
					out.write(buffer, 0, read);
				}
			}
		}
		catch (IOException e) {
		}
		finally {
			if (out != null) {
				try {
					out.flush();
					out.close();
					out = null;
				}
				catch (IOException e) {
				}
			}
			if (raf != null) {
				try {
					raf.close();
					raf = null;
				}
				catch (IOException e) {
				}
			}
		}
	}

}
