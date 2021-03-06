/**
 * AudioFileIndexer.java
 * (C) 2011 Florian Staudacher, Christian Wurst
 * 
 * This file is part of AutoDJ.
 *
 * AutoDJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AutoDJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AutoDJ.  If not, see <http://www.gnu.org/licenses/>.
 */

package AutoDJ.metaReader;

import java.nio.ByteBuffer;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.RandomAccessFile;
import java.lang.Exception;


/**
 * abstract class AudioFileIndexer
 * 
 * provides an interface to read the metadata of an audio file
 * subclasses have to implement the actual parsing according to the file format
 * 
 * @author Florian Staudacher <florian_staudacher@yahoo.de>
 *
 */
public abstract class AudioFileIndexer {

	protected File audioFile;   // contains the file handle to the audio file
	protected ByteBuffer buff;  // contains the actual metadata
	protected RandomAccessFile raf;
	protected String filePath; 
	
	// here comes the metadata definition
	protected String title, 
			 artist, 
			 album, 
			 comment, 
			 genre,
			 trackno,
			 year;
	BufferedImage	 cover; 
	
	protected long 	 lastModified,	// milliseconds since 1970-01-01 00:00 
			 length;	// filesize in bytes
	
	/**
	 * reads the metadata in the audio file and saves it to local member variables
	 * you must call this method once, before you need to read the actual metadata
	 * 
	 * TODO: implement in the subclasses
	 */
	abstract void populateMetadata();
	
	/**
	 * opens the file and puts the part containing metadata in ByteBuffer buff
	 * also places the file handle in audioFile
	 * 
	 * TODO: implement in the subclass
	 * 
	 * @param path
	 * @throws Exception 
	 */
	abstract void readFile(String path) throws Exception;
	
	/**
	 * gets infos about the file, calls the populateMetadata method
	 * also reads some things that are not music-related:
	 * - last modification date
	 * - filesize in byte
	 */
	public void getFileInfo() {
		//System.out.println(filePath);
		populateMetadata();
		lastModified = audioFile.lastModified();
		length = audioFile.length();
	}
	
	/**
	 * return the matching indexer subclass determined by the filename extension
	 * 
	 * @param String fileName
	 * @return AudioFileIndexer
	 */
	public static AudioFileIndexer initIndexer(String fileName) {
		String ext = fileName.substring(fileName.length()-3);
				
		if(ext.equalsIgnoreCase("mp3")) 
			return new Mp3Indexer(fileName);
		if(ext.equalsIgnoreCase("ogg") ||
		   ext.equalsIgnoreCase("oga")) 
			return new OggIndexer(fileName);
		
		System.out.println("unknown filetype");
		return null;
	}
	
	/**
	 * returns a human-readable representation of the metadata
	 * coming from the file we just read
	 * 
	 * @return String output
	 */
	public String toString() {
		return  getTitle() +" by "+ getArtist() +" ("+getYear()+")\n\t"+
				 "#"+getTrackno()+" on "+getAlbum() +"\n\t"+
				filePath+"\n\t";
	}
	
	/**
	 * how nice: java only uses big endian encoding, but we need little endian
	 * also, all integers are signed per default, which cannot be turned off
	 * ... therefore we need to juggle around some bytes by hand
	 */
	protected int getIntFromBuff() {
		byte[] tmp = new byte[4];
		try {
			raf.read(tmp);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return unsignedBytesToInt(tmp);
	}
	
// currently not needed	
/*	protected long getLongFromBuff() {
		byte[] tmp = new byte[8];		
		try {
			raf.read(tmp);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return unsignedBytesToLong(tmp);
	}
	
	protected long unsignedBytesToLong(byte[] tmp) {
		long res = 0;
		for(int i=0; i<tmp.length; i++) {
               res|=(1L<<i);
        }
		return res;
	}
*/
	
	protected int unsignedBytesToInt(byte[] buf) {
		int i = 0;
		for (int k = 0; k < 4; k++) {
			i += unsignedByteToInt(buf[k]) << (24-(8*k)); 
		}
		return swabInt(i);
	}
	
	protected int unsignedByteToInt(byte b) {
	    return (int) b & 0xFF;
	}

	protected int swabInt(int v) {
	    return  (v >>> 24) | (v << 24) | 
	      ((v << 8) & 0x00FF0000) | ((v >> 8) & 0x0000FF00);
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		if( title != null) return title;
		
		return "Unknown Title";
	}

	/**
	 * @return the artist
	 */
	public String getArtist() {
		if( artist != null ) return artist;
		
		return "Unknown Artist";
	}

	/**
	 * @return the album
	 */
	public String getAlbum() {
		if( album != null ) return album;
		
		return "Unknown Album";
	}

	/**
	 * @return the genre
	 */
	public String getGenre() {
		if( genre != null ) return genre;
		
		return "Unknown Genre";
	}

	/**
	 * @return the trackno
	 */
	public int getTrackno() {
		if( trackno == null || trackno.isEmpty() ) return 0;
		
		return Integer.parseInt(trackno);
	}

	/**
	 * @return the year
	 */
	public int getYear() {
		if( year == null || year.isEmpty() ) return 0;
		
		return Integer.parseInt(year);
	}

	/**
	 * @return the cover
	 */
	public BufferedImage getCover() {		
		return cover;
	}
	
}
