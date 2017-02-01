package org.jasome.executive;

import com.google.common.collect.Sets;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import org.jasome.calculators.impl.NumberOfFieldsCalculator;
import org.jasome.calculators.impl.RawTotalLinesOfCodeCalculator;
import org.jasome.calculators.impl.TotalLinesOfCodeCalculator;
import org.jasome.output.Output;
import org.jasome.output.XMLOutputter;
import org.jasome.parsing.Scanner;
import org.w3c.dom.Document;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.regex.Pattern;

public class CommandLineExecutive {

    public static void main(String[] args) throws IOException, ParseException {

        Options options = new Options();

        {
            Option help = new Option("h", "help", false, "print this message");
            Option version = new Option("v", "version", false, "print the version information and exit");
            Option excludetests = new Option("xt", "excludetests", false, "exclude test files from scanning");
            Option output = new Option("o", "output", true, "where to save output (default is print to STDOUT");

            options.addOption(help);
            options.addOption(version);
            options.addOption(excludetests);
            options.addOption(output);
        }

        CommandLineParser parser = new DefaultParser();
        CommandLine line = parser.parse(options, args);

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
            Scanner scanner = new Scanner();

            scanner.registerClassCalculator(new RawTotalLinesOfCodeCalculator());
            scanner.registerClassCalculator(new NumberOfFieldsCalculator());

            TotalLinesOfCodeCalculator totalLinesOfCodeCalculator = new TotalLinesOfCodeCalculator();
            scanner.registerPackageCalculator(totalLinesOfCodeCalculator);
            scanner.registerClassCalculator(totalLinesOfCodeCalculator);
            scanner.registerMethodCalculator(totalLinesOfCodeCalculator);

            IOFileFilter readableJavaFiles = FileFilterUtils.and(new SuffixFileFilter(".java"), CanReadFileFilter.CAN_READ);

            IOFileFilter doesNotHaveTestSuffix = new NotFileFilter(new RegexFileFilter(Pattern.compile("(Test|Spec)\\.java$")));
            IOFileFilter isNotInTestSubDirectory = FileFilterUtils.asFileFilter(pathname -> {
                return !pathname.getPath().contains("/src/test/java");
            });

            IOFileFilter fileFilter = line.hasOption("excludetests") ? FileFilterUtils.and(readableJavaFiles, doesNotHaveTestSuffix, isNotInTestSubDirectory) : readableJavaFiles;


            Output scannerOutput = scanner.scan(gatherFilesFrom(new File(fileParam), fileFilter));

            try {
                Document outputDocument = new XMLOutputter().output(scannerOutput);

                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

                DOMSource source = new DOMSource(outputDocument);

                StreamResult result;
                if(line.hasOption("output")) {
                    String outputLocation = line.getOptionValue("output");
                    result = new StreamResult(new File(outputLocation));
                } else {
                    result = new StreamResult(System.out);
                }

                transformer.transform(source, result);
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            }


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
