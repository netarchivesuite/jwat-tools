package org.jwat.tools.tasks.changed;

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

import com.antiaction.common.cli.CommandLine;

@RunWith(JUnit4.class)
public class TestChangedTaskCLI {

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
	public void test_changedtask_cli_parser() {
		CommandLine cmdLine;
		ChangedOptions options;

		ChangedTaskCLI object = new ChangedTaskCLI();
		Assert.assertNotNull(object);

		Object[][] cases = new Object[][] {
			{
				new String[] {"file1"},
				null,
				new String[] {"file1"}
			},
			{
				new String[] {"file1", "file2"},
				null,
				new String[] {"file1", "file2"}
			},
			{
				new String[] {"-o", "output-file", "file3"},
				new File("output-file"),
				new String[] {"file3"}
			}
		};

		for (int i=0; i<cases.length; ++i) {
			cmdLine = new CommandLine();
			cmdLine.argsArray = (String[])cases[ i ][ 0 ];
			options = ChangedTaskCLI.parseArguments(cmdLine);
			Assert.assertEquals(cases[ i ][ 1 ], options.outputFile);
			String[] expectedFileList = (String[])cases[ i ][ 2 ];
			List<String> fileList = options.filesList;
			Assert.assertEquals(expectedFileList.length, fileList.size());
			for (int j=0; j<expectedFileList.length; ++j) {
				Assert.assertEquals(expectedFileList[ j ], fileList.get( j ));
			}
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {};
			options = ChangedTaskCLI.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-o", "outfile"};
			options = ChangedTaskCLI.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}
	}

}
