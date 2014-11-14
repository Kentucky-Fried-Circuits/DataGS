function magnumLowVACDropout(val,iModel) {
	if ( null == iModel )
		expor=0;
	else
		expor=magnumInverterModel(iModel,3); //Need to get this one too

	if ( 1 == expor ) {
		switch (val) {
			case 110: return 110;
			case 122: return 120;
			case 135: return 130;
			case 145: return 140;
			case 155: return 150;
			case 165: return 160;
			case 175: return 170;
			case 182: return 180;
			case 190: return 190;
			case 192: return "UPS Mode";
			case 254: return "EMS over-ride open relay";
			case 255: return "EMS over-ride open relay";
			default: return "Export Unknown Value: " + val;
		}
	} else {
		switch (val) {
			case 110: return 60;
			case 122: return 65;
			case 135: return 70;
			case 145: return 75;
			case 155: return 80;
			case 165: return 85;
			case 175: return 90;
			case 182: return 95;
			case 190: return 100;
			case 192: return "UPS Mode";
			case 254: return "EMS over-ride open relay";
			case 255: return "EMS over-ride open relay";
			default: return "Unknown Value: " + val;
		}
	}
}

function timeHuman(val, sec) {
	if ( typeof( sec ) === 'undefined' ) sec = 1;
	var h = val.substring(0,0+2);
	var m = val.substring(3,3+2);
	var s = val.substring(6,6+2);

	if ( sec ) {
		if ( h > 0 )
			return "["+h+"h "+m+"m "+s+"s]";
		if ( m > 0 )
			return "["+m+"m "+s+"s]";
	} else {
		if ( h > 0 )
			return "["+h+"h "+m+"m]";
		if ( m > 0 )
			return "["+m+"m]";

	}

	return s+" seconds";
}

function magnumInverterModel(val,voltage) {
	m='';
	switch ( val ) {
		// v=base voltage, s=scale factor from 12v, e=export, m=model, l=loadAmps, i=inputAmps, 2=inputAC2, 1=inputAC1, c=invChgAmps
		//             v  s e m     l i 2 1 c
		case 0x06: m= "12|1|0|MM612|0|0|0|0|0"; break;
		case 0x07: m= "12|1|0|MM612-AE|0|0|0|0|0"; break;
		case 0x08: m= "12|1|0|MM1212|0|0|0|0|0"; break;
		case 0x09: m= "12|1|0|MMS1012|0|0|0|0|0"; break;
		case 0x0A: m= "12|1|1|MM1012E|0|0|0|0|0"; break;
		case 0x0B: m= "12|1|0|MM1512|0|0|0|0|0"; break;
		case 0x0C: m= "12|1|1|MMS912E|0|0|0|0|0"; break;
		case 0x0D: m= "12|1|1|MMA1012|0|0|0|0|0"; break;
		case 0x0F: m= "12|2|0|ME1512|0|0|0|0|0"; break;
		case 0x14: m= "12|2|0|ME2012|0|0|0|0|0"; break;
		case 0x15: m= "12|1|0|RS2212|0|0|0|0|0"; break;
		case 0x19: m= "12|2|0|ME2512|0|0|0|0|0"; break;
		case 0x1E: m= "12|2|0|ME3112|0|0|0|0|0"; break;
		case 0x23: m= "12|1|0|MS2012|0|0|0|0|0"; break;
		case 0x24: m= "12|1|1|MS1512E|0|0|0|0|0"; break;
		case 0x28: m= "12|1|1|MS2012E|0|0|0|0|0"; break;
		case 0x2C: m= "12|1|0|MSH3012M|1|1|0|1|1"; break;
		case 0x2D: m= "12|1|0|MS2812|0|0|0|0|0"; break;
		case 0x2F: m= "12|1|1|MS2712E|0|0|0|0|0"; break;
		case 0x35: m= "24|1|1|MM1324E|0|0|0|0|0"; break;
		case 0x36: m= "24|1|0|MM1524|0|0|0|0|0"; break;
		case 0x37: m= "24|1|0|RD1824|0|0|0|0|0"; break;
		case 0x3B: m= "24|1|1|RD2624E|0|0|0|0|0"; break;
		case 0x3F: m= "24|1|0|RD2824|0|0|0|0|0"; break;
		case 0x45: m= "24|1|1|RD4024E|0|0|0|0|0"; break;
		case 0x4A: m= "24|1|0|RD3924|0|0|0|0|0"; break;
		case 0x59: m= "24|1|1|MS4124PE|0|0|0|0|0"; break;
		case 0x5A: m= "24|1|1|MS4124E|0|0|0|0|0"; break;
		case 0x5B: m= "24|1|0|MS2024|0|0|0|0|0"; break;
		case 0x67: m= "24|1|0|MSH4024|1|1|0|1|1"; break;
		case 0x68: m= "24|1|0|MSH4024M|1|1|0|1|1"; break;
		case 0x69: m= "24|1|0|MS4024|0|0|0|0|0"; break;
		case 0x6A: m= "24|1|0|MS4024AE|0|0|0|0|0"; break;
		case 0x6B: m= "24|1|0|MS4024PAE|1|1|0|0|1"; break;
		case 0x6C: m= "48|1|0|INV_48V|0|0|0|0|0"; break;
		case 0x6F: m= "48|1|0|MS4448AE|0|0|0|0|0"; break;
		case 0x70: m= "48|1|0|MS3748AEJ|0|0|0|0|0"; break;
		case 0x72: m= "48|1|0|MS4048|0|0|0|0|0"; break;
		case 0x73: m= "48|1|0|MS4448PAE|1|1|0|0|1"; break;
		case 0x74: m= "48|1|0|MS3748PAEJ|1|1|0|0|1"; break;
		case 0x75: m= "48|1|0|MS4348PE|0|0|0|0|0"; break;
		default: m="12|1|0|Unknown Value: 0x"+val.toString(16)+" ("+val+")|1|1|1|1|1";
	}
	var parts=m.split("|");
	if ( 1==voltage ) return parts[0]; /* nominal DC voltage */
	if ( 2==voltage ) return parts[1]; /* scale factor for AC output */
	if ( 3==voltage ) return parts[2]; /* 1 if export, 0 if non-export */
	if ( 4==voltage ) return parts[4]; /* 1 if inverter has loadAmps, 0 if not */
	if ( 5==voltage ) return parts[5]; /* 1 if inverter has inputAmps, 0 if not */
	if ( 6==voltage ) return parts[6]; /* 1 if inverter has inputAC2, 0 if not */
	if ( 7==voltage ) return parts[7]; /* 1 if inverter has inputAC1, 0 if not */
	if ( 8==voltage ) return parts[8]; /* 1 if inverter has invChgAmps, 0 if not */
	
	return parts[3];
}

