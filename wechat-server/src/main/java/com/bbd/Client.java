package com.bbd;

import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Client {
    private final Logger logger = Logger.getLogger(Client.class.getName());
    private SimpleFormatter formatter = new SimpleFormatter();

    public static void main(String[] args) throws SecurityException, IOException {
        new Client().start();
    }

    public void start() throws SecurityException, IOException {
        System.out.println(Arrays.toString(logger.getHandlers()));
        FileHandler fh = new FileHandler("./MyLogFile.log");
        logger.addHandler(fh);
        fh.setFormatter(formatter);
        logger.info("Hello World POG!");
        input();
    }

    protected static final String commands[] = { "--exit", "--msg", "--createGroup", "--addUser", "--signIn",
            "--addGroupMember",
            "--viewGroup",
            "--help" };

    private void input() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter your command");
        System.out.println("For more commands --help <enter>");
        boolean breakLoop = false;
        while (!breakLoop) {
            try {
                System.out.println("Start");
                String command = scanner.next().trim();
                switch (command.toLowerCase()) {
                    case "--exit":
                        breakLoop = true;
                        scanner.close();
                        break;
                    case "--msg":
                        String user = scanner.next().trim();
                        String msg = scanner.nextLine().trim();
                        System.out.println("Sending to " + user + ":\n" + msg);
                        break;
                    case "--creategroup":
                        String groupName = scanner.next();
                        if (scanner.hasNext()) {
                            scanUsers(scanner);
                        } else {
                            System.out.println("Enter group members username (separated by spaces)");
                            scanUsers(scanner);
                        }
                        System.out.println("Created group " + groupName);
                        break;
                    case "--help":
                        command = scanner.nextLine().trim();
                        if (command == null || command == "") {
                            System.out.println(Arrays.toString(commands));
                            System.out.println("For more info: --help <command>");
                        } else {
                            processHelp(scanner, command);
                        }
                        break;
                    case "--adduser":
                        user = scanner.next().trim();
                        System.out.println(findUser(user) ? "Added Friend" : "Can't find user");
                        break;
                    case "--signin":
                        System.err.println("Unimplemented");
                        break;
                    case "--addgroupmember":
                        String group = scanner.next().trim();
                        System.out.println(findGroup(group) ? "Found Group" : "Wrong Group Entered");
                        scanUsers(scanner);
                        break;
                    case "--viewgroup":
                        group = scanner.next().trim();
                        System.out.println(group + ": " + viewMembers(group));
                        break;
                    default:
                        System.err.println("Incorrect Command Entered");
                        break;
                }
            } catch (Exception e) {
                System.err.println("Incorrect commands used");
            }
        }

    }

    private String viewMembers(String group) {
        return "Place Holder Group Members";
    }

    private boolean findGroup(String group) {
        return true;
    }

    private void processHelp(Scanner scanner, String command) {
        command = command.replace("--", "");
        switch (command.toLowerCase()) {
            case "exit":
                System.out.println("Exits the program");
                break;
            case "msg":
                System.out.println("Send a message to a specified user");
                System.out.println("--msg <user> <message contents>");
                break;
            case "creategroup":
                System.out.println("Creates a group for specified users with a unique group name");
                System.out.println("--createGroup <GroupName> <User>[, <User>]");
                break;
            case "adduser":
                System.out.println("Adds a user to your friend list");
                System.out.println("--addUser <User>");
                break;
            case "signIn":
                System.out.println("Signs in");
                break;
            case "addGroupMember":
                System.out.println("Adds users to a specified group");
                System.out.println("--addGroupMember <Group Name> <User>[ <User>]");
                break;
            case "viewgroup":
                System.out.println("Prints the group members within a specified group");
                System.out.println("--viewGroup <Group Name>");
                break;
            default:
                System.err.println("Command not applicable");
                break;
        }
    }

    private void scanUsers(Scanner scanner) {
        String users = scanner.nextLine().trim();
        String[] usersArr = users.split(" ");
        logger.log(Level.WARNING, Arrays.toString(usersArr));
        for (String s : usersArr) {
            System.out.println(findUser(s) ? "Added Users" : "Cannot find " + s);
        }
    }

    private boolean findUser(String s) {
        return true;
    }

}