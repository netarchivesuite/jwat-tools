package org.jwat.tools.tasks.delete;

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
public class TestDeleteTaskCLI {

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
	public void test_deletetask_cli_parser() {
		CommandLine cmdLine;
		DeleteOptions options;

		DeleteTaskCLI object = new DeleteTaskCLI();
		Assert.assertNotNull(object);

		Object[][] cases = new Object[][] {
			{
				new String[] {"file1"},
				false, new File(DeleteOptions.DEFAULT_DELETEDFILES_FILENAME),
				new String[] {"file1"}
			},
			{
				new String[] {"file1", "file2"},
				false, new File(DeleteOptions.DEFAULT_DELETEDFILES_FILENAME),
				new String[] {"file1", "file2"}
			},
			{
				new String[] {"-o", "output-file", "file3"},
				false, new File("output-file"),
				new String[] {"file3"}
			},
			{
				new String[] {"-o", "output-file2", "--dryrun", "file4"},
				true, new File("output-file2"),
				new String[] {"file4"}
			}
		};

		for (int i=0; i<cases.length; ++i) {
			cmdLine = new CommandLine();
			cmdLine.argsArray = (String[])cases[ i ][ 0 ];
			options = DeleteTaskCLI.parseArguments(cmdLine);
			Assert.assertEquals(cases[ i ][ 1 ], options.bDryRun);
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
			options = DeleteTaskCLI.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-o", "outfile"};
			options = DeleteTaskCLI.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}
	}

}
