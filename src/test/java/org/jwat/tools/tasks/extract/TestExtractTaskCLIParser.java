package org.jwat.tools.tasks.extract;

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
public class TestExtractTaskCLIParser {

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
	public void test_extracttask_cli_parser() {
		CommandLine cmdLine;
		ExtractOptions options;

		ExtractTaskCLIParser object = new ExtractTaskCLIParser();
		Assert.assertNotNull(object);

		Object[][] cases = new Object[][] {
			{
				new String[] {"file1"},
				1, null, new String[] {"file1"}
			},
			{
				new String[] {"file1", "file2"},
				1, null, new String[] {"file1", "file2"}
			},
			{
				new String[] {"-w", "8", "file3"},
				8, null, new String[] {"file3"}
			},
			{
				new String[] {"--workers", "42", "file4"},
				42, null, new String[] {"file4"}
			},
			{
				new String[] {"file5"},
				1, null, new String[] {"file5"}
			},
			{
				new String[] {"-u", "targeturi", "file6"},
				1, "targeturi", new String[] {"file6"}
			}
		};

		for (int i=0; i<cases.length; ++i) {
			cmdLine = new CommandLine();
			cmdLine.argsArray = (String[])cases[ i ][ 0 ];
			options = ExtractTaskCLIParser.parseArguments(cmdLine);
			Assert.assertEquals(cases[ i ][ 1 ], options.threads);
			Assert.assertEquals(cases[ i ][ 2 ], options.targetUri);
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
			options = ExtractTaskCLIParser.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-w", "42"};
			options = ExtractTaskCLIParser.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-w", "0", "file"};
			options = ExtractTaskCLIParser.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-w", "fourtytwo", "file"};
			options = ExtractTaskCLIParser.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

	}

}
