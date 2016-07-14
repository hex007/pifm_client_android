package com.hex.riplay.Items;

import android.support.annotation.NonNull;

import java.io.Serializable;

/**
 * Created on 08/07/16.
 */
public class Song implements Serializable, Comparable<Song> {
	public final String name;
	public final int index;


	public Song(int index, String name) {
		this.name = name;
		this.index = index;
	}


	@Override
	public int compareTo(@NonNull Song song) {
		return Integer.compare(index, song.index);
	}
}
