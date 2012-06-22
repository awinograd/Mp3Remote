package com.winograd.mp3remote;

public class Song {
	public String title;
	public String artist; 
	public String album;
	public String filename;
	public int songNumber;
	
	public String toString(){
		String toReturn = title;
		
		if(artist != null){
			toReturn += " by " + artist;
		}
		
		return toReturn;
	}
}
