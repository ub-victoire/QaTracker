package org.qatracker;

import org.qatracker.service.TestCaseService;
import org.qatracker.storage.TestCaseFileStore;
import org.qatracker.ui.ConsoleApp;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            System.out.println("Usage: java -cp out/main org.qatracker.Main [data-file]");
            return;
        }
        try {
            Path path = Path.of(args.length == 1 ? args[0] : "testcases.xml");
            var store = new TestCaseFileStore(path);
            var service = new TestCaseService(store.load());
            System.out.println("Loaded " + service.getAll().size() + " test case(s).");
            new ConsoleApp(service, store, new Scanner(System.in)).run();
        } catch (IOException | IllegalArgumentException exception) {
            System.out.println("Cannot open QA Tracker: " + exception.getMessage());
            System.out.println("Startup stopped to protect existing data. No data was overwritten.");
            System.out.println("Check the file or back it up before repairing it.");
        }
    }
}
