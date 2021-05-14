import java.io.*;
import java.net.*;

/*
 * with code from http://books.msspace.net/mirrorbooks/irchacks/059600687X/irchks-CHP-5-SECT-5.html
 */

public class IRCExample {
    public static void main(String[] args) throws Exception {

        // The server to connect to and our details.
        String server      = "irc.longislandweizen.de";
        int    port        = 7012; //default port typically 6667
        String server_pass = "wwu"; //most public IRCs have no password
        String nick        = "Joschka|bot";
        String user        = "joschkauser";

        // The channel which the bot will join.
        String channel = "#Welcome";

        // Connect directly to the IRC server.
        Socket socket = new Socket(server, port);

        // Can only use PrintWriter and println, newline is added automatically
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream( )));

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream( )));

        // Log on to the server.
        writer.write("PASS " + server_pass + "\r\n");
        writer.write("NICK " + nick + "\r\n");
        writer.write("USER " + user + " irc.longislandweizen.de * :Joschka Hüllmann\r\n");
        writer.flush();

        // Read lines from the server until it tells us we have connected.
        String line = null;
        while ((line = reader.readLine( )) != null) {
            System.out.println(line);
            if (line.indexOf("004") >= 0) {
                // We are now logged in.
                break;
            }

            else if (line.indexOf("433") >= 0) {
                System.out.println("Nickname is already in use.");
                return;
            }

            else if (line.startsWith("PING ")) {
                writer.write("PONG " + line.substring(5) + "\r\n");
                writer.flush();
            }
        }

        // Join the channel.
        writer.write("JOIN " + channel + "\r\n");
        writer.flush();

        // Keep reading lines from the server.
        while ((line = reader.readLine( )) != null) {
            if (line.startsWith("PING ")) {
                // We must respond to PINGs to avoid being disconnected.
                writer.write("PONG " + line.substring(5) + "\r\n");
                writer.write("PRIVMSG " + channel + " :I got pinged!\r\n");
                writer.flush();
            } else {
                // Print the raw line received by the bot.
                System.out.println(line);
            }
        }
    }
}
