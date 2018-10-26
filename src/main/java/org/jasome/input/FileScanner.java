package org.jasome.input;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FileScanner extends Scanner {
    private static final Logger logger = LoggerFactory.getLogger(FileScanner.class);

    private File scanDir;
    private IOFileFilter filter = FileFilterUtils.and(
            new SuffixFileFilter(".java"),
            CanReadFileFilter.CAN_READ,
            HiddenFileFilter.VISIBLE
    );

    public FileScanner(File scanDir) {
        this.scanDir = scanDir;
    }

    public Project scan() {

        Collection<File> inputFiles = gatherFilesFrom(scanDir, filter);

        Collection<Pair<String, Map<String, String>>> sourceCodeWithAttributes = inputFiles
                .stream()
                .<Optional<Pair<String, Map<String, String>>>>map(file -> {
                    try {
                        String fileContents = FileUtils.readFileToString(file, Charset.defaultCharset());

                        Map<String, String> attributes = ImmutableMap.of("sourceFile", file.getAbsolutePath().replace(scanDir.getAbsolutePath(), "."));

                        return Optional.of(Pair.of(fileContents, attributes));
                    } catch (IOException e) {
                        return Optional.empty();
                    }
                }).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

        Project project = doScan(sourceCodeWithAttributes, scanDir.getAbsolutePath());

        project.addAttribute("sourceDir", scanDir.getAbsolutePath());

        return project;
    }

    public void setFilter(IOFileFilter filter) {
        this.filter = FileFilterUtils.and(filter, this.filter);
    }

    private static Collection<File> gatherFilesFrom(File file, IOFileFilter filter) {

        Collection<File> filesToScan;
        if (file.isDirectory()) {
            Collection<File> javaFiles = FileUtils.listFiles(file, filter, TrueFileFilter.INSTANCE);

            if (javaFiles.size() == 0) {
                logger.error("No .java files found in " + file.toString());
                System.exit(-1);
            }

            filesToScan = javaFiles;
        } else {
            if (!filter.accept(file)) {
                logger.error("Not a .java source file: " + file.toString());
                System.exit(-1);
            }

            filesToScan = Sets.newHashSet(file);
        }
        return filesToScan;
    }
}
