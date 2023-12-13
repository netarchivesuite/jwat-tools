package org.jwat.tools.tasks.arc2warc;

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
public class TestArc2WarcTaskCLI {

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
	public void test_arc2warctask_cli_parser() {
		CommandLine cmdLine;
		Arc2WarcOptions options;

		Arc2WarcTaskCLI object = new Arc2WarcTaskCLI();
		Assert.assertNotNull(object);

		Object[][] cases = new Object[][] {
			{
				new String[] {"file1"},
				1, new File(System.getProperty("user.dir")), Arc2WarcOptions.DEFAULT_PREFIX, false,
				new String[] {"file1"}
			},
			{
				new String[] {"file1", "file2"},
				1, new File(System.getProperty("user.dir")), Arc2WarcOptions.DEFAULT_PREFIX, false,
				new String[] {"file1", "file2"}
			},
			{
				new String[] {"-w", "8", "file3"},
				8, new File(System.getProperty("user.dir")), Arc2WarcOptions.DEFAULT_PREFIX, false,
				new String[] {"file3"}
			},
			{
				new String[] {"--workers", "42", "file4"},
				42, new File(System.getProperty("user.dir")), Arc2WarcOptions.DEFAULT_PREFIX, false,
				new String[] {"file4"}
			},
			{
				new String[] {"-d", "thedestdir", "file5"},
				1, new File("thedestdir"), Arc2WarcOptions.DEFAULT_PREFIX, false,
				new String[] {"file5"}
			},
			{
				new String[] {"--destdir", "thedestdir", "file6"},
				1, new File("thedestdir"), Arc2WarcOptions.DEFAULT_PREFIX, false,
				new String[] {"file6"}
			},
			{
				new String[] {"--overwrite", "file7"},
				1, new File(System.getProperty("user.dir")), Arc2WarcOptions.DEFAULT_PREFIX, true,
				new String[] {"file7"}
			},
			{
				new String[] {"--prefix", "leprefix", "file8"},
				1, new File(System.getProperty("user.dir")), "leprefix", false,
				new String[] {"file8"}
			}
		};

		for (int i=0; i<cases.length; ++i) {
			System.out.println(i);
			cmdLine = new CommandLine();
			cmdLine.argsArray = (String[])cases[ i ][ 0 ];
			options = Arc2WarcTaskCLI.parseArguments(cmdLine);
			Assert.assertEquals(cases[ i ][ 1 ], options.threads);
			Assert.assertEquals(cases[ i ][ 2 ], options.destDir);
			Assert.assertEquals(cases[ i ][ 3 ], options.prefix);
			Assert.assertEquals(cases[ i ][ 4 ], options.bOverwrite);
			String[] expectedFileList = (String[])cases[ i ][ 5 ];
			List<String> fileList = options.filesList;
			Assert.assertEquals(expectedFileList.length, fileList.size());
			for (int j=0; j<expectedFileList.length; ++j) {
				Assert.assertEquals(expectedFileList[ j ], fileList.get( j ));
			}
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {};
			options = Arc2WarcTaskCLI.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-w", "42"};
			options = Arc2WarcTaskCLI.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-w", "0", "file"};
			options = Arc2WarcTaskCLI.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-w", "fourtytwo", "file"};
			options = Arc2WarcTaskCLI.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}
	}

}
