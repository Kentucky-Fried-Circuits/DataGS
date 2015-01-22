#!/bin/bash

PWDS="$(pwd)"

cd src/dataGS

#find the line in which the firmware date is set and save it to STR
STR=$(grep -n 'private final static String FIRMWARE_DATE' DataGS.java );

NAMESTR=$(grep -n 'System.err.println("# Major version: " + FIRMWARE_DATE' DataGS.java );

#save today's date to DATE
DATE=`date +%Y-%m-%d`

COMPNAME=`hostname`;

#If the date is already there, there is no need to replace it again
if [[ $STR != *"$DATE"*  ]]
then
	#save the line number
	NUM=$( echo ${STR} | cut -d : -f 1);

	#save line number of the computername
	COMPNUM=$( echo ${NAMESTR} | cut -d : -f 1);
	#echo "$NUM"


	#replace the line number STR with a new line containing today's date
	sed -i "${NUM}s/.*/\tprivate final static String FIRMWARE_DATE = \"$DATE\";/" DataGS.java
	
	sed -i "${COMPNUM}s/.*/\t\tSystem.err.println(\"\# Major version: \" + FIRMWARE_DATE + \" (${COMPNAME})\");/" DataGS.java

	#return to root directory of DataGS
	cd $PWDS

	#recompile
	ant;

fi

cd $PWDS

cd utilities/DataGSJar

./makeJar
