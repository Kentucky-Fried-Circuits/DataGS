
	var alarmSound;
	var flashCount = 0;
	var booFlash = true;
	var booFault = false;
	var faultMsg="The inverter has faulted!";

	alarmSound = new Howl({
			urls: ['res/sounds/warning.wav'],
			loop: true
		});
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
	/* this toggles the background of a div to simulate flashing */
	function screenFlash() {
		booFlash = !booFlash;		
		if ( booFlash ) {
			$("#alarm").css("background","orange");	
		} else {
			$("#alarm").css("background","red");	
		}
		if ( booFault ){		
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
				console.log("BOOOOOP");
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
			console.log(alarmSound.isPlaying);
		} else {
			silence=false;
			$('#controlSilence').html('false');
			console.log(alarmSound.isPlaying);
			if ( !alarmSound.isPlaying && booFault ){
				alarmSound.isPlaying = true;
				alarmSound.play();
				
			}
		}

	}

	/* dismisses the alarm */
	function dismiss(){
		silence = false;
		$("#controlSilence").html("false");
		booFault=false;
		//$("#alarm").hide(); //This needs to be hidden outside of this function
		//$("#alarmMessage").html("<p>"+faultMsg+"</p>");
		$("#alarm").hide();
		alarmSound.stop();	

	}
