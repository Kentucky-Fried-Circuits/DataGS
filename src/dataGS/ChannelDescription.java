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
	
	public int id;
	public String title;
	public String description;
	public String units;
	public int precision;
	public int sortOrder;
	
}
