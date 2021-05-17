import requests
import socket
import sys

def main():
  # server details
  server      = "irc.freenode.org"
  port        = 6667   # default port for IRC is typically 6667
  server_pass = "wwu"  # most public IRCs have no password, so I commented this out later
  nick        = "CACS|bot"
  user        = "CACSuser"
  whitelist   = ("joschi", "joschi2") # do not respond to users other than in the whitelist

  channel = "#CACStest1" # channel to join

  # Create TCP/IP socket and connect to server
  sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  sock.connect((server, port))
  client = sock.makefile("rwb")

  # Register our connection
  #irc.send("PASS {}\r\n".format(server_pass))
  sendMessage(client, "USER {} irc.longislandweizen.de * :Max Mustermann".format(user))
  sendMessage(client, "NICK {}".format(nick))

  # Make sure we are connected
  for line in client:
    line = line.decode("UTF-8").strip()
    print(line)
    if "004" in line:
      break
    elif "433" in line:
      print("ERROR: Nickname already in use, shutting down.")
      return
    elif line.startswith("PING"):
      sendMessage(client, "PONG {}".format(line[5:]))

  # Join channel
  sendMessage(client, "JOIN {}".format(channel))

  for line in client:
    line = line.decode("UTF-8").strip()
    print(line)
    if line.startswith("PING"):
      sendMessage(client, "PONG {}".format(line[5:]))
      sendMessage(client, "PRIVMSG {} :I got pinged!".format(channel))
    else:
      parts = line.split(" ", 4)

      # some basic sanity checks
      if len(parts) != 4 \
        or len(parts[0]) < 2 \
        or parts[1] != "PRIVMSG" \
        or len(parts[2]) < 1 \
        or len(parts[3]) < 2:
        continue # not a valid PRIVMSG, we ignore other messages

      # parse message
      sender = parseSender(parts[0][1:]) # remove colon and hostname
      type = parts[1]
      recipient = parts[2]
      message = parts[3][1:] # remove colon

      # only react to users on the whitelist
      if sender not in whitelist:
        continue

      if recipient == nick:
        # we received a direct message from a user, and we want to respond with the same text
        sendMessage(client, "PRIVMSG {} :Thanks for messaging me, this is what you wrote: {}".format(sender,message))

      if message == "!weather":
        # we received weather message, retrieve current weather from the internet.
        weather = retrieveWeather()

        # check if it was a direct message and set recipient for response accordingly
        if recipient == nick:
          target = sender # direct message
        else:
          target = recipient # channel message

        # send response
        sendMessage(client, "PRIVMSG {} :Weather: {}".format(target,weather))



# This function encapsulates sending and flushing the message buffer, appending newline, encoding, and printing to console.
def sendMessage(client, message):
  client.write("{}\r\n".format(message).encode("UTF-8"))
  client.flush()
  print(message)



# This function retrieves the current weather from a public API.
def retrieveWeather():
  response = requests.get('https://goweather.herokuapp.com/weather/M%C3%BCnster')
  if response.status_code != 200:
    return "ERROR while retrieving weather"
  json = response.json()
  return "Die Temperatur in Muenster ist {} mit Wind von {}. Beschreibung: {}.".format(json["temperature"], json["wind"], json["description"])


# Some networks do not allow sending messages to full hostname, so split the nickname apart.
def parseSender(sender):
  index = sender.find("!")
  if index != -1:
    return sender[0:index]
  else:
    return sender



if __name__ == "__main__":
  main()

