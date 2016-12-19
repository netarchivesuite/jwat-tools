package org.jwat.tools.tasks.cdx;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.jwat.tools.ExitException;
import org.jwat.tools.NoExitSecurityManager;
import org.jwat.tools.tasks.compress.CompressTaskCLIParser;

import com.antiaction.common.cli.CommandLine;

@RunWith(JUnit4.class)
public class TestCDXTaskCLIParser {

	private SecurityManager securityManager;

	@Before
	public void setUp() {
		securityManager = System.getSecurityManager();
		System.setSecurityManager(new NoExitSecurityManager());
	}

	@After
	public void tearDown() {
		System.setSecurityManager(securityManager);
	}

	@Test
	public void test_cdxtask_cli_parser() {
		CommandLine cmdLine;
		CDXOptions options;

		CDXTaskCLIParser object = new CDXTaskCLIParser();
		Assert.assertNotNull(object);

		Object[][] cases = new Object[][] {
			{
				new String[] {"file1"},
				1, new File("cdx.unsorted.out"),
				new String[] {"file1"}
			},
			{
				new String[] {"-w", "8", "file2"},
				8, new File("cdx.unsorted.out"),
				new String[] {"file2"}
			},
			{
				new String[] {"-o", "output-file", "file3"},
				1, new File("output-file"),
				new String[] {"file3"}
			}
		};

		for (int i=0; i<cases.length; ++i) {
			cmdLine = new CommandLine();
			cmdLine.argsArray = (String[])cases[ i ][ 0 ];
			options = CDXTaskCLIParser.parseArguments(cmdLine);
			Assert.assertEquals(cases[ i ][ 1 ], options.threads);
			Assert.assertEquals(cases[ i ][ 2 ], options.outputFile);
			String[] expectedFileList = (String[])cases[ i ][ 3 ];
			List<String> fileList = options.filesList;
			Assert.assertEquals(expectedFileList.length, fileList.size());
			for (int j=0; j<expectedFileList.length; ++j) {
				Assert.assertEquals(expectedFileList[ j ], fileList.get( j ));
			}
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {};
			options = CDXTaskCLIParser.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-o", "outfile"};
			options = CDXTaskCLIParser.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-w", "0", "file"};
			options = CDXTaskCLIParser.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-w", "fourtytwo", "file"};
			options = CDXTaskCLIParser.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}
	}

}
