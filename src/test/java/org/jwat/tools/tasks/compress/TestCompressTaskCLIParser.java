package org.jwat.tools.tasks.compress;

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
public class TestCompressTaskCLIParser {

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
	public void test_compresstask_cli_parser() {
		CommandLine cmdLine;
		CompressOptions options;

		CompressTaskCLIParser object = new CompressTaskCLIParser();
		Assert.assertNotNull(object);

		Object[][] cases = new Object[][] {
			{
				new String[] {"file1"},
				1, -1,
				false, false, false, false, false,
				null, null, new String[] {"file1"}
			},
			{
				new String[] {"file1", "file2"},
				1, -1,
				false, false, false, false, false,
				null, null, new String[] {"file1", "file2"}
			},
			{
				new String[] {"-w", "8", "-1", "file3"},
				8, 1,
				false, false, false, false, false,
				null, null, new String[] {"file3"}
			},
			{
				new String[] {"--workers", "42", "-1", "file4"},
				42, 1,
				false, false, false, false, false,
				null, null, new String[] {"file4"}
			},
			{
				new String[] {"-d", "destination", "-2", "file5"},
				1, 2,
				false, false, false, false, false,
				new File("destination"), null, new String[] {"file5"}
			},
			{
				new String[] {"--destdir", "destination", "-3", "file6"},
				1, 3,
				false, false, false, false, false,
				new File("destination"), null, new String[] {"file6"}
			},
			{
				new String[] {"--batch", "-4", "file7"},
				1, 4,
				true, false, false, false, false,
				null, null, new String[] {"file7"}
			},
			{
				new String[] {"--dryrun", "-5", "file8"},
				1, 5,
				false, true, false, false, false,
				null, null, new String[] {"file8"}
			},
			{
				new String[] {"--remove", "-6", "file9"},
				1, 6,
				false, false, true, false, false,
				null, null, new String[] {"file9"}
			},
			{
				new String[] {"--twopass", "-7", "file10"},
				1, 7,
				false, false, false, true, false,
				null, null, new String[] {"file10"}
			},
			{
				new String[] {"--verify", "-8", "file11"},
				1, 8,
				false, false, false, false, true,
				null, null, new String[] {"file11"}
			},
			{
				new String[] {"--listfile", "listfile1", "-9", "file12"},
				1, 9,
				false, false, false, false, false,
				null, new File("listfile1"), new String[] {"file12"}
			},
			{
				new String[] {"--fast", "-w", "4", "file13"},
				4, 1,
				false, false, false, false, false,
				null, null, new String[] {"file13"}
			},
			{
				new String[] {"--best", "-w", "6", "file14"},
				6, 9,
				false, false, false, false, false,
				null, null, new String[] {"file14"}
			}
		};

		for (int i=0; i<cases.length; ++i) {
			cmdLine = new CommandLine();
			cmdLine.argsArray = (String[])cases[ i ][ 0 ];
			options = CompressTaskCLIParser.parseArguments(cmdLine);
			Assert.assertEquals(cases[ i ][ 1 ], options.threads);
			Assert.assertEquals(cases[ i ][ 2 ], options.compressionLevel);
			Assert.assertEquals(cases[ i ][ 3 ], options.bBatch);
			Assert.assertEquals(cases[ i ][ 4 ], options.bDryrun);
			Assert.assertEquals(cases[ i ][ 5 ], options.bRemove);
			Assert.assertEquals(cases[ i ][ 6 ], options.bTwopass);
			Assert.assertEquals(cases[ i ][ 7 ], options.bVerify);
			Assert.assertEquals(cases[ i ][ 8 ], options.dstPath);
			Assert.assertEquals(cases[ i ][ 9 ], options.lstFile);
			String[] expectedFileList = (String[])cases[ i ][ 10 ];
			List<String> fileList = options.filesList;
			Assert.assertEquals(expectedFileList.length, fileList.size());
			for (int j=0; j<expectedFileList.length; ++j) {
				Assert.assertEquals(expectedFileList[ j ], fileList.get( j ));
			}
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {};
			options = CompressTaskCLIParser.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-w", "42"};
			options = CompressTaskCLIParser.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-w", "0", "file"};
			options = CompressTaskCLIParser.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"-w", "fourtytwo", "file"};
			options = CompressTaskCLIParser.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}
	}

}