function magnumInverterStackMode(val) {
	switch (val) {
		case 0x00: return "Standalone Unit";
		case 0x01: return "Parallel Stack Master";
		case 0x02: return "Parallel Stack Slave";
		case 0x04: return "Series Stack Master";
		case 0x08: return "Series Stack Slave";
		default: return "Unknown Value: "+val;
	}
}

function ags_type(rev, stop_vdc){
	//console.log(rev+" "+stop_vdc);
	if ( null != rev )
		rev = parseFloat(rev);

	if ( null != stop_vdc )
		stop_vdc = parseFloat(stop_vdc);

	if ( 0 != rev && null != rev && null == stop_vdc ) {
		return "RC"
	} else if ( 0 != rev && null != rev ) {
		return "ARC";
	} else {
		
		return "none";

	}
	return "none";

}

function magnumInverterStatus(val) {
	mis=[];

	mis[0x00]=['Charger Standby','AC in, charging disabled.'];
	mis[0x01]=['Equalize Mode','Equalizing with AC.'];
	mis[0x02]=['Float Mode','Floating with AC.'];
	mis[0x04]=['Absorb Mode','Absorbing with AC.'];
	mis[0x08]=['Bulk Mode','Bulk Charging with AC.'];
	mis[0x09]=['Battery Saver Mode','Battery full. No charging current.'];
	mis[0x10]=['Charge Mode, no AC','Unit is in charger mode, but no AC applied.'];
	mis[0x20]=['Off','Inverter off. Charger waiting for user input. No AC pass through.'];
	mis[0x40]=['Inverting',''];
	mis[0x50]=['Inverter Standby',''];
	mis[0x80]=['Search Mode','Searching for a load. Charger may be on or off.'];

	return mis[val][0]+"<br>"+mis[val][1];
	
}

function magnumChargerLED(val) {
	if ( 0==val ) return 'Charging LED off';
	if ( 1==val || val>14 ) return 'Charging LED on';
	if ( val >= 7 ) return 'Charging LED Flashing';
	if ( val >= 2 ) return 'Charging LED Slowly Flashing';
	return 'Invalid Charging LED value: ' + val;
}

function magnumInverterLED(val) {
	if ( 0==val ) return 'Inverting LED off';
	if ( 1==val || val>14 ) return 'Inverting LED On';
	if ( val >= 7 ) return 'Inverting LED Flashing';
	if ( val >= 2 ) return 'Inverting LED Slowly Flashing';
	return 'Invalid Inverting LED value: ' + val;
}


