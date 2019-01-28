package takamaka.translator;

import java.io.IOException;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Translator {

	public static void main(String[] args) throws IOException {
		Options options = createOptions();
		CommandLineParser parser = new DefaultParser();

	    try {
	    	CommandLine line = parser.parse(options, args);
	    	String[] appJarNames = line.getOptionValues("app");
		    Program program = new Program(Stream.concat(arrayToStream(appJarNames), arrayToStream(line.getOptionValues("lib"))));
		    for (String appJarName: appJarNames)
		    	new JarInstrumentation(appJarName, program);
	    }
	    catch (ParseException e) {
	    	System.err.println("Syntax error: " + e.getMessage());
	    	new HelpFormatter().printHelp("java " + Translator.class.getName(), options);
	    }
	}

	private static <T> Stream<T> arrayToStream(T[] array) {
		return array != null ? Stream.of(array) : Stream.empty();
	}

	private static Options createOptions() {
		Options options = new Options();
		options.addOption(Option.builder("app").desc("instrument the given application jars").hasArgs().argName("JARS").required().build());
		options.addOption(Option.builder("lib").desc("use the given library jars").hasArgs().argName("JARS").build());

		return options;
	}
}