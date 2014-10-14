package dataGS;

public class ChannelDescription {
/*
id: numeric value
title: human readable title for the channel
description: optional more text about the channel
units: short label that goes after the number
precision: number of places to the right of the decimal. If 0, decimal point is supressed. If negative, round to that many places to the left of the decimal. IE -1 is round to the nearest 10.
sortOrder: order in which to initially display. Not guaranteed to be unique. Deal with it. 
 */
	
	public String id;
	public String title;
	public String description;
	public String units;
	public int precision;
	public int sortOrder;
	public boolean history;
	public boolean log;
	
	
	public static enum Modes {
		AVERAGE, SAMPLE
	};
	
	public Modes mode;
	
	public ChannelDescription(String id, Modes mode, String title, String description, String units, int precision, int sortOrder, boolean history, boolean log) {
		this.id=id;
		this.mode=mode;
		this.title=title;
		this.description=description;
		this.units=units;
		this.precision=precision;
		this.sortOrder=sortOrder;
		this.history=history;
		this.log=log;
	}
	
	public ChannelDescription(String id) {
		this.id=id;
		this.mode=Modes.SAMPLE;
		title="";
		description="";
		units="";
		precision=2;
		sortOrder=0;
		history=false;
		log=false;
	}
	
	public void setMode(Modes m) {
		mode=m;
	}
	
	public void setTitle(String t) {
		title=t;
	}
	
	public void setDescription(String d) {
		description=d;
	}
	
	public void setUnits(String u) {
		units=u;
	}
	
	public void setPrecision(int p) {
		precision=p;
	}
	
	public void setSortOrder(int s) {
		sortOrder=s;
	}
	

	public String toString() {
		String s="### Channel id: " + id + "\n";
		s += "# mode: " + mode + "\n";
		s += "# title: " + title + "\n";
		s += "# description: " + description + "\n";
		s += "# units: " + units + "\n";
		s += "# precision: " + precision + "\n";
		s += "# sortOrder: " + sortOrder;
		return s;
	}
	
}
