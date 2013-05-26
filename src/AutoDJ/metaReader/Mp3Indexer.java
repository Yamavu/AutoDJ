/**
 * Mp3Indexer.java
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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.io.*;

import javax.imageio.ImageIO;


/**
 * class Mp3Indexer
 * 
 * provides the functionality to read metadata (ID3-Tags) from MP3 files
 * 
 * @author Florian Staudacher <florian_staudacher@yahoo.de>
 *
 */
public class Mp3Indexer extends AudioFileIndexer {

	protected int tagSize = 128;	// size of an ID3v1 tag
	protected int version = 0;
	
	/**
	 * initialize this object, start to read the audio file
	 * 
	 * @param String path
	 */
	public Mp3Indexer(String path) {
		filePath = path;
		try {
			readFile(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 *  read the song metadata, 
	 *  decide which tag version to use
	 */
	public void populateMetadata() {
		switch(version) {
			case 1:
				populateMetadataV1();
				break;
			case 2:
				populateMetadataV2();
				break;
		}
	}
		
	/**
	 * read the metadata from a ID3v1 header 
	 * (hopefully those tags die a slow and painful death, soon)
	 */
	protected void populateMetadataV1() {
		byte[] tag        = new byte[3];
        byte[] tagTitle   = new byte[30];
        byte[] tagArtist  = new byte[30];
        byte[] tagAlbum   = new byte[30];
        byte[] tagYear    = new byte[4];
        byte[] tagComment = new byte[30];
        byte[] tagGenre   = new byte[1];
        
        buff.get(tag).get(tagTitle).get(tagArtist).get(tagAlbum)
                        .get(tagYear).get(tagComment).get(tagGenre);
        if(!"TAG".equals(new String(tag))){
        //        throw new IllegalArgumentException(
        //                "ByteBuffer does not contain ID3 tag data"
        //        );
        	return;
        }
        
        title 	= new String(tagTitle).trim();
        artist 	= new String(tagArtist).trim();
        album	= new String(tagAlbum).trim();
        year	= new String(tagYear).trim();
        comment	= new String(tagComment).trim();
        setGenre(tagGenre[0]);
        
	}
	
	/**
	 * read everything we can from an ID3v2 header
	 * if the tag ends or some other error occurs, stop trying
	 */
	protected void populateMetadataV2() {
		while(true) {
			if(!readID3v2Tag()) break;
		}
		
	}
	
	/**
	 * read a single tag from an ID3v2 header,
	 * just see which one we got and either save or skip it
	 * 
	 * @return boolean success
	 */
	@SuppressWarnings("unused")
	protected boolean readID3v2Tag() {
		byte[] frame = new byte[4];
		int length;
		
		try {
			buff.get(frame);
			length = buff.getInt();
			buff.get(); buff.get(); // skip flags
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
				
		String ident = new String(frame);
		
		//System.out.println(ident+": "+length);
		
		if(ident.startsWith("ID3")) {
			//ignore
		} else if ("AENC".equals(ident)) { skipBytes(length);
		} else if ("APIC".equals(ident)) { // cover art
			byte[] coverTag = getID3v2Raw(length);
			
			int nullCnt = 0, i = 0;
			String mime = "";  // image mimetype
			ArrayList<Byte> descBytes = new ArrayList<Byte>();
			String desc = "";  // image text description
			
			String encoding = getTextEncoding(coverTag[0]);
			
			for(i = 1; i < length; i++) {				
				if( coverTag[i] == 0x00 ) {
					nullCnt++;
					
					// UTF-16 is terminated by two NULL bytes
					if (nullCnt > 2 && !encoding.equalsIgnoreCase("UTF-16")) break;
					if (nullCnt > 3 && encoding.equalsIgnoreCase("UTF-16")) break;
					continue;
				}
								
				switch(nullCnt) {
					case 1: 
						mime += new String(coverTag, i, 1);
						break;
					case 2: 
						descBytes.add(coverTag[i]);
						break;
					default:
						//System.out.println(new String(coverTag, i, 1));
						break;
				}
				
			}
			
			byte[] descArr = new byte[descBytes.size()];
			for(int j = 0; j < descBytes.size(); j++) {
				descArr[j] = descBytes.get(j);
			}
			try {
				desc = new String(descArr, encoding);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
			i++; // count i+1, now we're at the image data
			
			try {
				cover = ImageIO.read(new ByteArrayInputStream(coverTag, i, length-i));
			} catch( Exception e) {
				e.printStackTrace();
			}
			
		} else if ("COMM".equals(ident)) { // comments
			comment = getID3v2Text(length);
		} else if ("COMR".equals(ident)) { skipBytes(length);
			
		} else if ("ENCR".equals(ident)) { skipBytes(length);
		} else if ("EQUA".equals(ident)) { skipBytes(length);
		} else if ("ETCO".equals(ident)) { skipBytes(length);
			
		} else if ("GEOB".equals(ident)) { skipBytes(length);
		} else if ("GRID".equals(ident)) { skipBytes(length);
			
		} else if ("IPLS".equals(ident)) { skipBytes(length);
			
		} else if ("LINK".equals(ident)) { skipBytes(length);
			
		} else if ("MCDI".equals(ident)) { skipBytes(length);
		} else if ("MLLT".equals(ident)) { skipBytes(length);
			
		} else if ("OWNE".equals(ident)) { skipBytes(length);
			
		} else if ("PRIV".equals(ident)) { skipBytes(length);
		} else if ("PCNT".equals(ident)) { // play counter 
			 getID3v2Text(length);
		} else if ("POPM".equals(ident)) { skipBytes(length);
		} else if ("POSS".equals(ident)) { skipBytes(length);
			
		} else if ("RBUF".equals(ident)) { skipBytes(length);
		} else if ("RVAD".equals(ident)) { skipBytes(length);
		} else if ("RVRB".equals(ident)) { skipBytes(length);
			
		} else if ("TALB".equals(ident)) {
			album = getID3v2Text(length);
		} else if ("TBPM".equals(ident)) { skipBytes(length);
		} else if ("TCOM".equals(ident)) { skipBytes(length);
		} else if ("TCON".equals(ident)) {
			genre = getID3v2Text(length);
		} else if ("TDAT".equals(ident)) { skipBytes(length);
		} else if ("TDRC".equals(ident)) { skipBytes(length);
		} else if ("TDLY".equals(ident)) { skipBytes(length);
		} else if ("TENC".equals(ident)) { skipBytes(length);
		} else if ("TEXT".equals(ident)) { skipBytes(length);
		} else if ("TFLT".equals(ident)) { skipBytes(length);
		} else if ("TIME".equals(ident)) { skipBytes(length);
		} else if ("TIT1".equals(ident)) { skipBytes(length);
		} else if ("TIT2".equals(ident)) {
			title = getID3v2Text(length);
		} else if ("TIT3".equals(ident)) { skipBytes(length);
		} else if ("TKEY".equals(ident)) { skipBytes(length);
		} else if ("TLAN".equals(ident)) { skipBytes(length);
		} else if ("TLEN".equals(ident)) { 
			//System.out.println(getID3v2Text(length));
			skipBytes(length);
		} else if ("TMED".equals(ident)) { skipBytes(length);
		} else if ("TOAL".equals(ident)) { skipBytes(length);
		} else if ("TOFN".equals(ident)) { skipBytes(length);
		} else if ("TOLY".equals(ident)) { skipBytes(length);
		} else if ("TOPE".equals(ident)) { skipBytes(length);
		} else if ("TORY".equals(ident)) { skipBytes(length);
		} else if ("TOWN".equals(ident)) { skipBytes(length);
		} else if ("TPE1".equals(ident)) {
			artist = getID3v2Text(length);
		} else if ("TPE2".equals(ident)) { skipBytes(length);
		} else if ("TPE3".equals(ident)) { skipBytes(length);
		} else if ("TPE4".equals(ident)) { skipBytes(length);
		} else if ("TPOS".equals(ident)) { skipBytes(length);
		} else if ("TPUB".equals(ident)) { skipBytes(length);
		} else if ("TRCK".equals(ident)) { 
			trackno = getID3v2Text(length);
		} else if ("TRDA".equals(ident)) { skipBytes(length);
		} else if ("TRSN".equals(ident)) { skipBytes(length);
		} else if ("TRSO".equals(ident)) { skipBytes(length);
		} else if ("TSIZ".equals(ident)) { skipBytes(length);
		} else if ("TSRC".equals(ident)) { skipBytes(length);
		} else if ("TSSE".equals(ident)) { skipBytes(length);
		} else if ("TYER".equals(ident)) {
			year = getID3v2Text(length);
		} else if ("TXXX".equals(ident)) { skipBytes(length);
			
		} else if ("UFID".equals(ident)) { skipBytes(length);
		} else if ("USER".equals(ident)) { skipBytes(length);
		} else if ("USLT".equals(ident)) { skipBytes(length);
			
		} else if ("WCOM".equals(ident)) { skipBytes(length);
		} else if ("WCOP".equals(ident)) { skipBytes(length);
		} else if ("WOAF".equals(ident)) { skipBytes(length);
		} else if ("WOAR".equals(ident)) { skipBytes(length);
		} else if ("WOAS".equals(ident)) { skipBytes(length);
		} else if ("WORS".equals(ident)) { skipBytes(length);
		} else if ("WPAY".equals(ident)) { skipBytes(length);
		} else if ("WPUB".equals(ident)) { skipBytes(length);
		} else if ("WXXX".equals(ident)) { skipBytes(length);
		} else {		
			return false;
		}
		return true;
	}
	
	/**
	 * get the string value out of an ID3v2 tag
	 * 
	 * @param int length in bytes
	 * @return String value
	 */
	protected String getID3v2Text(int size) {
		byte[] data = new byte[size-1];
		String encoding;
		
		// find out the text encoding
		encoding = getTextEncoding(buff.get());
		
		try{
			buff.get(data);
			return new String(data, encoding);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	/**
	 * get the raw bytes from the tag value
	 * 
	 * @param int size
	 * @return byte[]
	 */
	protected byte[] getID3v2Raw(int size) {
		byte[] data = new byte[size];
		try{
			buff.get(data);
			return data;
		} catch (BufferUnderflowException e) {
			//e.printStackTrace();
		}
		return new byte[0];	
	}
	
	/**
	 * return the text encoding for a text frame
	 * 
	 * @param byte the byte that specifies the encoding
	 * @return String the encoding we've decided
	 */
	protected String getTextEncoding(byte enc) {
		String encoding;
		
		switch( enc ) {
			case 0x01:
				// UTF-16 with BOM
				encoding = "UTF-16";
				break;
			case 0x02:
				// UTF-16 without BOM
				encoding = "UTF-16";
				break;
			case 0x03:
				encoding = "UTF-8";
				break;
			default: 
				// If nothing else is said, strings, including numeric strings and URLs, 
				// are represented as ISO-8859-1 characters [id3v2.4 spec]
				encoding = "ISO-8859-1";
				break;
		}
		
		return encoding;
	}
	
	/**
	 * just ignore a given length of bytes
	 * @param int length
	 */
	protected void skipBytes(int size) {
		try { 
			buff.position(buff.position()+size);
		} catch (IllegalArgumentException e) {
			//e.printStackTrace();
		}
	}
	
	/**
	 * open an MP3 file for reading, do a little sanity check
	 * and then skip forward until we reach the header
	 */
	public void readFile(String path) throws Exception {
		audioFile = new File(path);
		raf = new RandomAccessFile(audioFile, "r");
		
		byte[] tagData;
		byte[] check = new byte[3];
		raf.read(check);
		
		if('I' == check[0] &&
			'D' == check[1] &&
			'3' == check[2]) {
			// ID3 v2
			
			raf.skipBytes(3); // skip the version & flags
			byte[] size = new byte[4];
			raf.read(size);
			
			tagSize  = (0xFF & size[0]) << 24 ;
			tagSize |= (0xFF & size[1]) << 16;
			tagSize |= (0xFF & size[2]) << 8;
			tagSize |= (0xFF & size[3]);
			
			tagData = new byte[tagSize];

			raf.seek(0);
			raf.read(tagData);
			version = 2;
		} else {
			// ID3 v1

			tagData = new byte[tagSize];
			raf.seek(raf.length() - tagSize);
			raf.read(tagData);
			version = 1;
		}
		
		buff = ByteBuffer.allocate(tagSize);
		buff.put(tagData);
		buff.rewind();		
	}
	
	/**
	 * setting the numerical genre according to the ID3v1 specification
	 * http://id3.org/d3v2.3.0
	 * 
	 * @param num
	 */
	public void setGenre(byte num) {
		switch (new Byte(num).intValue()) {
			case 0: genre 	= "Blues"; break;
			case 1: genre 	= "Classic Rock"; break;
			case 2: genre 	= "Country"; break;
			case 3: genre 	= "Dance"; break;
			case 4: genre 	= "Disco"; break;
			case 5: genre 	= "Funk"; break;
			case 6: genre	= "Grunge"; break;
			case 7: genre 	= "Hip-Hop"; break;
			case 8: genre 	= "Jazz"; break;
			case 9: genre 	= "Metal"; break;
			case 10: genre 	= "New Age"; break;
			case 11: genre 	= "Oldies"; break;
			case 12: genre 	= "Other"; break;
			case 13: genre 	= "Pop"; break;
			case 14: genre 	= "R&B"; break;
			case 15: genre 	= "Rap"; break;
			case 16: genre 	= "Reggae"; break;
			case 17: genre 	= "Rock"; break;
			case 18: genre 	= "Techno"; break;
			case 19: genre 	= "Industrial"; break;
			case 20: genre 	= "Alternative"; break;
			case 21: genre 	= "Ska"; break;
			case 22: genre 	= "Death Metal"; break;
			case 23: genre 	= "Pranks"; break;
			case 24: genre 	= "Soundtrack"; break;
			case 25: genre 	= "Euro-Techno"; break;
			case 26: genre 	= "Ambient"; break;
			case 27: genre 	= "Trip-Hop"; break;
			case 28: genre 	= "Vocal"; break;
			case 29: genre 	= "Jazz+Funk"; break;
			case 30: genre 	= "Fusion"; break;
			case 31: genre 	= "Trance"; break;
			case 32: genre 	= "Classical"; break;
			case 33: genre 	= "Instrumental"; break;
			case 34: genre 	= "Acid"; break;
			case 35: genre 	= "House"; break;
			case 36: genre 	= "Game"; break;
			case 37: genre 	= "Sound Clip"; break;
			case 38: genre 	= "Gospel"; break;
			case 39: genre 	= "Noise"; break;
			case 40: genre 	= "AlternRock"; break;
			case 41: genre 	= "Bass"; break;
			case 42: genre 	= "Soul"; break;
			case 43: genre 	= "Punk"; break;
			case 44: genre 	= "Space"; break;
			case 45: genre 	= "Meditative"; break;
			case 46: genre 	= "Instrumental Pop"; break;
			case 47: genre 	= "Instrumental Rock"; break;
			case 48: genre 	= "Ethnic"; break;
			case 49: genre 	= "Gothic"; break;
			case 50: genre 	= "Darkwave"; break;
			case 51: genre 	= "Techno-Industrial"; break;
			case 52: genre 	= "Electronic"; break;
			case 53: genre 	= "Pop-Folk"; break;
			case 54: genre 	= "Eurodance"; break;
			case 55: genre 	= "Dream"; break;
			case 56: genre 	= "Southern Rock"; break;
			case 57: genre 	= "Comedy"; break;
			case 58: genre 	= "Cult"; break;
			case 59: genre 	= "Gangsta"; break;
			case 60: genre 	= "Top 40"; break;
			case 61: genre 	= "Christian Rap"; break;
			case 62: genre 	= "Pop/Funk"; break;
			case 63: genre 	= "Jungle"; break;
			case 64: genre 	= "Native American"; break;
			case 65: genre 	= "Cabaret"; break;
			case 66: genre 	= "New Wave"; break;
			case 67: genre 	= "Psychadelic"; break;
			case 68: genre 	= "Rave"; break;
			case 69: genre 	= "Showtunes"; break;
			case 70: genre 	= "Trailer"; break;
			case 71: genre 	= "Lo-Fi"; break;
			case 72: genre 	= "Tribal"; break;
			case 73: genre 	= "Acid Punk"; break;
			case 74: genre 	= "Acid Jazz"; break;
			case 75: genre 	= "Polka"; break;
			case 76: genre 	= "Retro"; break;
			case 77: genre 	= "Musical"; break;
			case 78: genre 	= "Rock & Roll"; break;
			case 79: genre 	= "Hard Rock"; break;
			// the following genres are Winamp extensions
			case 80: genre 	= "Folk"; break;
			case 81: genre 	= "Folk-Rock"; break;
			case 82: genre 	= "National Folk"; break;
			case 83: genre 	= "Swing"; break;
			case 84: genre 	= "Fast Fusion"; break;
			case 85: genre 	= "Bebob"; break;
			case 86: genre 	= "Latin"; break;
			case 87: genre 	= "Revival"; break;
			case 88: genre 	= "Celtic"; break;
			case 89: genre 	= "Bluegrass"; break;
			case 90: genre 	= "Avantgarde"; break;
			case 91: genre 	= "Gothic Rock"; break;
			case 92: genre 	= "Progressive Rock"; break;
			case 93: genre 	= "Psychedelic Rock"; break;
			case 94: genre 	= "Symphonic Rock"; break;
			case 95: genre 	= "Slow Rock"; break;
			case 96: genre 	= "Big Band"; break;
			case 97: genre 	= "Chorus"; break;
			case 98: genre 	= "Easy Listening"; break;
			case 99: genre 	= "Acoustic"; break;
			case 100: genre	= "Humor"; break;
			case 101: genre	= "Speech"; break;
			case 102: genre	= "Chanson"; break;
			case 103: genre	= "Opera"; break;
			case 104: genre	= "Chamber Music"; break;
			case 105: genre	= "Sonata"; break;
			case 106: genre	= "Symphony"; break;
			case 107: genre	= "Booty Bass"; break;
			case 108: genre	= "Primus"; break;
			case 109: genre	= "Porn Groove"; break;
			case 110: genre	= "Satire"; break;
			case 111: genre	= "Slow Jam"; break;
			case 112: genre	= "Club"; break;
			case 113: genre	= "Tango"; break;
			case 114: genre	= "Samba"; break;
			case 115: genre	= "Folklore"; break;
			case 116: genre	= "Ballad"; break;
			case 117: genre	= "Power Ballad"; break;
			case 118: genre	= "Rhythmic Soul"; break;
			case 119: genre	= "Freestyle"; break;
			case 120: genre	= "Duet"; break;
			case 121: genre	= "Punk Rock"; break;
			case 122: genre	= "Drum Solo"; break;
			case 123: genre	= "Acapella"; break;
			case 124: genre	= "Euro-House"; break;
			case 125: genre	= "Dance Hall"; break;
			default: return;
		}
	}
	
	public String toString() {
		return "Mp3-File: "+super.toString();
	}
	
}
