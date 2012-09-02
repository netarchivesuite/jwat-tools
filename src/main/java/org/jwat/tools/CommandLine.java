package org.jwat.tools;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

public class CommandLine {

	private Map<String, ArgumentDefinition> longArguments = new TreeMap<String, ArgumentDefinition>();

	private ArgumentDefinition[] shortArguments = new ArgumentDefinition[ 256 ];

	private Map<String, ArgumentDefinition> textArguments = new TreeMap<String, ArgumentDefinition>();

	private List<ArgumentDefinition> normalArguments = new LinkedList<ArgumentDefinition>();

	public CommandLine() {
	}

	public void addOption(String argDefStr, int argDefId) {
		addOption( argDefStr, argDefId, -1 );
	}

	public void addOption(String argDefStr, int argDefId, int argDefSubId) {
		ArgumentDefinition argDef;
		int idx;
		if ( argDefStr.startsWith( "--" ) ) {
			argDef = new ArgumentDefinition();
			argDef.type = ArgumentDefinition.AT_MMT;
			argDef.id = argDefId;
			argDef.subId = argDefSubId;
			idx = argDefStr.indexOf( '=' );
			if ( idx == -1 ) {
				argDef.name = argDefStr.substring( 2 );
			}
			else if ( idx > 2 ) {
				argDef.name = argDefStr.substring( 2, idx );
				argDef.valueType = ArgumentDefinition.VT_REQUIRED;
			}
			else {
				throw new IllegalArgumentException( "Incomplete argument definition: " + argDefStr );
			}
			longArguments.put( argDef.name, argDef );
		}
		else if ( argDefStr.startsWith( "-" ) ) {
			if ( argDefStr.length() > 1 ) {
				argDef = new ArgumentDefinition();
				argDef.type = ArgumentDefinition.AT_MC;
				argDef.id = argDefId;
				argDef.subId = argDefSubId;
				argDef.name = argDefStr.substring( 1, 2 );
				if ( argDefStr.length() > 2 ) {
					if ( argDefStr.charAt( 2 ) == '=' ) {
						argDef.shortValueType = ArgumentDefinition.SVT_TEXT;
					}
					else if ( argDefStr.charAt( 2 ) == '[' ) {
						idx = argDefStr.indexOf( ']', 3 );
						if ( idx > 3 ) {
							argDef.shortValueType = ArgumentDefinition.SVT_OPTIONAL_CHAR;
							argDef.shortValueOptions = argDefStr.substring( 3, idx );
						}
						if ( idx == -1 ) {
							throw new IllegalArgumentException( "Missing ']': " + argDefStr );
						}
					}
					else if ( argDefStr.charAt( 2 ) == '<' ) {
						idx = argDefStr.indexOf( '>', 3 );
						if ( idx > 3 ) {
							argDef.shortValueType = ArgumentDefinition.SVT_REQUIRED_CHAR;
							argDef.shortValueOptions = argDefStr.substring( 3, idx );
						}
						if ( idx == -1 ) {
							throw new IllegalArgumentException( "Missing '>': " + argDefStr );
						}
					}
					else {
						throw new IllegalArgumentException( "Invalid argument definition: " + argDefStr );
					}
				}
				if ( argDef.name.charAt( 0 ) < 256 ) {
					shortArguments[ argDef.name.charAt( 0 ) ] = argDef;
				}
				else {
					throw new IllegalArgumentException( "Invalid chargument definition: " + argDefStr );
				}
			}
			else {
				throw new IllegalArgumentException( "Incomplete argument definition: " + argDefStr );
			}
		}
		else {
			argDef = new ArgumentDefinition();
			argDef.type = ArgumentDefinition.AT_TXT;
			argDef.id = argDefId;
			argDef.subId = argDefSubId;
			idx = argDefStr.indexOf( '=' );
			if ( idx == -1 ) {
				argDef.name = argDefStr.substring( 2 );
			}
			else if ( idx > 0 ) {
				argDef.name = argDefStr.substring( 2, idx );
				argDef.valueType = ArgumentDefinition.VT_REQUIRED;
			}
			else {
				throw new IllegalArgumentException( "Incomplete argument definition: " + argDefStr );
			}
			textArguments.put( argDef.name, argDef );
		}
	}

