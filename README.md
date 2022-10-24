# threescan

The most powerful Minecraft server scanner in the world.

## Usage

```bash
java -jar threescan.jar <type> <file/stdin> <timeout> <thread>
```

Currently, there are 3 types: 'simple', 'range', 'masscan'.

### Simple

This type is used to scan a single host. The IP address of the host is automatically resolved, so you can scan servers
using TCPShield.

```text
<host> <portStart> <portEnd>
127.0.0.1 25565 25565
```

### Range

This type is used to scan a range of hosts. This can't be used for scanning servers using TCPShield.

```text
<CIDR> <portStart> <portEnd>
127.0.0.1/24 25565 25565

<ipv4Start>-<ipv4End> <portStart> <portEnd>
127.0.0.1-127.0.0.255 25565 25565

<ipv4> <portStart> <portEnd>
127.0.0.1 25565 25565
```

### Masscan

This type is used to combine the program with Masscan. This can't be used for scanning servers using TCPShield.

```text
open tcp <port> <host>
open tcp 25565 127.0.0.1 1000000000
```
