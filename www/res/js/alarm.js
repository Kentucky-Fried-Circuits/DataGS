	// requires jquery

	var alarmSound;
	var flashCount = 0;
	var booFlash = true;
	var booFault = false;
	var testAlr=false;
	var noResponse = false;
	var oldData = false;
	var noData = false;
	var faultMsg="The inverter has faulted!";

	alarmSound = new Howl({
			urls: ['res/sounds/warning.wav'],
			loop: true
		});
	alarmSound.isPlaying = false;
	/* This starts the warning cycle */
	function warn(msg){
		
		flashCount = 0;
		
		if ( null == msg ){	
			faultMsg="The inverter has faulted!"; 
			$("#alarmMessage").html("The inverter has faulted!");
		} else {
			faultMsg=msg;
			$("#alarmMessage").html("FAULTS:"+msg);
		}

		$("#alarm").show();
		if ( !booFault ) {
			booFault = true;
			screenFlash();
			soundAlarm();
		}
		

	}

	function testAlarm() {

		if ( !testAlr ) {
			testAlr = true;
			customFaults(null);
		}

	}

	/* this toggles the background of a div to simulate flashing */
	function screenFlash() {
		booFlash = !booFlash;		
		if ( booFlash ) {
			$("#alarm").css("background","orange");	
		} else {
			$("#alarm").css("background","red");	
		}
		if ( booFault ) {		
			setTimeout(screenFlash,500);
		}

	}
	var sound_file_url = "res/sounds/warning.wav";
	var silence = false;
	/* plays sound */
	function soundAlarm() {
		//sound from: http://soundbible.com/1577-Siren-Noise.html. The sound is in the public domain and was edited by Ian W. Gibbs to be shorter.
		if ( isIE ) {
			if ( booFault && !silence) {			

				$('#sound_element').html("<embed src='"+sound_file_url+"' hidden=true autostart=true loop=false>");	
				setTimeout(soundAlarm,2000);
				
			}

		} else {

			if ( booFault && !silence) {	
				//console.log("BOOOOOP");
				alarmSound.play();
				alarmSound.isPlaying = true;
				//setTimeout(soundAlarm,2000);
			}

		}


	}
	
	/* called by the silence and unsilence buttons.  */
	function silenceAlarm(bool){
		if ( bool ) {
			silence=true;
			$('#controlSilence').html('true');
			alarmSound.stop();
			alarmSound.isPlaying = false;
			//console.log(alarmSound.isPlaying);
		} else {
			silence=false;
			$('#controlSilence').html('false');
			//console.log(alarmSound.isPlaying);
			if ( !alarmSound.isPlaying && booFault ){
				alarmSound.isPlaying = true;
				alarmSound.play();
				
			}
		}

	}

	/* dismisses the alarm */
	function dismiss(){
		testAlr = false;
		//silence = false;
		//$("#controlSilence").html("false");
		booFault=false;
		//$("#alarm").hide(); //This needs to be hidden outside of this function
		//$("#alarmMessage").html("<p>"+faultMsg+"</p>");
		$("#alarm").hide();
		alarmSound.stop();	

	}

	function faultMessages(val){

		var fault = "";

		switch ( val ) {
			case 0: 
				fault = "LOW BATTERY, ATTACH TO CHARGING SOURCE IMEDIATELY";
				break;
			case 1: 
				fault = "LOW BATTERY, ATTACH TO CHARGING SOURCE IMEDIATELY";
				break;
			case 2: 
				fault = "LOST COMMUNICATION WITH PRO-VERTER<br><span style=\"font-size:.75em;\">PRO-VERTER APPEARS TO BE DISCONNECTED</span>";
				break;
			case 3: 
				fault = "TRIGGERED BY TEST BUTTON<br><span style=\"font-size:.75em;\">CLICK DISMISS TO CLEAR</span>";
				break;
			case 4: 
				fault = "NO RESPONSE FROM SERVER!<br><span style=\"font-size:.75em;\">PLEASE CHECK TO MAKE SURE YOU ARE STILL CONNECTED TO NETWORK!</span>";
				break;
			case 5: 
				fault = "PRO-VERTER INTERNAL ERROR: DATA NOT UPDATING<br><span style=\"font-size:.75em;\">CONTACT SUPPORT OR POWER CYCLE PROVERTER</span>";
				break;
			case 6: 
				fault = "BMK IN FAULT MODE!";
				break;
			case 7: 
				fault = "GENERATOR START FAULT - CHECK FUEL LEVEL";
				break;
			case 8: 
				fault = "CHARGER FAULT - OVER TEMPERATURE - CLEAN PRO-VERTER VENTS AND/OR REDUCE CHARGE RATE";
				break;
			case 9: 
				fault = "PROVERTER NOT CONNECTED TO THE MAGNUM NETWORK<br><span style=\"font-size:.75em;\">OR MAGNUM DEVICES ARE POWERED OFF</span>";
				break;
			default:
				fault = "UNKNOWN FAULT";
				break;
		}
			
		return fault;
	}


	function customFaults(dataAr){

		//console.log("custom");
		/* return string */
		var s = "";
		if ( null != dataAr ) {
			/* Check for inverter faults */
			if ( 0x00 != parseInt(dataAr["i_fault"].sampleValue) ) {
				s += "<hr>";
				s += "INVERTER FAULT: "+magnumInverterFault(parseInt(dataAr["i_fault"].sampleValue));
			}

			/* Check for AGS faults */
			if ( -1 != magnumAGSStatus(parseInt(dataAr['a_status'].sampleValue)).indexOf("Fault") ) {
				s += "<hr>";
				s += "AGS FAULT: "+magnumAGSStatus(parseInt(dataAr['a_status'].sampleValue));
			}



			
			if ( parseInt(dataAr["age_bmk"].sampleValue) < 100 ) {

				/* Critical Battery */
				if ( dataAr["b_dc_volts"].min - 1.0 <= parseFloat( dataAr["r_low_batt_cut_out"].sampleValue ) 
										&& parseInt(dataAr["i_fault"].sampleValue) != 0x08 ) {
					s += "<hr>";
					s += faultMessages(0);
				}

				/* Check for inverter faults */
				if ( 1 != parseInt(dataAr['b_fault'].sampleValue) ) {
					s += "<hr>";
					s += faultMessages(6);
				
				}

			} else if ( parseInt(dataAr["age_inverter"].sampleValue) < 100 ) {

				if ( dataAr["i_dc_volts"].min - 1.0 <= parseFloat( dataAr["r_low_batt_cut_out"].sampleValue ) 
										&& parseInt(dataAr["i_fault"].sampleValue) != 0x08 ) {
					s += "<hr>";
					s += faultMessages(1);
				}

			}



			//console.log(dataAr);
			/* no connection with Inverter */
			if ( parseInt(dataAr["age_inverter"].sampleValue) > 100 ) {
				s += "<hr>";
				s += faultMessages(9);
			}

			/* Data old */
		}

		/* test alarm */
		if ( testAlr ) {
			s += "<hr>";
			s += faultMessages(3);
		}

		/* No response from server */
		if ( noResponse ) {
			s += "<hr>";
			s += faultMessages(4);
		}

		/* same packet as last time */
		if ( oldData ) {
			s += "<hr>";
			s += faultMessages(5);
		}

		/* empty array returned in JSON */
		if ( noData ) {
			s += "<hr>";
			s += faultMessages(2);
		}


		if ( "" == s ) {
			//console.log("Fault Free");
			dismiss();
			return false;
		} else {
			warn(s);
			alarmUp();
			location.href = "#alarm";
			return true;
		}

	}
