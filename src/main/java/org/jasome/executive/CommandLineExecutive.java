package org.jasome.executive;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.cli.*;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.jasome.input.FileScanner;
import org.jasome.input.Project;
import org.jasome.output.XMLOutputter;
import org.w3c.dom.Document;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.Set;

public class CommandLineExecutive {

    public static void main(String[] args) throws IOException, ParseException {

        Options options = new Options();
        
//        args = new String[1];
//        args[0] = "C:\\Users\\gleip\\Documents\\ufjf";
                
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
            File scanDir = new File(fileParam).getAbsoluteFile();
            FileScanner scanner = new FileScanner(scanDir);

            IOFileFilter fileFilter = line.hasOption("excludetests") ? new ExcludeTestsFilter(scanDir) : FileFilterUtils.trueFileFilter();

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
                    File finalOutputFile = new File(outputLocation).getAbsoluteFile();
                    if(finalOutputFile.getParentFile()!=null) {
                        finalOutputFile.getParentFile().mkdirs();
                    }

                    result = new StreamResult(tempOutputFile);
                    transformer.transform(source, result);
                    tempOutputFile.renameTo(finalOutputFile);
                    System.out.println("Operation completed in " + ((endTime - startTime) / 1000) + " seconds, output written to " + finalOutputFile);
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

    private static class ExcludeTestsFilter implements IOFileFilter {
        private static Set<String> testSuffixes = ImmutableSet.of(
                "Test",
                "Spec",
                "Tests",
                "Specs",
                "Suite",
                "TestCase"
        );

        private static Set<String> testDirectories = ImmutableSet.of(
                "test",
                "tests",
                "examples",
                "example",
                "samples",
                "sample"
        );


        private IOFileFilter underlyingFilter;

        public ExcludeTestsFilter(File baseDir) {
            String baseDirPath = baseDir.getPath();
            IOFileFilter doesNotHaveTestSuffix = new NotFileFilter(FileFilterUtils.asFileFilter(path -> {
                for(String testSuffix: testSuffixes) {
                    if(path.getName().endsWith(testSuffix+".java")) {
                        return true;
                    }
                }
                return false;
            }));

            IOFileFilter isNotInTestSubDirectory = new NotFileFilter(FileFilterUtils.asFileFilter(path -> {
                String pathName = path.getPath();
                String relativePath = StringUtils.removeStart(pathName, baseDirPath);
                for(String testDirectory: testDirectories) {
                    if(relativePath.contains(File.separator+testDirectory+File.separator)) {
                        return true;
                    }
                }
                return false;
            }));

            this.underlyingFilter = FileFilterUtils.and(doesNotHaveTestSuffix, isNotInTestSubDirectory);
        }

        @Override
        public boolean accept(File file) {
            return underlyingFilter.accept(file);
        }

        @Override
        public boolean accept(File dir, String name) {
            return underlyingFilter.accept(dir, name);
        }
    }
}
