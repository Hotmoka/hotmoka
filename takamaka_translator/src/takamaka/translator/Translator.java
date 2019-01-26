package takamaka.translator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Translator {
	private final static String SUFFIX = "_takamaka.jar";

	public static void main(String[] args) throws IOException {
		new Translator(args);
	}

	private Translator(String[] args) throws IOException {
		Options options = createOptions();

		CommandLineParser parser = new DefaultParser();
		CommandLine line;
	    try {
	    	line = parser.parse(options, args);
	    }
	    catch (ParseException e) {
	        System.err.println("Syntax error: " + e.getMessage());
	        printHelp(options);
	        System.exit(0);
	        line = null; // make the compiler happy...
	    }

	    System.out.println(Arrays.toString(line.getOptionValues("app")));
	    System.out.println(Arrays.toString(line.getOptionValues("lib"))); // might be null
	    //processJar(args[0]);
	}

	private void printHelp(Options options) {
		new HelpFormatter().printHelp("java " + Translator.class.getName(), options);
	}

	private Options createOptions() {
		Options options = new Options();
		options.addOption(Option.builder("app").desc("instrument the given application jars").hasArgs().argName("JARS").required().build());
		options.addOption(Option.builder("lib").desc("use the given library jars").hasArgs().argName("JARS").build());
		options.addOption(Option.builder("h").longOpt("help").desc("print this help").build());

		return options;
	}

	private void processJar(String jarName) throws IOException {
		System.out.println("Processing " + jarName);
		File outputFile = new File(jarName.substring(0, jarName.length() - 4) + SUFFIX);

		try (final JarFile originalJar = new JarFile(jarName);
			 final JarOutputStream instrumentedJar = new JarOutputStream(new FileOutputStream(outputFile))) {
			originalJar.stream().forEach(new JarInstrumenter(originalJar, instrumentedJar)::addEntry);
		}
	}
}