package org.jasome.executive;

import org.apache.commons.cli.*;
import org.jasome.parsing.JasomeScanner;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class CommandLineExecutive {

    public static void main( String[] args ) throws IOException, ParseException {

        Option help = new Option( "h", "help", false, "print this message" );
        Option version = new Option( "v", "version", false, "print the version information and exit" );

        Options options = new Options();

        options.addOption( help );
        options.addOption( version );

        // create the parser
        CommandLineParser parser = new DefaultParser();
        // parse the command line arguments
        CommandLine line = parser.parse( options, args );

        if(line.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "jasome <source directory>", options );
            System.exit(0);
        } else if(line.hasOption("version")) {
            String v = CommandLineExecutive.class.getPackage().getImplementationVersion();
            System.out.println("jasome version: "+v);
            System.exit(0);
        } else if(line.getArgs().length != 1) {
            System.out.println("No source directory provided.");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "jasome <java file or directory>", options );
            System.exit(0);
        } else {
            String fileParam = line.getArgs()[0];
            File file = new File(fileParam);
            if(!file.canRead()) {
                System.err.println("Unable to read "+fileParam);
                System.exit(-1);
            }

            JasomeScanner scanner = new JasomeScanner();
            if(file.isDirectory()) {
                Path path = FileSystems.getDefault().getPath(fileParam);
                scanner.scan(path);
            } else {
                scanner.scanFile(file);
            }
        }
    }
}
