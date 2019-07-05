package takamaka.translator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * A simple test to parse, check and instrument a jar. It performs the same tasks that
 * Takamaka performs when a jar is added to blockchain.
 * 
 * Use for instance like this:
 * 
 * java Translator -app test_contracts_dependency.jar -lib takamaka_base.jar
 * 
 * The -lib are the dependencies that should already be in blockchain
 */
public class Translator {

	public static void main(String[] args) throws IOException {
		Options options = createOptions();
		CommandLineParser parser = new DefaultParser();

	    try {
	    	CommandLine line = parser.parse(options, args);
	    	String[] appJarNames = line.getOptionValues("app");
	    	String[] libJarNames = line.getOptionValues("lib");
	    	String destinationName = line.getOptionValue("o");
		    for (String appJarName: appJarNames) {
		    	Path origin = Paths.get(appJarName);
		    	Path destination = Paths.get(destinationName);

		    	List<URL> urls = new ArrayList<>();
		    	urls.add(origin.toUri().toURL());
		    	if (libJarNames != null)
		    		for (String lib: libJarNames)
		    			urls.add(new File(lib).toURI().toURL());

		    	ClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), ClassLoader.getSystemClassLoader());
		    	new JarInstrumentation(origin, destination, classLoader);
		    }
	    }
	    catch (ParseException e) {
	    	System.err.println("Syntax error: " + e.getMessage());
	    	new HelpFormatter().printHelp("java " + Translator.class.getName(), options);
	    }
	}

	private static Options createOptions() {
		Options options = new Options();
		options.addOption(Option.builder("app").desc("instrument the given application jars").hasArgs().argName("JARS").required().build());
		options.addOption(Option.builder("lib").desc("use the given library jars").hasArgs().argName("JARS").build());
		options.addOption(Option.builder("o").desc("dump the instrumented jar with the given name").hasArg().argName("DIR").required().build());

		return options;
	}
}