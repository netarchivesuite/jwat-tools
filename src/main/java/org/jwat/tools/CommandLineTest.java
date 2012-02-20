package org.jwat.tools;

public class CommandLineTest {

	public static final int A_DECOMPRESS = 1;
	public static final int A_COMPRESS = 2;
	public static final int A_CLVL_1 = 3;
	public static final int A_CLVL_2 = 4;
	public static final int A_CLVL_3 = 5;
	public static final int A_CLVL_4 = 6;
	public static final int A_CLVL_5 = 7;
	public static final int A_CLVL_6 = 8;
	public static final int A_CLVL_7 = 9;
	public static final int A_CLVL_8 = 10;
	public static final int A_CLVL_9 = 11;

	public static void main(String[] args) {
		CommandLine cmdLine = new CommandLine();
		cmdLine.addOption( "-d", A_DECOMPRESS );
		cmdLine.addOption( "-c", A_COMPRESS );
		cmdLine.addOption( "-1", A_CLVL_1 );
		cmdLine.addOption( "-2", A_CLVL_2 );
		cmdLine.addOption( "-3", A_CLVL_3 );
		cmdLine.addOption( "-4", A_CLVL_4 );
		cmdLine.addOption( "-5", A_CLVL_5 );
		cmdLine.addOption( "-6", A_CLVL_6 );
		cmdLine.addOption( "-7", A_CLVL_7 );
		cmdLine.addOption( "-8", A_CLVL_8 );
		cmdLine.addOption( "-9", A_CLVL_9 );
		try {
			//CommandLine.Arguments arguments = cmdLine.parse( "-d -c".split( " " ) );
			CommandLine.Arguments arguments = cmdLine.parse( "-d C:\\*.gz".split( " " ) );
			CommandLine.Argument argument;
			for ( int i=0; i<arguments.switchArgsList.size(); ++i) {
				argument = arguments.switchArgsList.get( i );
				System.out.println( argument.argDef.id + "=" + argument.value );
			}
		}
		catch (CommandLine.ParseException e) {
			e.printStackTrace();
		}
	}

}
