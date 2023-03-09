# threescan

The most powerful Minecraft server scanner in the world.

## Usage

```bash
java -jar threescan.jar <type> <mode> <timeout> <rate> <name> <uniqueId>
```

where

* type - the type of scan (simple, range, masscan, threescan)
* mode - the mode of scan (fast, full)
* timeout - the timeout of scan (in milliseconds)
* rate - the rate of scan (scan per second)
* name - the name of a player (used to determine server type)
* uniqueId - the uniqueId of a player (used to determine server type)

## Features

* zero memory copy
* asynchronous I/O
* ability to determine server type

## Types

Currently, there are 4 types: "simple", "range", "masscan", and "threescan".

### Simple

This type is used to scan a single host.

```text
<host>
127.0.0.1
<host> <port>
127.0.0.1 25565
<host> <portStart> <portEnd>
127.0.0.1 25565 25565
```

### Range

This type is used to scan a range of hosts.

```text
<CIDR> <portStart> <portEnd>
127.0.0.1/24 25565 25565

<ipv4Start>-<ipv4End> <portStart> <portEnd>
127.0.0.1-127.0.0.255 25565 25565

<ipv4> <portStart> <portEnd>
127.0.0.1 25565 25565
```

### Masscan

This type is used to combine the program with Masscan.

```text
Discovered open port <port>/tcp on <ipv4>
Discovered open port 25565/tcp on 127.0.0.1
```

### Threescan

This type is used to recheck the scan results of threescan.

```text
{"host":"<host>", "port":<port>, ...}
{"host":"127.0.0.1", "port":25565, ...}
```

## Modes

Currently, there are 2 modes: "fast" and "full".

With full mode, additional packets are sent to determine server types. However, scans will be slower and a message will be shown to scanned hosts.
