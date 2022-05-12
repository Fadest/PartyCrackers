# Party Crackers

This is a trial project made for [Meraki Studios](https://www.merakistudios.eu/), the guidelines for this project are
located in [this link](https://docs.google.com/document/d/1aJMZBo-iByt0O6xmcxGsi-_NlN1uG5jYzN4zBVtU5hc/edit).

## Commands

- /partycracker give <player> <type> (amount) » Give a player a certain amount of a Party Cracker type
- /partycracker list » List all Party Crackers
- /partycracker reload » Reload the messages and party crackers (Using this command will forcefully end the current
  party crackers)
- /partycracker info <type>» Get information about a Party Cracker type

## Tracked Time

I spent 6 hours and 24 minutes on this project.

![Clockify time spent screenshot](https://i.imgur.com/1uzf8oU.png)

## Information

- This project was made using [Paper](https://papermc.io/) 1.18.2 and tested using the latest stable version.
- Full support for [MiniMessage](https://docs.adventure.kyori.net/minimessage/format.html), allowing the creation of
  fancy messages to be displayed in the chat, including RGB colors, gradients and hover/click events.
- No libraries or external plugins are required since the plugin makes use of Spigot's library loader, and dependencies
  are downloaded automatically when the plugin is enabled.
- Configuration files are loaded using the JSON format since it's the most efficient way to store multiple party
  crackers on different folders and files.
- The plugin is made for [Meraki Studios](https://www.merakistudios.eu/), and is not intended for use on other servers
  since there is no support for it.
- The plugin is licensed under the [MIT License](https://choosealicense.com/licenses/mit/).