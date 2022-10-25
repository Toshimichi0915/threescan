# threescan

The most powerful Minecraft server scanner in the world.

## Usage

```bash
java -jar threescan.jar <type> <file/stdin> <timeout> <thread>
```

Currently, there are 3 types: 'simple', 'range', 'masscan'.

### Simple

This type is used to scan a single host.

```text
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
