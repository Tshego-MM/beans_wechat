package com.levelup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.levelup.model.Chat;
import com.levelup.model.Message;
import com.levelup.model.User;
import com.sun.net.httpserver.HttpServer;

public class Handler extends Thread {
    private Scanner scanner;
    private final String baseURI = "http://wechat-beans-app.eu-west-1.elasticbeanstalk.com";
    // private final String baseURI = "http://localhost:8080";
    private String globalUser = "";
    private String username = "";
    private int chatID = -1;
    private HttpClient client;
    private int clientID;
    private ReceiverHandler receiverForMsg;
    private String accessToken = "";
    private int gitId;
    private String email;
    private String number;

    public Handler(Scanner scanner) {
        this.scanner = scanner;
        client = HttpClient.newHttpClient();
    }

    @Override
    public void run() {
        try {
            begin();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void begin() {
        input();
    }

    protected static final String commands[] = { "--exit", "--msg", "--signIn", "--help" };

    private void input() {
        boolean msgTrigger = false;
        System.out.println("Please enter your command");
        System.out.println("Type --help <enter> to see all commands available");
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
                        startMessage();
                        msgTrigger = true;
                        break;
                    case "--help":
                        command = scanner.nextLine().trim();
                        if (command == null || command == "") {
                            System.out.println("Commands: " + Arrays.toString(commands));
                            System.out.println("For more info: --help <command>");
                        } else {
                            processHelp(command);
                        }
                        break;
                    case "--adduser":
                        String user = scanner.next().trim();
                        System.out.println(findUser(user) ? "Added Friend" : "Can't find user");
                        break;
                    case "--signin":
                        String code = login();
                        this.accessToken = getAccessToken(code);
                        String gitDetails = getUserDetails();
                        processGit(gitDetails);
                    case "--insert":
                        String s = (username == "" || username == null) || gitId == 0 ? "" : insertUser();
                        if (s.contains("Violation of UNIQUE KEY constraint \'UniqueUserName\'")) {
                            System.out.println("Welcome back");
                        } else {
                            System.out.println("Signed in");
                        }
                        break;
                    default:
                        if (command.startsWith("--")) {
                            System.err.println("Incorrect command entered.");
                        } else if (msgTrigger) {
                            processMsg(command);
                        } else {
                            System.err.println(
                                    "Unknown command entered.\nType --help <enter> to see all commands available");
                        }
                        break;
                }
            } catch (Exception e) {
                System.err.println("Mishap: ");
                if (e.getMessage().contains(("com.google.gson.JsonArray but was com.google.gson.JsonPrimitive"))) {
                    System.out.println("Ensure correct spelling of username");
                } else {
                    e.printStackTrace();
                }
            }
        }

    }

    private String insertUser() throws URISyntaxException, IOException, InterruptedException {
        JsonObject json = new JsonObject();
        json.addProperty("username", username);
        json.addProperty("id", gitId);
        User user = new User(gitId, this.username, this.email, this.number);
        String jsonString = jsonifyString(user);
        return post("/users", HttpRequest.BodyPublishers.ofString(jsonString)).body().toString();
    }

    private void processGit(String gitDetails) {
        JsonObject json = new Gson().fromJson(gitDetails, JsonObject.class);
        this.gitId = Integer.parseInt(json.get("id").toString());
        this.username = json.get("login").toString();
        this.username = this.username.substring(1, this.username.length() - 1);
        String tempEmail = json.get("email").toString();
        try {
            this.email = tempEmail.equals("null") ? insertEmail() : tempEmail;
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Scans email and phone nubmer
     * 
     * @return email of user
     * @throws InterruptedException
     * @throws IOException
     * @throws URISyntaxException
     */
    private String insertEmail() throws URISyntaxException, IOException, InterruptedException {
        HttpResponse<String> response = get("/chats/" + username, "get", "value");
        String localEmail = "";
        if (response.body().toString().equals("null") || response.body() == null
                || response.body().toString() == null) {
            System.out.println("Enter email");
            localEmail = scanner.next();
            String regex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
            while (!localEmail.matches(regex)) {
                System.out.println("Invalid email");
                localEmail = scanner.next();
            }
            while (true) {
                try {
                    System.out.println("Enter Phone number");
                    this.number = scanner.next();
                    int num = Integer.parseInt(this.number);
                    if (num <= 0) {
                    } else {
                        break;
                    }
                } catch (Exception e) {
                }
                System.out.println("Invalid Phone number");
            }
        } else {
            try {
                localEmail = new Gson().fromJson(response.body(), JsonElement.class).getAsJsonObject()
                        .get("EmailAddress").getAsString();
            } catch (Exception e) {
                localEmail = "";
            }
            this.number = "";
        }
        return localEmail;
    }

    private Optional<String> code = Optional.empty();

    private void checkAccessToken() {
        if (this.accessToken == "") {
            System.out.println("Please login first");
        }
    }

    private String login() throws URISyntaxException, IOException, InterruptedException {
        String stringCode = "";
        String clientId = "baacd8518020cf9e7322";
        String clientLoginURL = "https://github.com/login/oauth/authorize?client_id=" + clientId
                + "&scope=user";
        System.out.println(clientLoginURL);
        String resp = "Authentication Successful, you can close window";
        try {

            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/login", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("code=")) {
                    code = Optional.of(query.substring(5));
                    exchange.sendResponseHeaders(200, resp.getBytes().length);
                    exchange.getResponseBody().write(resp.getBytes());
                } else {
                    exchange.sendResponseHeaders(401, 0);
                }
                exchange.close();
                server.stop(0);
            });
            server.start();
            while (code.isEmpty()) {
                Thread.sleep(50);
            }
            stringCode = code.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringCode;
    }

    private String getAccessToken(String code)
            throws URISyntaxException, IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://github.com/login/oauth/access_token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        String.format("client_id=%s&client_secret=%s&code=%s", "baacd8518020cf9e7322",
                                "d95a1c43b6651c50ed47a58c109f648c45d3f3b2",
                                URLEncoder.encode(code, "UTF-8"))))
                .build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        return response.statusCode() == 200 ? response.body().split("&")[0].split("=")[1] : "";
    }

    private String getUserDetails() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/user"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Bearer " + this.accessToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    private String processMsg(String command) throws URISyntaxException, IOException, InterruptedException {
        String msg = command.trim() + " " + scanner.nextLine().trim();
        sendMessage(msg);
        return "";
    }

    private void processHelp(String command) {
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
            case "signin":
                System.out.println("Signs in");
                break;
            case "addgroupmember":
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

    private boolean findUser(String s) {
        return true;
    }

    private HttpResponse<String> post(String extension, HttpRequest.BodyPublisher publisher)
            throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(baseURI + extension))
                .headers("Content-type", "application/json")
                .header("Authorization", "Bearer " + this.accessToken)
                .POST(publisher)
                .build();
        return client.send(request, BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String extension, String header, String value)
            throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(baseURI + extension))
                .header("Authorization", "Bearer " + this.accessToken)
                .header(header, value)
                .GET()
                .build();
        return client.send(request, BodyHandlers.ofString());
    }

    private void displayConvo() throws URISyntaxException, IOException, InterruptedException {
        System.out.println("DEBUG: Display Convo");
        String resp = get("/chats", "Get", "value").body();
        JsonArray convertedObject = new Gson().fromJson(resp, JsonArray.class);
        convertedObject.asList().stream()
                .forEach(o -> System.out.println(o.getAsJsonObject().get("chatID").toString()));
        System.out.println(convertedObject.asList());
    }

    private void startMessage() throws URISyntaxException, IOException, InterruptedException {
        globalUser = scanner.next().trim();
        String msg = scanner.nextLine().trim();
        // get all chats for a certain user id and receiver username
        System.out.println("Started coversation with " + globalUser);
        System.out.println("Sending to " + globalUser + (msg == "" ? ":" : (":\nMe:" + msg)));
        // receiverForMsg = new ReceiverHandler(username, globalUser);
        // receiverForMsg.start();
        // sleep(50);
        Chat c = new Chat(-1, username, globalUser);
        String json = jsonifyString(c);
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
        DEBUG(json);
        String path = "/chats/userchat/" + cutString(jsonObject.get("Sender").toString()) + "/"
                + cutString(jsonObject.get("Receiver").toString());
        HttpResponse<String> response = get(path, "get", "value");
        DEBUG("chats response:" + response.body().toString());
        if (response.body().toString().contains("404 NOT_FOUND Not Found,Record not found")) {
            System.out.println("Brand New Conversation");
            String jsonString = jsonifyString(c); // json the chat
            response = post("/chats", HttpRequest.BodyPublishers.ofString(jsonString));
            if (response.body().toString().contains("404 NOT_FOUND Not Found,Record not found")) {
                System.err.println("Error with CHATS POST");
            }
            JsonArray resp = new Gson().fromJson(response.body(), JsonArray.class);
            chatID = Integer.parseInt(resp.get(resp.size() - 1).getAsJsonObject().get("ChatId").getAsString());
        } else { // All chats received
            try {
                JsonArray allChats = new Gson().fromJson(response.body(), JsonArray.class);
                if (msg != "") {
                    sendMessage(msg);
                } else {
                    allChats.asList().stream().forEach(obj -> printConvo(obj));
                }
            } catch (Exception e) {
                if (username == "" || username == null) {
                    System.out.println("Please --signin");
                } else {
                    System.out.println("Caught: " + response.body());
                    e.printStackTrace();
                }
            }
        }
    }

    private String cutString(String s) {
        return s.substring(1, s.length() - 1);
    }

    private void printConvo(JsonElement json) {
        // Lambda expression's parameter json cannot redeclare another local variable
        // defined in an enclosing scope.
        JsonObject jObject = json.getAsJsonObject();
        String content = jObject.get("content").toString();
        String name = jObject.get("senderUserName").toString().replace(username, "Me");
        String tmStamp = jObject.get("CreatedAt").toString();
        chatID = jObject.get("senderUserName").toString().equals("\"" + username + "\"")
                ? Integer.parseInt(jObject.get("ChatId").toString())
                : chatID;
        System.out.println(tmStamp.substring(1, tmStamp.length() - 1) + " " + name.replace("\"", "") + ": "
                + content.substring(1, content.length() - 1));
        DEBUG(chatID + "CHAT ID");
        DEBUG(json.getAsJsonObject().get("senderUserName").toString());
    }

    private void sendMessage(String msg) throws URISyntaxException, IOException, InterruptedException {
        String json = jsonifyString(new Message(chatID, msg));
        HttpResponse<String> response = post("/messages", HttpRequest.BodyPublishers.ofString(json));
        if (response.body().contains("201 CREATED Created,Record Inserted successfully")) {
            // Received Successfully
            String path = "/chats/userchat/" + username + "/"
                    + globalUser;
            response = get(path, "get", "value");
            JsonArray allChats = new Gson().fromJson(response.body(), JsonArray.class);
            allChats.asList().stream()
                    .forEach(obj -> printConvo(obj));
        } else {
            System.err.println("Please --signin and --msg");
        }
    }

    private String jsonifyString(Object o) {
        Gson gson = new Gson();
        return gson.toJson(o);
    }

    private boolean DEBUG = false;

    private void DEBUG(String s) {
        System.out.print(DEBUG ? "DEBUG: " + s + "\n" : "");
    }
    /*
     * Registerd app
     * gotten secrets
     * istall libraries (maven dependencies)
     * 
     */
}