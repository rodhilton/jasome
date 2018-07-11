package org.jasome.executive;

import org.apache.commons.cli.*;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.jasome.input.FileScanner;
import org.jasome.input.Project;
import org.jasome.output.XMLOutputter;
import org.w3c.dom.Document;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

public class CommandLineExecutive {

    public static void main(String[] args) throws IOException, ParseException {

        Options options = new Options();

        {

            //TODO: still need a way to do excludes, regex or something.  joda has an example package I want to ignore

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
            File scanDir = new File(fileParam);
            FileScanner scanner = new FileScanner(scanDir);

            IOFileFilter doesNotHaveTestSuffix = new NotFileFilter(new RegexFileFilter(Pattern.compile("(Test|Spec)\\.java$")));
            IOFileFilter isNotInTestSubDirectory = FileFilterUtils.asFileFilter(pathname -> {
                return !pathname.getPath().contains("/src/test/java");
            });

            IOFileFilter fileFilter = line.hasOption("excludetests") ? FileFilterUtils.and(doesNotHaveTestSuffix, isNotInTestSubDirectory) : FileFilterUtils.trueFileFilter();

            scanner.setFilter(fileFilter);

            long startTime = System.currentTimeMillis();

            Project scannerOutput = scanner.scan();

            ProcessorFactory.getProcessor().process(scannerOutput);

            long endTime = System.currentTimeMillis();

            try {
                Document outputDocument = new XMLOutputter().output(scannerOutput);

                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

                DOMSource source = new DOMSource(outputDocument);

                StreamResult result;
                if (line.hasOption("output")) {

                    String outputLocation = line.getOptionValue("output");
                    File tempOutputFile = new File(outputLocation + ".tmp");
                    File finalOutputFile = new File(outputLocation);

                    result = new StreamResult(tempOutputFile);
                    transformer.transform(source, result);
                    tempOutputFile.renameTo(finalOutputFile);
                    System.out.println("Operation completed in "+((endTime - startTime)/1000)+" seconds, output written to "+finalOutputFile);
                } else {
                    result = new StreamResult(System.out);
                    transformer.transform(source, result);
                }
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            }


        }
    }
}