function magnumAGSQuietTime(val) {
	switch (val) {
		case 0: return "Disabled.";
		case 1: return "21:00 to 07:00"; // 9pm to 7am
		case 2: return "21:00 to 08:00";
		case 3: return "21:00 to 09:00";
		case 4: return "22:00 to 08:00";
		case 5: return "23:00 to 08:00";
	}
}

function magnumAGSTemperature(tempF) {
	if ( 0 == tempF ) /* actually, "invalid" */
		return "Not connected.";
	if ( 250 == tempF )
		return "&gt; 40&deg;C / 104&deg;F";
	if ( 251 == tempF ) 
		return "&lt; 1&deg;C / 33&deg;F";
	if ( 252 == tempF )
		return "Not connected.";

	tempC=(tempF-32.0)*(5.0/9.0);
	return tempC.toFixed(0)+"&deg;C / "+tempF.toFixed(0)+"&deg;F";
}

function magnumAGSStatus(val) {
	switch (val) {
		case  0: return "Not Valid";
		case  1: return "Off";
		case  2: return "Ready";
		case  3: return "Manual Run";
		case  4: return "AC In";
		case  5: return "In Quiet Time";
		case  6: return "Start in Test Mode";
		case  7: return "Start on Temperature";
		case  8: return "Start on Voltage";
		case  9: return "Fault during Start on Test";
		case 10: return "Fault during Start on Temp";
		case 11: return "Fault during Start on Voltage";
		case 12: return "Start Time of Date";
		case 13: return "Start State of Charge";
		case 14: return "Start Exercise";
		case 15: return "Fault during Start for Time of Day";
		case 16: return "Fault during Start for State of Charge";
		case 17: return "Fault during Start for Exercise";
		case 18: return "Start on Amp";
		case 19: return "Start on Top Off";
		case 20: return "Not Used";
		case 21: return "Fault during Start on Amp";
		case 22: return "Fault during Top Off";
		case 23: return "Not Used";
		case 24: return "Fault on Maximum Generator Run";
		case 25: return "Generator Run Fault";
		case 26: return "Generator in Warm Up";
		case 27: return "Generator in Cool Down";
		default: return "Unknown Mode: " + val;
	}
}

function magnumTemperature(tempC) {
	var tempF=9.0/5.0*tempC + 32.0;

	if ( tempC > 0 ) 
		return tempC.toFixed(0)+"&deg;C / "+tempF.toFixed(0)+"&deg;F";

	return "<u>&lt;</u>"+tempC.toFixed(0)+"&deg;C / <u>&lt;</u>"+tempF.toFixed(0)+"&deg;F";

}

function magnumAGSMode(val) {
	switch (val) {
		case 0: return "Off";
		case 1: return "Enabled";
		case 2: return "Test";
		case 4: return "Enabled with Quiet Time";
		case 5: return "On";
		default: return "Unknown Value: " + val;
	}
}

function magnumExportVoltageScale(vin,vScale) {
	if ( 2 != vScale )
		return vin;
	return Math.round((vin-5.0)*2.0);
}

function magnumInverterFault(val) {
	switch ( val ) {
		case 0x00: return "No Fault on Inverter";
		case 0x01: return "Stuck Relay";
		case 0x02: return "DC Bridge Overload";
		case 0x03: return "AC Output Overload";
		case 0x04: return "Charging a Dead Battery";
		case 0x05: return "AC Backfeed";
		case 0x08: return "Low Battery Cutout";
		case 0x09: return "High Battery Cutout";
		case 0x0A: return "High AC Output Voltage";
		case 0x10: return "Bad FET Bridge";
		case 0x12: return "FETs too Hot";
		case 0x13: return "FETs too Hot too Fast";
		case 0x14: return "Internal Fault 4";
		case 0x16: return "Stacker Fault, both units not in same mode, problem with *other* inverter";
		case 0x18: return "Stacker sync clocks are not in phase";
		case 0x17: return "Stacker does not detect other stackers sync clock";
		case 0x19: return "Stacker AC output voltage is not properly phased";
		case 0x20: return "Over Temperature Shutdown";
		case 0x21: return "Transfer Relay is not Closed in Charge Mode";
		case 0x80: return "Bridge Fault in Charge Mode. Max PWM, no AC amps, Vbat < FloatV";
		case 0x81: return "High Battery Temperature";
		case 0x90: return "Open Transformer TCO Temperature Cutout";
		case 0x91: return "Open Input 30 amp AC Breaker CB3";
		default: return "Unknown Fault: Code" + val;
	}
}
