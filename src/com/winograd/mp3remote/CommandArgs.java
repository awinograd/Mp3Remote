package com.winograd.mp3remote;

import java.util.ArrayList;

public class CommandArgs {
	public String command;
	public String state;
	public int volume;
	//set default to -1 so we know when position isn't actually sent
	//a 0 default would give false positives when checking if position was sent
	public int position = -1;
	public String title;
	public String artist; 
	public String album; 
	public String message;
	public ArrayList<Song> songs;
}
