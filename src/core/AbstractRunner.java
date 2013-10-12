package core;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 *
 * @author vietan
 */
public class AbstractRunner {
    protected static CommandLineParser parser;
    protected static Options options;
    protected static CommandLine cmd;
    
    protected static boolean verbose;
    protected static boolean debug;
    
    protected static void addOption(String optName, String optDesc){
        options.addOption(OptionBuilder.withLongOpt(optName)
                    .withDescription(optDesc)
                    .hasArg()
                    .withArgName(optName)
                    .create());
    }
}
