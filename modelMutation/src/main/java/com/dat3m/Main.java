package com.dat3m;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.testInfo.*;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        String folderPath = "litmus/PTX/Manual";
        ProgramCanon programCanon = new ProgramCanon(folderPath);
        List<Program> mutatedPrograms = programCanon.getMutatedPrograms();
        System.out.println(mutatedPrograms.size());
        try {
            ModelComparer modelComparer = new ModelComparer(Arch.PTX, "cat/ptx-v7.5.cat", "cat/ptxMutation/");
            List<Program> interestingPrograms = modelComparer.getInterestingProgram(mutatedPrograms);
            System.out.println(interestingPrograms.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}