	public void addListArgument(String argDefStr, int argDefId, int min, int max) {
		if ( max <= 0 && max < min ) {
			throw new IllegalArgumentException( "Invalid argument number interval: " + min + ", " + max );
		}
		ArgumentDefinition argDef = new ArgumentDefinition();
		argDef.id = argDefId;
		argDef.name = argDefStr;
		argDef.min = min;
		argDef.max = max;
		normalArguments.add( argDef );
	}

	public void addArgument(String argDefStr, int argDefId) {
		ArgumentDefinition argDef = new ArgumentDefinition();
		argDef.id = argDefId;
		argDef.name = argDefStr;
		argDef.min = 1;
		argDef.max = 1;
		normalArguments.add( argDef );
	}

	private static final int SAS_ARGCHAR = 0;
	private static final int SAS_OPTIONAL_CHAR = 1;
	private static final int SAS_REQUIRED_CHAR = 2;
	private static final int SAS_EQU_OR_TEXT = 3;
	private static final int SAS_QUOTED_OR_TEXT = 4;
	private static final int SAS_QUOTED_TEXT = 5;
	private static final int SAS_TEXT = 6;

	public Arguments parse(String[] argsArray) throws ParseException {
		if ( argsArray == null || argsArray.length == 0 ) {
			return null;
		}
		Arguments args = new Arguments();
		Queue<Argument> argStack = new LinkedList<Argument>();
		String argStr;
		ArgumentDefinition argDef;
		Argument arg;
		Argument nArg = null;
		StringBuffer sb = new StringBuffer();
		int aIdx = 0;
		int cIdx;
		int nIdx = 0;
		char c;
		int state;
		while ( aIdx < argsArray.length ) {
			argStr = argsArray[ aIdx++ ];
			if ( argStr.startsWith( "--" ) ) {
				if ( argStr.length() == 2 ) {
					// Add parameter
				}
				else {
					//idx = argStr.indexOf(ch)
				}
				// unrecognized option `--la'
				throw new UnsupportedOperationException();
			}
			else if ( argStr.startsWith("-") ) {
				cIdx = 1;
				argDef = null;
				arg = null;
				state = SAS_ARGCHAR;
				while ( cIdx < argStr.length() ) {
					switch ( state ) {
					case SAS_ARGCHAR:
						c = argStr.charAt( cIdx++ );
						argDef = null;
						if ( c < 256 ) {
							argDef = shortArguments[ c & 255 ];
						}
						if ( argDef != null ) {
							arg = new Argument();
							arg.argDef = argDef;
							args.switchArgsList.add( arg );
							args.idMap.put( argDef.id, arg );
							switch ( argDef.shortValueType ) {
							case ArgumentDefinition.SVT_NONE:
								break;
							case ArgumentDefinition.SVT_OPTIONAL_CHAR:
								state = SAS_OPTIONAL_CHAR;
								break;
							case ArgumentDefinition.SVT_REQUIRED_CHAR:
								state = SAS_REQUIRED_CHAR;
								break;
							case ArgumentDefinition.SVT_TEXT:
								state = SAS_EQU_OR_TEXT;
								break;
							}
						}
						else {
							throw new ParseException( "invalid option -- " + c );
						}
						break;
					case SAS_OPTIONAL_CHAR:
						c = argStr.charAt( cIdx );
						if ( argDef.shortValueOptions.indexOf( c ) != -1 ) {
							arg.value = "" + c;
							++cIdx;
						}
						else {
							state = SAS_ARGCHAR;
						}
						break;
					case SAS_REQUIRED_CHAR:
						c = argStr.charAt( cIdx++ );
						if ( argDef.shortValueOptions.indexOf( c ) != -1 ) {
							arg.value = "" + c;
						}
						else {
							throw new ParseException( "invalid argument '" + c + "' for option -- " + arg.argDef.name );
						}
						break;
					case SAS_EQU_OR_TEXT:
						c = argStr.charAt( cIdx++ );
						sb.setLength( 0 );
						if ( c == '=' ) {
							state = SAS_QUOTED_OR_TEXT;
						}
						else if ( c == '"' ) {
							state = SAS_QUOTED_TEXT;
						}
						else {
							sb.append( c );
							state = SAS_TEXT;
						}
						break;
					case SAS_QUOTED_OR_TEXT:
						c = argStr.charAt( cIdx++ );
						if ( c == '"' ) {
							state = SAS_QUOTED_TEXT;
						}
						else {
						}
						break;
					case SAS_QUOTED_TEXT:
						c = argStr.charAt( cIdx++ );
						if ( c != '"' ) {
							sb.append( c );
						}
						else {
							if ( cIdx < argStr.length() ) {
								throw new ParseException( "argument value beyond end quote" );
							}
						}
						break;
					case SAS_TEXT:
						c = argStr.charAt( cIdx++ );
						sb.append( c );
						break;
					}
				}
				switch ( state ) {
				case SAS_REQUIRED_CHAR:
				case SAS_EQU_OR_TEXT:
					argStack.add( arg );
					break;
				case SAS_QUOTED_TEXT:
				case SAS_QUOTED_OR_TEXT:
				case SAS_TEXT:
					arg.value = sb.toString();
					break;
				}
			}
			else {
				argDef = textArguments.get( argStr );
				if ( argDef != null ) {
					arg = new Argument();
					arg.argDef = argDef;
					args.switchArgsList.add( arg );
					args.idMap.put( argDef.id, arg );
				}
				else {
					if ( argStr.startsWith( "\"" ) || argStr.endsWith( "\"" ) ) {
						if ( !argStr.startsWith( "\"" ) ) {
							throw new ParseException( "argument value missing start quote" );
						}
						if ( !argStr.endsWith( "\"" ) ) {
							throw new ParseException( "argument value missing end quote" );
						}
						argStr = argStr.substring( 1, argStr.length() - 1 );
					}
					if ( !argStack.isEmpty() ) {
						arg = argStack.remove();
						arg.value = argStr;
					}
					else {
						if ( nIdx < normalArguments.size() ) {
							argDef = normalArguments.get( nIdx );
							if ( argDef.min == 1 && argDef.max == 1 ) {
								arg = new Argument();
								arg.argDef = argDef;
								arg.value = argStr;
								//args.switchArgsList.add( arg );
								args.idMap.put( argDef.id, arg );
								++nIdx;
							}
							else {
								if ( nArg == null) {
									nArg = new Argument();
									nArg.argDef = argDef;
									//args.switchArgsList.add( nArg );
									args.idMap.put( argDef.id, nArg );
								}
								nArg.values.add( argStr );
								if ( nArg.values.size() >= argDef.max ) {
									nArg = null;
									++nIdx;
								}
							}
						}
						else {
						}
					}
				}
			}
		}
		if ( !argStack.isEmpty() ) {
			arg = argStack.remove();
			throw new ParseException( "option requires an argument -- " + arg.argDef.name );
		}
		if ( nArg != null ) {
			if ( nArg.values.size() < nArg.argDef.min ) {
				throw new ParseException( "argument(s) required -- " + nArg.argDef.name );
			}
			nArg = null;
			++nIdx;
		}
		while ( nIdx < normalArguments.size() ) {
			argDef = normalArguments.get( nIdx++ );
			if ( argDef.min > 0 ) {
				throw new ParseException( "argument(s) required -- " + argDef.name );
			}
		}
		return args;
	}

