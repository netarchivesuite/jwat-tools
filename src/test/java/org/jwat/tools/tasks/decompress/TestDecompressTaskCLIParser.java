package org.jwat.tools.tasks.decompress;

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
public class TestDecompressTaskCLIParser {

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
	public void test_decompresstask_cli_parser() {
		CommandLine cmdLine;
		DecompressOptions options;

		DecompressTaskCLIParser object = new DecompressTaskCLIParser();
		Assert.assertNotNull(object);

		Object[][] cases = new Object[][] {
			{
				new String[] {"file1"},
				1,
				new String[] {"file1"}
			},
			{
				new String[] {"-w", "8", "file2"},
				8,
				new String[] {"file2"}
			}
		};

		for (int i=0; i<cases.length; ++i) {
			cmdLine = new CommandLine();
			cmdLine.argsArray = (String[])cases[ i ][ 0 ];
			options = DecompressTaskCLIParser.parseArguments(cmdLine);
			Assert.assertEquals(cases[ i ][ 1 ], options.threads);
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
			options = DecompressTaskCLIParser.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-w", "8"};
			options = DecompressTaskCLIParser.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-w", "0", "file"};
			options = DecompressTaskCLIParser.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-w", "fourtytwo", "file"};
			options = DecompressTaskCLIParser.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}
	}

}
