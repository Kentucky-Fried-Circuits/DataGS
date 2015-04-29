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





##magWebPro.pl
```perl
#!/usr/bin/perl

use NetSNMP::agent (':all');
use NetSNMP::ASN qw(ASN_OCTET_STR ASN_INTEGER);
use JSON qw( decode_json );
use LWP::Simple;

#get json text with wget

my $json = `(wget http://localhost:8080/data/now.json -q -O -)`; #get('http://localhost:8080/data/now.json');
my $decoded = decode_json($json);
my @data = @{ $decoded->{'data'} };

my $default_value = "No Data";
my $json_value = $default_value;

#Not being used, but keeping it here incase I think of a use for it
#This will allow us to find the oid given the channel name
#sub getOIDChannelName {
#	#This hash contains the key -> value pair of channel -> oid
#	my %oidHash = ();
#	$oidHash{"1.3.6.1.4.1.42934.2.2"} = "i_ac_volts_out";
#	$oidHash{"1.3.6.1.4.1.42934.2.1"} = "i_ac_volts_in";


#        my ($oidChannel) = $_[0];
#        $oidHash{$oidChannel};
#}

sub hello_handler {
  my ($handler, $registration_info, $request_info, $requests) = @_;
  my $request;

#sets the values to the oid's

  for($request = $requests; $request; $request = $request->next()) {
    my $oid = $request->getOID();
    if ($request_info->getMode() == MODE_GET) {
     updateJson();
     if ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.1.0")) {
        # "i_ac_volts_in" 
        $request->setValue(ASN_OCTET_STR, sprintf("%s", getJsonVar("i_fault","sampleValue")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.2.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%s", getJsonVar("i_stack_mode","sampleValue")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.3.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%.1f", getJsonVar("i_temp_fet","avg")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.4.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%s", getJsonVar("i_status","sampleValue")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.5.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%.1f", getJsonVar("i_dc_amps","avg")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.6.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%.1f", getJsonVar("i_ac_volts_out","avg")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.7.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%.1f", getJsonVar("i_amps_out","avg")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.8.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%s", getJsonVar("i_revision","sampleValue")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.9.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%.1f", getJsonVar("i_amps_in","avg")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.10.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%.1f", getJsonVar("i_amps_out_charging","avg")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.11.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%.1f", getJsonVar("i_ac_volts_in","avg")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.12.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%.1f", getJsonVar("i_temp_battery","avg")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.13.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%.1f", getJsonVar("i_dc_power","avg")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.14.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%s", getJsonVar("i_model","sampleValue")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.16.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%.1f", getJsonVar("i_ac_volts_out_over_80","avg")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.17.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%.1f", getJsonVar("i_ac_hz","avg")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.18.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%.1f", getJsonVar("i_temp_transformer","avg")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.20.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%.1f", getJsonVar("i_amps_out_inverting","avg")));
      }
      elsif ($oid == new NetSNMP::OID(".1.3.6.1.4.1.42934.2.21.0")) {
        #"i_ac_volts_out" 
        $request->setValue(ASN_OCTET_STR, sprintf("%.1f", getJsonVar("i_dc_volts","avg")));
      }
 
    } elsif ($request_info->getMode() == MODE_GETNEXT) {
      if ($oid < new NetSNMP::OID('.1.3.6.1.4.1.42934.1.0')) {
        updateJson();
        $request->setOID('.1.3.6.1.4.1.42934.2.1.0');
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_fault','sampleValue')));
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.1.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_stack_mode','sampleValue')));
        $request->setOID('.1.3.6.1.4.1.42934.2.2.0');
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.2.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_temp_fet','avg')));
        $request->setOID('.1.3.6.1.4.1.42934.2.3.0');
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.3.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_status','sampleValue')));
        $request->setOID('.1.3.6.1.4.1.42934.2.4.0');
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.4.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_dc_amps','avg')));
        $request->setOID('.1.3.6.1.4.1.42934.2.5.0');
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.5.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_ac_volts_out','avg')));
        $request->setOID('.1.3.6.1.4.1.42934.2.6.0');
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.6.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_amps_out','avg')));
        $request->setOID('.1.3.6.1.4.1.42934.2.7.0');
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.7.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_revision','sampleValue')));
        $request->setOID('.1.3.6.1.4.1.42934.2.8.0');
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.8.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_amps_in','avg')));
        $request->setOID('.1.3.6.1.4.1.42934.2.9.0');
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.9.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_amps_out_charging','avg')));
        $request->setOID('.1.3.6.1.4.1.42934.2.10.0');
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.10.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_ac_volts_in','avg')));
        $request->setOID('.1.3.6.1.4.1.42934.2.11.0');
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.11.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_temp_battery','avg')));
        $request->setOID('.1.3.6.1.4.1.42934.2.12.0');
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.12.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_dc_power','avg')));
        $request->setOID('.1.3.6.1.4.1.42934.2.13.0');
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.13.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_model','sampleValue')));
        $request->setOID('.1.3.6.1.4.1.42934.2.14.0');
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.14.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_ac_volts_out_over_80','avg')));
        $request->setOID('.1.3.6.1.4.1.42934.2.16.0');
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.16.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_ac_hz','avg')));
        $request->setOID('.1.3.6.1.4.1.42934.2.17.0');
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.17.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_temp_transformer','avg')));
        $request->setOID('.1.3.6.1.4.1.42934.2.18.0');
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.18.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_amps_out_inverting','avg')));
        $request->setOID('.1.3.6.1.4.1.42934.2.20.0');
      }
      elsif ($oid == new NetSNMP::OID('.1.3.6.1.4.1.42934.2.20.0')) {
        $request->setValue(ASN_OCTET_STR, sprintf('%.1f', getJsonVar('i_dc_volts','avg')));
        $request->setOID('.1.3.6.1.4.1.42934.2.21.0');
      }
    }
  }
}

sub updateJson
{
  $json = `(wget http://localhost:8080/data/now.json -q -O -)`; #get('http://localhost:8080/data/now.json');
  $decoded = decode_json($json);
  @data = @{ $decoded->{'data'} };
}


# parameters $_[0] is the channel we want and $_[1] is the aggregated variable we want
sub getJsonVar
{
    	
	my ($channel) = $_[0];
        my ($mode) = $_[1];

	$json_value = $default_value;
        foreach my $d ( @data ) {
                if ( $d->{"channel"} eq $channel ) {
                        $json_value = $d->{$mode};
                }
        }
        $json_value;
}

my $agent = new NetSNMP::agent();
$agent->register("hello_world", ".1.3.6.1.4.1.42934",
                 \&hello_handler);

print "hello world loaded\n";


```
