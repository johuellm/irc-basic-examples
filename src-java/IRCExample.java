import java.io.*;
import java.net.*;

/*
 * with code from http://books.msspace.net/mirrorbooks/irchacks/059600687X/irchks-CHP-5-SECT-5.html
 */

public class IRCExample {
    public static void main(String[] args) throws Exception {

        // The server to connect to and our details.
        String server      = "irc.freenode.org";
        int    port        = 6667;  // default port for IRC is typically 6667
        String server_pass = "wwu"; // most public IRCs have no password, so I commented this out later
        String nick        = "CACS|bot";
        String user        = "CACSuser";
        String[] whitelist = new String[]{"joschi", "joschi2"}; // do not respond to users other than in the whitelist.

        // The channel which the bot will join.
        String channel = "#CACStest1";

        // Connect directly to the IRC server.
        Socket socket = new Socket(server, port);

        // Use a reader to "read" (=access) the data from the remote IRC server. Use a writer to
        // write (=send) data to the remote IRC server.
        // Since IRC is a text-based protocol, you can also use PrintWriter and its method println.
        // Then the newline ("\r\n") is added automatically.
        // You can also use any of the other writers if you are aware of the necessary encodings.
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream()));

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

        // Log on to the server.
        // This is based on RFC 1459 section 4.1 Connection Registration
        // https://datatracker.ietf.org/doc/html/rfc1459#section-4.1
        // writer.write("PASS " + server_pass + "\r\n"); // most public IRC servers have no password
        writer.write("NICK " + nick + "\r\n");
        System.out.print("NICK " + nick + "\r\n");
        writer.write("USER " + user + " irc.longislandweizen.de * :Max Mustermann\r\n");
        System.out.print("USER " + user + " irc.longislandweizen.de * :Max Mustermann\r\n");
        writer.flush();

        // Read lines from the server until it tells us we have connected.
        String line = null;
        while ((line = reader.readLine( )) != null) {
            System.out.println(line);
            if (line.indexOf("004") >= 0) {
                // We are now logged in.
                break;
            } else if (line.indexOf("433") >= 0) {
                // Nickname is already in use
                return;
            } else if (line.startsWith("PING ")) {
                System.out.println(line);
                writer.write("PONG " + line.substring(5) + "\r\n");
                System.out.print("PONG " + line.substring(5) + "\r\n");
                writer.flush();
            }
        }

        // Join the channel.
        writer.write("JOIN " + channel + "\r\n");
        writer.flush();

        // Keep reading lines from the server and react in funny ways
        while ((line = reader.readLine( )) != null) {
            System.out.println(line);
            if (line.startsWith("PING ")) {
                // We must respond to PINGs to avoid being disconnected.
                writer.write("PONG " + line.substring(5) + "\r\n");
                System.out.print("PONG " + line.substring(5) + "\r\n");
                writer.write("PRIVMSG " + channel + " :I got pinged!\r\n");
                System.out.print("PRIVMSG " + channel + " :I got pinged!\r\n");
                writer.flush();
            } else {
                // split the message at whitespace
                String parts[]   = line.split(" ", 4);

                // some basic sanity checks
                if (parts.length != 4
                        || parts[0].length() < 2
                        || !parts[1].equals ("PRIVMSG")
                        || parts[2].length() < 1
                        || parts[3].length() < 2) {
                    // not a valid PRIVMSG, we ignore other messages
                    continue;
                }

                // parse message
                String sender    = parseSender(parts[0].substring(1)); // remove colon and hostname
                String type      = parts[1];
                String recipient = parts[2];
                String message   = parts[3].substring(1); // remove colon

                // only react to users on the whitelist
                if(!contains(whitelist, sender)) {
                    continue;
                }

                if(recipient.equals(nick)) {
                    // we received a direct message from a user, and we want to respond with the same text.
                    writer.write("PRIVMSG " + sender + " :Thanks for messaging me, this is what you wrote: "
                            + message + "\r\n");
                    writer.flush();
                    System.out.print("PRIVMSG " + sender + " :Thanks for messaging me, this is what you wrote: "
                            + message + "\r\n");
                }

                if(message.equals("!weather")) {
                    // we received weather message, retrieve current weather from the internet.
                    String weather = retrieveWeather();
                    String target;

                    // check if it was a direct message and set recipient for response accordingly
                    if(recipient.equals(nick)) {
                        target = sender; // direct message
                    } else {
                        target = recipient; // channel message
                    }

                    // send response
                    writer.write("PRIVMSG " + target + " :Weather: " + weather + "\r\n");
                    writer.flush();
                    System.out.print("PRIVMSG " + target + " :Weather: " + weather + "\r\n");
                }
            }
        }
    }

    /*
     * This function retrieves the current weather from a public API.
     * TODO: correctly parse JSON and format it nicely. For now it just prints the JSON.
     */
    private static String retrieveWeather() {
        try {
            // setup HTTP Connection
            URL url = new URL("https://goweather.herokuapp.com/weather/M%C3%BCnster");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP Error code : "
                        + conn.getResponseCode());
            }
            // Read response and build results string
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            conn.disconnect();

            // return weather
            return builder.toString();
        } catch (Exception e) {
            return "Exception in NetClientGet.";
        }
    }

    /*
     * Some networks do not allow sending messages to full hostname, so split the nickname apart.
     */
    private static String parseSender(String sender) {
        int index = sender.indexOf("!");
        if(index != -1) {
            return sender.substring(0,index);
        } else {
            return sender;
        }
    }

    /*
     * Basic loop over an array to check if a value is contained.
     */
    private static boolean contains(String array[], String value) {
        for(int i=0; i < array.length; i++) {
            if(array[i].equals(value)) {
                return true;
            }
        }
        return false;
    }
}
