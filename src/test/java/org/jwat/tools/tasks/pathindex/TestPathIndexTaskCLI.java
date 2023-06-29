package org.jwat.tools.tasks.pathindex;

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
public class TestPathIndexTaskCLI {

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
	public void test_pathindextask_cli_parser() {
		CommandLine cmdLine;
		PathIndexOptions options;

		PathIndexTaskCLI object = new PathIndexTaskCLI();
		Assert.assertNotNull(object);

		Object[][] cases = new Object[][] {
			{
				new String[] {"file1"},
				new File(PathIndexOptions.DEFAULT_OUTPUT_FILENAME),
				new String[] {"file1"}
			},
			{
				new String[] {"file1", "file2"},
				new File(PathIndexOptions.DEFAULT_OUTPUT_FILENAME),
				new String[] {"file1", "file2"}
			},
			{
				new String[] {"-o", "directory/file", "file3"},
				new File("directory/file"),
				new String[] {"file3"}
			}
		};
		// path-index.unsorted.out
		for (int i=0; i<cases.length; ++i) {
			cmdLine = new CommandLine();
			cmdLine.argsArray = (String[])cases[ i ][ 0 ];
			options = PathIndexTaskCLI.parseArguments(cmdLine);
			System.out.println(options.outputFile);
			Assert.assertEquals((File)cases[ i ][ 1 ], options.outputFile);
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
			options = PathIndexTaskCLI.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-o", "outfile"};
			options = PathIndexTaskCLI.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}
	}

}
