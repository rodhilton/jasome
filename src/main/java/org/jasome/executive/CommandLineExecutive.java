package org.jasome.executive;

import com.google.common.collect.Sets;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import org.jasome.calculators.impl.RawTotalLinesOfCodeCalculator;
import org.jasome.calculators.impl.TotalLinesOfCodeCalculator;
import org.jasome.parsing.JasomeScanner;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.regex.Pattern;

public class CommandLineExecutive {

    public static void main(String[] args) throws IOException, ParseException {

        Option help = new Option("h", "help", false, "print this message");
        Option version = new Option("v", "version", false, "print the version information and exit");

        Option excludetests = new Option("xt", "excludetests", false, "exclude test files from scanning");

        Options options = new Options();

        options.addOption(help);
        options.addOption(version);
        options.addOption(excludetests);

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

            scanner.registerClassCalculator(new RawTotalLinesOfCodeCalculator());

            TotalLinesOfCodeCalculator totalLinesOfCodeCalculator = new TotalLinesOfCodeCalculator();
            scanner.registerPackageCalculator(totalLinesOfCodeCalculator);
            scanner.registerClassCalculator(totalLinesOfCodeCalculator);
            scanner.registerMethodCalculator(totalLinesOfCodeCalculator);

            IOFileFilter readableJavaFiles = FileFilterUtils.and(new SuffixFileFilter(".java"), CanReadFileFilter.CAN_READ);

            IOFileFilter doesNotHaveTestSuffix = new NotFileFilter(new RegexFileFilter(Pattern.compile("(Test|Spec)\\.java$")));
            IOFileFilter isNotInTestSubDirectory = FileFilterUtils.asFileFilter(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    System.out.println(pathname.getPath());
                    return !pathname.getPath().contains("/src/test/java");
                }
            });

            IOFileFilter fileFilter = line.hasOption("excludetests") ? FileFilterUtils.and(readableJavaFiles, doesNotHaveTestSuffix, isNotInTestSubDirectory) : readableJavaFiles;


            scanner.scan(gatherFilesFrom(new File(fileParam), fileFilter));
        }
    }

    private static Collection<File> gatherFilesFrom(File file, IOFileFilter filter) {

        Collection<File> filesToScan;
        if (file.isDirectory()) {
            Collection<File> javaFiles = FileUtils.listFiles(file, filter, TrueFileFilter.INSTANCE);

            if (javaFiles.size() == 0) {
                System.err.println("No .java files found in " + file.toString());
                System.exit(-1);
            }

            filesToScan = javaFiles;
        } else {
            if (!filter.accept(file)) {
                System.err.println("Not a .java source file: " + file.toString());
                System.exit(-1);
            }

            filesToScan = Sets.newHashSet(file);
        }
        return filesToScan;
    }
}
