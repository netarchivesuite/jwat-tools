package org.jwat.tools.tasks.interval;

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
public class TestIntervalTaskCLI {

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
	public void test_intervaltask_cli_parser() {
		CommandLine cmdLine;
		IntervalOptions options;

		IntervalTaskCLI object = new IntervalTaskCLI();
		Assert.assertNotNull(object);

		Object[][] cases = new Object[][] {
			{
				new String[] {"10", "30", "infile1", "outfile1"},
				10L, false, 30L, "outfile1", new String[] {"infile1"}
			},
			{
				new String[] {"10", "+30", "infile2", "outfile2"},
				10L, true, 40L, "outfile2", new String[] {"infile2"}
			},
			{
				new String[] {"$10", "$30", "infile3", "outfile3"},
				16L, false, 48L, "outfile3", new String[] {"infile3"}
			},
			{
				new String[] {"0x10", "+0x30", "infile4", "outfile4"},
				16L, true, 64L, "outfile4", new String[] {"infile4"}
			}
		};

		for (int i=0; i<cases.length; ++i) {
			cmdLine = new CommandLine();
			cmdLine.argsArray = (String[])cases[ i ][ 0 ];
			options = IntervalTaskCLI.parseArguments(cmdLine);
			Assert.assertEquals(cases[ i ][ 1 ], options.sIdx);
			Assert.assertEquals(cases[ i ][ 2 ], options.bPlusEIdx);
			Assert.assertEquals(cases[ i ][ 3 ], options.eIdx);
			Assert.assertEquals(cases[ i ][ 4 ], options.dstName);
			String[] expectedFileList = (String[])cases[ i ][ 5 ];
			List<String> fileList = options.filesList;
			Assert.assertEquals(expectedFileList.length, fileList.size());
			for (int j=0; j<expectedFileList.length; ++j) {
				Assert.assertEquals(expectedFileList[ j ], fileList.get( j ));
			}
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"fourtytwo", "30", "infile1", "outfile1"};
			options = IntervalTaskCLI.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"$fourtytwo", "$30", "infile2", "outfile2"};
			options = IntervalTaskCLI.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"0xfourtytwo", "+0x30", "infile3", "outfile3"};
			options = IntervalTaskCLI.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"10", "fourtytwo", "infile4", "outfile4"};
			options = IntervalTaskCLI.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"$10", "$fourtytwo", "infile5", "outfile5"};
			options = IntervalTaskCLI.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}

		try {
			cmdLine = new CommandLine();
			cmdLine.argsArray = new String[] {"0x10", "+0xfourtytwo", "infile6", "outfile6"};
			options = IntervalTaskCLI.parseArguments(cmdLine);
			Assert.fail("Exception expected!");
		}
		catch (ExitException e) {
		}
	}

}
