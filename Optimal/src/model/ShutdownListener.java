package model;

import java.util.Scanner;

public class ShutdownListener extends Thread {
    @Override
    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
			    String input = scanner.nextLine().trim();
			    if ("q".equalsIgnoreCase(input)) {
			        System.exit(0);
			    }
			}
		}
    }
}
