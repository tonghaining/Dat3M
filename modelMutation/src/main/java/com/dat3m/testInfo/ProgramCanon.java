package com.dat3m.testInfo;

import com.dat3m.dartagnan.program.Program;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProgramCanon {
    private String folderPath;

    public ProgramCanon(String folderPath) {
        this.folderPath = folderPath;
    }

    public List<String> getLitmusPaths() {
        // get all files in the folder end with .litmus
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        List<String> paths = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".litmus")) {
                    paths.add(file.getAbsolutePath());
                }
            }
        }
        return paths;
    }

    private List<Program> getLitmusMutation(String litmusInputPath) {
        EventExtractor extractor = new EventExtractor(litmusInputPath);
        PTXTagManipulator tagManipulator = new PTXTagManipulator(extractor);
        try {
            List<Program> taggedPrograms = tagManipulator.getTaggedPrograms();
            return taggedPrograms;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return List.of();
    }

    public List<Program> getMutatedPrograms() {
        List<Program> mutatedPrograms = new ArrayList<>();
        List<String> litmusPaths = getLitmusPaths();
        for (String litmusPath : litmusPaths) {
            mutatedPrograms.addAll(getLitmusMutation(litmusPath));
        }
        return mutatedPrograms;
    }
}
