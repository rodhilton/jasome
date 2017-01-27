package org.jasome.executive;

import com.google.common.collect.Sets;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import org.jasome.calculators.RawTotalLinesOfCodeCalculator;
import org.jasome.calculators.TotalLinesOfCodeCalculator;
import org.jasome.parsing.JasomeScanner;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class CommandLineExecutive {

    public static void main(String[] args) throws IOException, ParseException {

        Option help = new Option("h", "help", false, "print this message");
        Option version = new Option("v", "version", false, "print the version information and exit");

        Options options = new Options();

        options.addOption(help);
        options.addOption(version);

        // create the parser
        CommandLineParser parser = new DefaultParser();
        // parse the command line arguments
        CommandLine line = parser.parse(options, args);

        //TODO: option to exclude by suffix or regex, just some way to exclude test files

        if (line.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("jasome <source directory>", options);
            System.exit(0);
        } else if (line.hasOption("version")) {
            String v = CommandLineExecutive.class.getPackage().getImplementationVersion();
            System.out.println("jasome version: " + v);
            System.exit(0);
        } else if (line.getArgs().length != 1) {
            System.out.println("No source directory provided.");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("jasome <java file or directory>", options);
            System.exit(0);
        } else {
            String fileParam = line.getArgs()[0];
            JasomeScanner scanner = new JasomeScanner();
            scanner.register(new RawTotalLinesOfCodeCalculator());
            scanner.register(new TotalLinesOfCodeCalculator());
            scanner.scan(gatherFilesFrom(new File(fileParam)));
        }
    }

    private static Collection<File> gatherFilesFrom(File file) {
        IOFileFilter javaFilesOnly = new AndFileFilter(new SuffixFileFilter(".java"), CanReadFileFilter.CAN_READ);

        Collection<File> filesToScan;
        if (file.isDirectory()) {
            Collection<File> javaFiles = FileUtils.listFiles(file, javaFilesOnly, TrueFileFilter.INSTANCE);

            if (javaFiles.size() == 0) {
                System.err.println("No .java files found in " + file.toString());
                System.exit(-1);
            }

            filesToScan = javaFiles;
        } else {
            if (!javaFilesOnly.accept(file)) {
                System.err.println("Not a .java source file: " + file.toString());
                System.exit(-1);
            }

            filesToScan = Sets.newHashSet(file);
        }
        return filesToScan;
    }
}