	public static class ArgumentDefinition {
		static final int AT_MMT = 1;
		static final int AT_MC = 2;
		static final int AT_TXT = 3;
		static final int VT_NONE = 0;
		static final int VT_OPTIONAL = 1;
		static final int VT_REQUIRED = 2;
		static final int SVT_NONE = 0;
		static final int SVT_OPTIONAL_CHAR = 1;
		static final int SVT_REQUIRED_CHAR = 2;
		static final int SVT_TEXT = 3;
		public byte type;
		public int id;
		public int subId;
		public String name;
		public byte valueType = VT_NONE;
		public byte shortValueType = SVT_NONE;
		public String shortValueOptions;
		public int min;
		public int max;
	}

	public static class Argument {
		public ArgumentDefinition argDef;
		public String value;
		public List<String> values = new LinkedList<String>();
	}

	public static class Arguments {
		public List<Argument> switchArgsList = new LinkedList<Argument>();
		public Map<Integer, Argument> idMap = new TreeMap<Integer, Argument>();
		public List<String> unnamedArgsList = new LinkedList<String>();
	}

	public static class ParseException extends IOException {
		/**
		 * UID.
		 */
		private static final long serialVersionUID = -8007926073603384778L;

		public ParseException() {
			super();
		}

		public ParseException(String message) {
			super( message );
		}

		public ParseException(String message, Throwable cause) {
			super( message, cause );
		}

		public ParseException(Throwable cause) {
			super( cause );
		}
	}

}
