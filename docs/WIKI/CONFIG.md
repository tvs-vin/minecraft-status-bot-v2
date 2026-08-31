# CONFIG

All the different options for the glorious mcsb.config file

IN PROGRESS

## Nessesary options

### mode | String
This option specifies what mode to run in
    - 1. Standalone
    - 2. Manager
    - 3. Worker
    - 4. Hybrid

### nodeName | String
This specfies the name of the node, will be used whenever the node is refered to

## Web UI

Set `webUI=true` to start the embedded Javalin web server when the mod loads.
The server listens on `webUIPort` (default: `8080`). The generated configuration
file is located at `config/mcsb.properties`.

Available endpoints:

- `/` - Basic running message
- `/health` - Returns `ok` when the server is running
- `/api/status` - Returns the configured node name and mode as JSON
