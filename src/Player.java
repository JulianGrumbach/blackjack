import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;

//Uses UDP_Chat as template
public class Player {

    private static InetAddress ip;
    private static int port;
    private static String name;
    private static boolean isRegistered = false;
    private static InetAddress croupier_ip;
    private static int croupier_port;

    private static void fatal(String input) {
        System.err.println(input);
        System.exit(-1);
    }

    public static boolean isIP(String ip) { // Checks if String is valid IPv4 address
        String[] parts = ip.split("\\."); // Split by dot
        if (parts.length != 4) {
            return false;
        } // Must be 4 chunks
        for (String p : parts) { // Check if numbers are valid
            try {
                int number = Integer.parseInt(p);
                if (number < 0 || number > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public static boolean isPort(String port) {
        try {
            int number = Integer.parseInt(port);
            if (number < 0 || number > 65535) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static void main(String[] args) {

        // Handling arguments, checking validity
        if (args.length != 3) {
            fatal("Arguments: \"<ip> <port number> <player name>\"");
        }
        if (!isIP(args[0])) {
            fatal("Invalid IP address");
        } else {
            // Parse entered IP address
            try {
                ip = InetAddress.getByName(args[0]);
            } catch (UnknownHostException e) {
                fatal("Invalid IP address while parsing");
            }
        }
        if (!isPort(args[1])) {
            fatal("Invalid port number");
        } else {
            port = Integer.parseInt(args[1]);
        }
        name = args[2];

        // Start a new thread to listen for messages
        new Thread(() -> receiveLines(port)).start();

        // Main thread continues to process user input
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) { // closes automatically
            String input;
            while (!(input = br.readLine()).equalsIgnoreCase("quit")) {
                String[] parts = input.split(" ");
                if (parts[0].equalsIgnoreCase("registerPlayer") && !isRegistered) {
                    if ((parts.length == 3 || parts.length == 4) && isPort(parts[2]) && isIP(parts[1])) {
                        System.out.println("Registering player...");
                        name = parts.length == 4 ? parts[3] : name;
                        register(parts[1], parts[2]);
                    } else {
                        System.err.println("Invalid command: \"registerPlayer <ip> <port> [<name>]\"");
                    }
                } else if (parts[0].equalsIgnoreCase("gameover")) {
                    if (isRegistered) {
                        System.out.println("Unregistering player...");
                        sendLines(croupier_ip, croupier_port, "gameover");
                        isRegistered = false;
                    }
                } else if (parts[0].equalsIgnoreCase("bet")) {
                    if (!isRegistered) {
                        System.err.println("Player not registered.");
                        continue;
                    }
                    if (parts.length == 2 && parts[1].matches("\\d+")) {
                        int amount = Integer.parseInt(parts[1]);
                        if (amount > 0) {
                            sendLines(croupier_ip, croupier_port, "bet " + name + " " + amount);
                        } else {
                            System.err.println("Invalid amount.");
                        }
                    } else {
                        System.err.println("Invalid command: \"bet <amount>\"");
                    }
                } else if (parts[0].equalsIgnoreCase("hit")) {
                    if (!isRegistered) {
                        System.err.println("Player not registered.");
                        continue;
                    }
                    if (parts.length == 3) {
                        sendLines(croupier_ip, croupier_port, "hit " + name + " " + parts[1] + " " + parts[2]);
                    }
                } else if (parts[0].equalsIgnoreCase("stand")) {
                    if (!isRegistered) {
                        System.err.println("Player not registered.");
                        continue;
                    }
                    if (parts.length == 3) {
                        sendLines(croupier_ip, croupier_port, "stand " + name + " " + parts[1] + " " + parts[2]);
                    }
                } else if (parts[0].equalsIgnoreCase("doubledown")) {
                    if (!isRegistered) {
                        System.err.println("Player not registered.");
                        continue;
                    }
                    if (parts.length == 3) {
                        sendLines(croupier_ip, croupier_port, "doubleDown " + name + " " + parts[1] + " " + parts[2]);
                    }
                } else if (parts[0].equalsIgnoreCase("split")) {
                    if (!isRegistered) {
                        System.err.println("Player not registered.");
                        continue;
                    }
                    if (parts.length == 3) {
                        sendLines(croupier_ip, croupier_port, "split " + name + " " + parts[1] + " " + parts[2]);
                    }
                } else if (parts[0].equalsIgnoreCase("surrender")) {
                    if (!isRegistered) {
                        System.err.println("Player not registered.");
                        continue;
                    }
                    if (parts.length == 3) {
                        sendLines(croupier_ip, croupier_port, "surrender " + name + " " + parts[1] + " " + parts[2]);
                    }
                } else {
                    System.err.println("Unknown command.");
                    // print available commands
                    System.out.println("Commands:");
                    System.out.println("registerPlayer <ip> <port> [<name>]");
                    System.out.println("bet <amount>");
                    System.out.println("hit <deck> <card>");
                    System.out.println("stand <deck> <card>");
                    System.out.println("doubleDown <deck> <card>");
                    System.out.println("split <deck> <card>");
                    System.out.println("surrender <deck> <card>");
                    System.out.println("gameover");
                    System.out.println("quit");
                }
            }
            // Unregister player
            if (isRegistered) {
                sendLines(croupier_ip, croupier_port, "gameover");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private static final int packetSize = 4096;

    private static void receiveLines(int port) {
        try (DatagramSocket s = new DatagramSocket(port)) { // closes automatically
            byte[] buffer = new byte[packetSize];
            String line;
            do {
                DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                s.receive(p);
                line = new String(buffer, 0, p.getLength(), StandardCharsets.UTF_8);
                if (line.startsWith("registration successful")) {
                    isRegistered = true;
                    System.out.println("Registration successful.");
                } else if (line.startsWith("registration failed")) {
                    isRegistered = false;
                    System.err.println("Registration failed.");
                    System.err.println(line.substring(20));
                } else if (line.startsWith("bet accepted")) {
                    System.out.println("Bet accepted.");
                } else if (line.startsWith("bet declined")) {
                    System.err.println("Bet declined.");
                    System.err.println(line.substring(13));
                } else if (line.startsWith("gameover")) {
                    System.err.println("Game over.");
                    System.err.println(line.substring(9));
                } else if (line.startsWith("action accepted")) {
                    System.out.println("Action accepted.");
                } else if (line.startsWith("action declined")) {
                    System.err.println("Action declined.");
                    System.err.println(line.substring(16));
                } else if (line.startsWith("prize")) {
                    int prize = Integer.parseInt(line.substring(6));
                    if (prize > 0) {
                        System.out.println("You won " + prize + " chips.");
                    } else if (prize == 0) {
                        System.out.println("You broke even.");
                    } else {
                        System.out.println("You lost " + (-prize) + " chips.");
                    }
                    // Confirm prize
                    sendLines(croupier_ip, croupier_port, "prize accepted " + name);
                } else if (line.startsWith("{")) {
                    // Received card as JSON message
                    Card card = Card.fromJSON(line);
                    printCard(card);
                    // Confirm card
                    sendLines(croupier_ip, croupier_port,
                            "player " + name + " received " + card.getDeck() + " " + card.toString());
                } else {
                    System.err.println("Received unknown message: " + line);
                }
            } while (!line.equalsIgnoreCase("quit"));
        } catch (IOException e) {
            System.err.println("Unable to receive message on port \"" + port + "\".");
        }
    }

    private static void printCard(Card card) {
        System.out.println("Card: " + card.toString());
        System.out.println("Deck: " + card.getDeck());
        System.out.println("Owner: " + card.getOwner());
    }

    private static void sendLines(InetAddress dest_ip, int dest_port, String message) {
        try (DatagramSocket s = new DatagramSocket()) { // closes automatically
            byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket p = new DatagramPacket(buffer, buffer.length, dest_ip, dest_port);
            s.send(p);
        } catch (IOException e) {
            System.err.println("Unable to send message to \"" + dest_ip.getHostAddress() + "\".");
        }
    }

    private static void register(String c_ip, String c_port) {
        try {
            croupier_ip = InetAddress.getByName(c_ip);
        } catch (UnknownHostException e) {
            System.err.println("Invalid IP address for croupier, while parsing.");
            return;
        }
        croupier_port = Integer.parseInt(c_port);
        String message = "registerPlayer " + ip + " " + port + " " + name;
        sendLines(croupier_ip, croupier_port, message);
    }
}
