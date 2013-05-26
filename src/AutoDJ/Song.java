/**
 * Song.java
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

package AutoDJ;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
/*import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.datatype.Artwork;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v24Frames;*/

import AutoDJ.metaReader.AudioFileIndexer;

/**
 * Song is a class which represents a single song.
 * It stores all relevant data to handle a song in AutoDJ.
 */

public class Song {
	/**
	 * The id of this song in the database.
	 */
	private int id;
	/**
	 * The artist of this song.
	 */
	private String artist;
	/**
	 * The title of this song.
	 */
	private String title;
	/**
	 * The track number on the album this song was released on.
	 */
	private int trackno;
	/**
	 * The name of the album this song was released on.
	 */
	private String album;
	/**
	 * The cover art of this song.
	 */
	private BufferedImage cover;
	/**
	 * The year song was released in.
	 */
	private int year;
	/**
	 *The genre this song belongs to.
	 */
	private String genre;
	/**
	 *The MP3 file this song is stored in as a File object.
	 */
	private File filename;
	/**
	 * The md5sum of the MP3 file this song is stored in.
	 */
	private String md5sum;
	
	/**
	 * Creates a new Song object from a File. It extracts all
	 * the relevant data from the file on the harddisk and
	 * stores it inside the newly created instance.
	 * @param file The file representing the MP3 file on disk.
	 */
	public Song(File file) {
		filename = file;
		// calculate the md5sum
		try {
			md5sum = calculateMD5(filename);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		// get the ID3 tag information
		try {
			AudioFileIndexer audioMetadata = AudioFileIndexer.initIndexer(filename.getAbsolutePath());
			audioMetadata.getFileInfo();
			
			artist  = audioMetadata.getArtist();
			title   = audioMetadata.getTitle();
			album   = audioMetadata.getAlbum();
			cover   = audioMetadata.getCover();
			year    = audioMetadata.getYear();
			trackno = audioMetadata.getTrackno();
			genre   = audioMetadata.getGenre(); 
	
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * Creates a new Song object with the given data.
	 * Used for creation of Song objects already stored in the database.
	 * @param id The unique id this song has in the database.
	 * @param artist The artist of this song.
	 * @param title The title of this song.
	 * @param trackno The track number of this song on the given album.
	 * @param album The name of the album this song is on.
	 * @param genre genre this song belongs to.
	 * @param year The year this song was first released to public.
	 * @param filename A File object storing the filename of the MP3 file.
	 * @param md5sum The md5sum of this MP3 file.
	 */
	public Song(int id, String artist, String title, int trackno, String album,
			BufferedImage cover, int year, String genre, File filename, String md5sum) {
		this.id      = id;
		this.artist  = artist;
		this.title   = title;
		this.trackno = trackno;
		this.album   = album;
		this.cover   = cover;
		this.year    = year;
		this.genre   = genre;
		this.filename= filename;
		this.md5sum  = md5sum;
	}
	
	/**
	 * calculate a MD5sum of a given file
	 * @param file the file to calculate the md5sum from.
	 */
	public static String calculateMD5(File file) throws NoSuchAlgorithmException, FileNotFoundException {
		MessageDigest digest = MessageDigest.getInstance("MD5");
		InputStream is = new FileInputStream(file);				
		byte[] buffer = new byte[8192];
		int read = 0;
		try {
			while( (read = is.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}		
			byte[] md5sum = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5sum);
			return bigInt.toString(16);
		} catch(IOException e) {
			throw new RuntimeException("Unable to process file for MD5", e);
		} finally {
			try {
				is.close();
			}
			catch(IOException e) {
				throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
			}
		}
	}

	/**
	 * Returns the unique id this song has in the database.
	 * @return The unique id this song has in the database.
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Returns the artist of this song.
	 * @return The artist of this song.
	 */
	public String getArtist() {
		return this.artist;
	}

	/**
	 * Returns the title of this song.
	 * @return The title of this song.
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * Returns the track number of this song on the given album.
	 * @return The track number of this song on the given album.
	 */
	public int getTrackno() {
		return this.trackno;
	}

	/**
	 * Returns the name of the album this song is on.
	 * @return The name of the album this song is on.
	 */
	public String getAlbum() {
		return this.album;
	}

	/**
	 * Returns the cover art of this song.
	 * @return The cover art of this song.
	 */
	public BufferedImage getCover() {
		return cover;
	}
	
	/**
	 * returns a byte array containing the binary data from the album cover
	 * 
	 * @return byte[] cover image
	 */
	public byte[] getCoverBytes() {
		if( getCover() == null ) return new byte[0];
		
		WritableRaster raster = getCover().getRaster();
		DataBufferByte buff = (DataBufferByte) raster.getDataBuffer();
		return buff.getData();
	}

	/**
	 * Returns the year this song was first released to public.
	 * @return The year this song was first released to public.
	 */
	public int getYear() {
		return this.year;
	}

	/**
	 * Returns the genre this song belongs to.
	 * @return The genre this song belongs to.
	 */
	public String getGenre() {
		return this.genre;
	}

	/**
	 * Returns a File object storing the filename of the MP3 file.
	 * @return A File object storing the filename of the MP3 file.
	 */
	public File getFile() {
		return this.filename;
	}

	/**
	 * Returns the md5sum of this MP3 file.
	 * @return The md5sum of this MP3 file.
	 */
	public String getMD5sum() {
		return this.md5sum;
	}

	/**
	 * Returns a String representation of the data stored
	 * in this object. Useful for debugging purposes.
	 * @return A String representation of the data stored
	 * in this object.
	 */
	public String toString() {
		String retval="Info about Song";
		retval+=getFile().getAbsolutePath() + "\n";
		retval+=getArtist() + " - " + getTitle() + "\n";
		retval+="from the album: " + getAlbum() + ", released in " +getYear() + "\n";
		retval+="file has a MD5sum of " + getMD5sum();
		return retval;
	}

	/**
	 * Compares two Song instances. Returns true only if the md5sums
	 * match (meaning the file content doesn't differ) AND the fully
	 * qualified path name doesn't differ.
	 * @return true, if the md5sums match and the path names don't differ,
	 * false otherwise.
	 */
	public boolean equals(Song song) {
		return compareMD5sum(song) && compareFile(song);
	}

	/**
	 * Compares two Song instances. Returns true only if the md5sums
	 * match (meaning the file content doesn't differ).
	 * @return true, if the md5sums match, false otherwise.
	 */
	public boolean compareMD5sum(Song song) {
		return this.getMD5sum().equals(song.getMD5sum());
	}

	/**
	 * Compares two Song instances. Returns true only if the fully
	 * qualified path name doesn't differ.
	 * @return true, if the path names don't differ, false otherwise.
	 */
	public boolean compareFile(Song song) {
		return this.getFile().equals(song.getFile());
	}
}
