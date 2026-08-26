# minecraft-status-bot-v2
A new and improved MCSB

Plan for MCSB-V2
 - [ ] Configurable from a JSON file
 - [ ] Run in 4 different modes
   - [ ] Standalone
     - [ ] Run entirely from a .JAR inside the minecraft server
     - [ ] Gives all the data you could want about it
     - [ ] Has API to tie in other mods info (I.E Listing players with nicknames)
   - [ ] Manager
     - [ ] Runs outside of the minecraft instance (Maybe from the same jarfile?)
     - [ ] Accepts connections from other clients configured to have a Manager
     - [ ] Has a WEBUI to configure and manage properly
   - [ ] Worker
     - [ ] Connects to a Manager
     - [ ] Does not connect to discord itself, only to manager
     - [ ] Gathers info about the server its installed on, sends and or logs it with Manager
   - [ ] Hybrid
     - [ ] Has all the Manager Features
     - [ ] Also acts like a Worker, automatically configured to send info to itself
 - [ ] Be able to assign MC usernames to Discord names
 - [ ] Allow chat passthough
   - [ ] Configurable based on mode
   - [ ] Have private team chats show in teams channels on discord
