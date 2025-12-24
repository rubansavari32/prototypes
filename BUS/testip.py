#!/usr/bin/env python3
"""
make_achi_pkt.py
Generates ACHI_Network_Basic.zip with:
 - README.txt
 - router_R1_config.txt
 - server_setup.txt
 - pcs_ips.csv
 - topology_diagram.txt
 - ACHI_Network_Basic.pkt (placeholder text file explaining why it's not real .pkt)
This helps you recreate the topology inside Cisco Packet Tracer quickly.
"""

import os
import zipfile
import textwrap

OUT_DIR = os.path.join(os.getcwd(), "ACHI_Network_Basic")
ZIP_PATH = os.path.join(os.getcwd(), "ACHI_Network_Basic.zip")
os.makedirs(OUT_DIR, exist_ok=True)

readme = textwrap.dedent("""\
ACHI Network Basic - Packet Tracer Setup (Manual import)
======================================================

This package helps you recreate the ACHI company network in Cisco Packet Tracer.

Files included:
1) router_R1_config.txt  - CLI commands to run on Router R1 (paste into CLI)
2) server_setup.txt      - Steps to enable HTTP/DNS services on Server1 (Packet Tracer Server)
3) pcs_ips.csv           - IP addresses to assign to PCs (if you prefer static IPs)
4) topology_diagram.txt  - Simple ASCII diagram of the network
5) ACHI_Network_Basic.pkt - PLACEHOLDER: explanation & manual steps (not a real .pkt)
6) README.txt            - (this file)

Topology summary
----------------
- Router: R1 (GigabitEthernet0/0 -> Switch S1 Fa0/1)
- Switch: S1 (2960)
- PCs: PC1(.10), PC2(.11), PC3(.12)
- Server: 192.168.1.100 (HTTP + DNS)
- Printer: 192.168.1.50

Quick Recreation Steps
----------------------
1. Open Cisco Packet Tracer.
2. Add devices:
   - Router 2901 -> name it R1
   - Switch 2960 -> name it S1
   - 3 x Generic PCs -> PC1, PC2, PC3
   - Server -> Server1
   - Printer -> Printer1

3. Connect with Copper Straight-Through:
   - R1 G0/0  <--> S1 Fa0/1
   - PC1 Fa0  <--> S1 Fa0/2
   - PC2 Fa0  <--> S1 Fa0/3
   - PC3 Fa0  <--> S1 Fa0/4
   - Server1 Fa0 <--> S1 Fa0/5
   - Printer1 Fa0 <--> S1 Fa0/6

4. Router CLI:
   - Click R1 -> CLI tab -> paste contents of router_R1_config.txt

5. Server:
   - Click Server1 -> Services -> HTTP ON and DNS ON
   - Add DNS A record: achi-server.local -> 192.168.1.100

6. PCs:
   - Set to DHCP (Desktop -> IP Configuration -> DHCP) or assign static from pcs_ips.csv

7. Test:
   - From PC1 Command Prompt: ping 192.168.1.100
   - Open web browser: http://192.168.1.100

If you prefer, follow the router_R1_config.txt and server_setup.txt files directly.

Notes:
- The file "ACHI_Network_Basic.pkt" included here is a plain-text placeholder explaining why a real .pkt isn't included and giving manual re-creation steps.
""")

router_cfg = textwrap.dedent("""\
enable
configure terminal
hostname R1
no ip domain-lookup
interface GigabitEthernet0/0
 description Link-to-S1
 ip address 192.168.1.1 255.255.255.0
 no shutdown
exit

ip dhcp pool ACHI_POOL
 network 192.168.1.0 255.255.255.0
 default-router 192.168.1.1
 dns-server 8.8.8.8
exit

line con 0
 logging synchronous
exit

end
write memory
""")

server_setup = textwrap.dedent("""\
Packet Tracer Server1 setup (HTTP + DNS)
---------------------------------------

1) Click on Server1 -> Desktop -> IP Configuration
   - To set static:
       IP Address: 192.168.1.100
       Subnet Mask: 255.255.255.0
       Default Gateway: 192.168.1.1
       DNS Server: 8.8.8.8

2) Click on Server1 -> Services tab
   - HTTP: Turn ON
   - DNS: Turn ON
     - Add an A record:
         Name: achi-server.local
         Address: 192.168.1.100

3) Save and test:
   - PC browser -> http://192.168.1.100
""")

pcs_csv = "Device,IP,Subnet Mask,Gateway,DNS\nPC1,192.168.1.10,255.255.255.0,192.168.1.1,8.8.8.8\nPC2,192.168.1.11,255.255.255.0,192.168.1.1,8.8.8.8\nPC3,192.168.1.12,255.255.255.0,192.168.1.1,8.8.8.8\nPrinter1,192.168.1.50,255.255.255.0,192.168.1.1,8.8.8.8\nServer1,192.168.1.100,255.255.255.0,192.168.1.1,8.8.8.8\n"

topology = textwrap.dedent("""\
ASCII Topology Diagram
----------------------
           [Internet Cloud] (optional)
                   |
                 (none)
                   |
                R1 (192.168.1.1)
                   |
                 G0/0
                   |
                S1 (2960)
        --------------------------------
        |     |     |      |           |
       PC1   PC2   PC3   Server1     Printer1
(192.168.1.10 .. .12)  (192.168.1.100) (192.168.1.50)
""")

pkt_placeholder = textwrap.dedent("""\
ACHI_Network_Basic.pkt - PLACEHOLDER
===================================

IMPORTANT: This is NOT a real Cisco Packet Tracer .pkt file.
The .pkt format is proprietary. This placeholder explains why and gives exact
manual steps to recreate the workspace inside Packet Tracer.

If you need the actual .pkt file, the simplest options are:
 - Recreate the topology manually using the files in this ZIP (fast).
 - If you have Packet Tracer installed, you can paste the contents of router_R1_config.txt into the R1 CLI and follow server_setup.txt.

See README.txt for step-by-step instructions.
""")

# write files
files = {
    "README.txt": readme,
    "router_R1_config.txt": router_cfg,
    "server_setup.txt": server_setup,
    "pcs_ips.csv": pcs_csv,
    "topology_diagram.txt": topology,
    "ACHI_Network_Basic.pkt": pkt_placeholder
}

for fname, content in files.items():
    path = os.path.join(OUT_DIR, fname)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)

# create zip
with zipfile.ZipFile(ZIP_PATH, "w", zipfile.ZIP_DEFLATED) as z:
    for fname in files:
        z.write(os.path.join(OUT_DIR, fname), arcname=fname)

print("Created:", ZIP_PATH)
print("Open the ZIP and follow README.txt to recreate the topology in Packet Tracer.")
