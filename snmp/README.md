#Enabling SNMP view of MagWebPro data
1. packages needed
	snmpd libsnmp-perl (snmp if we want to snmpwalk locally)
2. open UDP port 161
3. put snmpd.conf in /etc/snmp/snmbd.conf

#If SNMP server only
1. disable logging in startDataGS
2. firewall port 80 & 8080


#Setting up net-snmp for the pi

1. Install snmpd and net-snmp with the perl extention
	Net-snmp instructions can be found here: http://www.net-snmp.org/wiki/index.php/Net-Snmp_on_Ubuntu 
	Place the MIBs

2. Configure /etc/snmp/snmpd.conf
	Use the snmpd.conf found in this directory

3. Install JSON library found in this directory for perl

4. Add the magWebPro.pl to the pi in this directory: /usr/share/snmp/magWebPro.pl

5. restart snmpd with this command: `service snmpd restart`. The words ` * Restarting network management: hello world loaded` should appear indicating it was successful.

6. Do a SNMP walk for the OID: `.1.3.6.1.4.1.42934`. If everything is set up correctly, you should see values from your now.json page running from the pi.

 snmpwalk -Os -v 2c -m APRS-WORLD-MAGWEB-PRO-MIB -c aprs 192.168.10.216 iso



