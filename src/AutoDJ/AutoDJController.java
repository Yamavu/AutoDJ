/**
 * AutoDJCore.java
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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

//import javax.activation.MimetypesFileTypeMap;

import AutoDJ.audioPlayer.PlayerThread;
import AutoDJ.firstrun.Firstrun;
import AutoDJ.prefs.Settings;
import AutoDJ.wizard.Wizard;

/**
 * AutoDJController is a class which represents AutoDJ's Controller
 * part as specified in MVC. It reacts on user input coming from
 * AutoDJView, does all necessary calculations and updates
 * AutoDJModel accordingly.<br \>
 * <a href="http://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller">
 * Wikipedia: model-view-controller</a>
 * @see AutoDJModel
 * @see AutoDJView
 */

public class AutoDJController implements Observer {
	/**
	 * The AutoDJModel-Object this class modifies.
	 * @see AutoDJModel
	 */
	private AutoDJModel model;
	/**
	 * The SongDatabase this class works with.
	 * @see SongDatabase
	 */
	private SongDatabase myDatabase;
	
	/**
	 * The Wrapper around MPlayer. All Songs are played through this
	 * @see PlayerThread
	 */
	private PlayerThread myPlayer;
	
	private Random random = new Random();
	
	
	/**
	 * Creates a new AutoDJController object which interacts with
	 * the specified AutoDJModel.
	 * @param m The AutoDJModel object this AutoDJController will
	 * interact with.
	 */
	public AutoDJController (AutoDJModel m) {
		
		try{
			myPlayer=new PlayerThread();
			myPlayer.run();
		} catch (IOException e) {
			// TODO handle MPlayer Missing Exception in UI
			System.out.println(e.getMessage());
		}
		
		// if this is the first time the user opens this application
		// show a short wizard that asks for the most important settings
		if( Settings.get("firstrun", "true").equals("true") ) {
			Firstrun first = new Firstrun();
			int retVal = first.begin();
			
			if( retVal == Wizard.FINISH_RETURN_CODE) {
				Settings.set("firstrun", "false");
			} else {
				System.exit(0);
			}
		} 
		
		initializeDatabase(m);	
	}
	
	/**
	 * Reads the database credentials from the settings and 
	 * establishes a connection
	 */
	private void initializeDatabase(AutoDJModel m) {
		String url = "";
		
		if( Settings.get("dbType", "mysql").equals("mysql") ) {
			url = getMySqlConnectionString();
		} else if( Settings.get("dbType").equals("sqlite")) {
			url = getSqliteConnectionString();
		}
		
		myDatabase = new SongDatabase(url);
		model = m;
	}
	
	/**
	 * build the mysql string from user settings and return it
	 * 
	 * @return String mysql connection string
	 */
	private String getMySqlConnectionString() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		String dbUser=Settings.get("dbUser");
		String dbPass=Settings.get("dbPass");
		String dbHost=Settings.get("dbHost");
		String dbName=Settings.get("dbName");
		String url="jdbc:mysql://"+dbHost+"/"+dbName+"?user="+dbUser+"&password="+dbPass;
		
		return url;
	}
	
	/**
	 * build the sqlite connections string from user settings
	 * 
	 * @return String sqlite connection string
	 */
	private String getSqliteConnectionString() {
		try {
			Class.forName("org.sqlite.JDBC").newInstance();
		} catch (Exception e) {
			System.out.println("SQLite not found: "+ e.getMessage());
		}
		
		String dbPath = Settings.get("dbPath");
		String url = "jdbc:sqlite:"+dbPath+"/AutoDJ.sqlite.db";
		
		return url;
	}
	
	/**
	 * Rescans the harddisk for all MP3 files and updates the
	 * song database, if necessary.
	 */
	//TODO: needs too many system resources, walk through directories and apply logic seperately 
	/*private void rescanDatabase() {
		// get all songs from DB
		Vector<Song> databaseList = new Vector<Song>();
		databaseList = myDatabase.getSongs("");
		
		// get all songs from HD
		final String mp3Dir=Settings.get("mp3Dir");
		File mp3dir = new File(mp3Dir);
		Vector<File> mp3Files = new Vector<File>();
		mp3Files = getAllmp3Files(mp3dir, mp3Files);
		
		// create a new Vector to store all mp3-infos in
		Vector<Song> songFiles = new Vector<Song>();
		for (int i=0; i<mp3Files.size(); i++) {
			songFiles.add(new Song(mp3Files.elementAt(i)));
		}
		
		model.setLogtext("Database size: "+databaseList.size()+" Song(s)");
		model.setLogtext("Files on HD: "+songFiles.size()+" Song(s)");
		
		// find and remove all files which have a DB entry
		// and are unchanged from songFiles
		// mark all such files in databaseList (set them null)
		for(int i=0; i<databaseList.size(); i++) {
			for (int j=0; j<songFiles.size(); j++) {
				if (databaseList.elementAt(i).equals(songFiles.elementAt(j))) {
					databaseList.set(i,null);
					songFiles.remove(j);
					break;
				} else if ( databaseList.elementAt(i).compareMD5sum (songFiles.elementAt(j)) ||
							databaseList.elementAt(i).compareFile   (songFiles.elementAt(j))) {
					model.setLogtext("Found possible match:");
					model.setLogtext("DB:   "+databaseList.elementAt(i).toString());
					model.setLogtext("File: "+songFiles.elementAt(j).toString());
					// update DB
					myDatabase.changeSong(databaseList.elementAt(i), songFiles.elementAt(j));
					databaseList.set(i,null);
					songFiles.remove(j);
					break;
				}
			}
		}
		
		// remove all found files from databaseList
		while(databaseList.remove(null)) {
			continue;
		}

		for (int i=0; i<songFiles.size(); i++) {
			myDatabase.addSong(songFiles.elementAt(i));
		}
		
		model.setLogtext("Added "+songFiles.size()+" song(s) to database.");
	}*/

	
	/**
	 * Rescans the harddisk for all MP3 files and updates the
	 * song database, if necessary. 
	 */
	// TODO: why does this thing abort operation at about 500/3000 songs??
	private void rescanDatabase2() {
		
		int updS = 0, newS = 0;
		
		final File mp3Dir=new File(Settings.get("mp3Dir"));
		String[] fileTypes={"mp3","MP3","mP3","Mp3","ogg","oga"};
		Iterator<File> fileIter = FileUtils.iterateFiles(mp3Dir, fileTypes, true);
		try{
			while (fileIter.hasNext()) {
				File file = fileIter.next();
				Song newSong;
				Song match = myDatabase.getSongExactly(file.getAbsolutePath());
				if (match!=null){
					if (!Song.calculateMD5(file).equals(match.getMD5sum())){
						newSong = new Song(file);
						myDatabase.changeSong(match, newSong);
						model.setLogtext("Update Song:");
						model.setLogtext("DB:   "+match.toString());
						model.setLogtext("File: "+newSong.toString());
						updS++;
					}// else do nothing
				} else {
					myDatabase.addSong(new Song(file));
					newS++;
				}
				
			}
		}catch (OutOfMemoryError e){
			System.err.println("Out of Memory. Aborting Scan. Bruised, but happy.");
		}catch (Exception f){
			f.printStackTrace();
		}
				
		model.setLogtext("Added "+newS+" song(s) to database.");
		model.setLogtext("Changed "+updS+" song(s) to database.");
		
	}
	
	/**
	 * Recursively gets all MP3 files in a given directory.
	 * @param file A File object representing the directory we search in.
	 * @param mp3file A Vector of File objects representing all MP3 files
	 * we found so far.
	 * @return A Vector of File objects representing all MP3 files
	 * we found.
	 */
	/*private static Vector<File> getAllmp3Files(File file, Vector<File> mp3file) {
		if (file.isDirectory()) {
			String[] children = file.list();
			for (int i=0; i<children.length; i++) {
				getAllmp3Files(new File(file, children[i]), mp3file);
			}
		} else {
			// it's a file, not a dir, but it is a mp3-file?
			// this checks the mime type via the file extension
			// other, more sophisticated solutions exist, but they are reported as slow
			MimetypesFileTypeMap map = new MimetypesFileTypeMap();
			// unfortunately this method doesn't know about audio/mpeg
			map.addMimeTypes("audio/mpeg mp3 MP3 mP3 Mp3");
			map.addMimeTypes("audio/vorbis ogg oga");
			
			String fileMime = map.getContentType(file); 
			if (fileMime.equals("audio/mpeg") || 
				fileMime.equals("audio/vorbis")) 
			    mp3file.add(file);
		}
		return mp3file;
	}*/

	/**
	 * Query the song database for songs matching the search string
	 * and update AutoDJModel accordingly.
	 * @param search The search string, which will be matched against
	 * the songs artist, title or album.
	 * @see SongDatabase
	 */
	public void filterSongLibrary(String search) {
		model.setSongLibrary(myDatabase.getSongs(search));
	}
	
	/**
	 * Updates this object if changes in an other object occurs. At the moment
	 * this class is notified only if something in AutoDJView has changed,
	 * e.g. a button was pressed etc.
	 * @param view The object which notified this class of some change.
	 * @param msg The ObserverMessage the object which changed sent.
	 * @see ObserverMessage
	 */
	@Override
	public void update(Observable view, Object msg) {
		if (msg instanceof ObserverMessage) {
			ObserverMessage message = (ObserverMessage) msg;
			int index;
			List<Song> selectedSongs, playlistSongs;
			switch (message.getMessage()){
				case ObserverMessage.PLAY:
					if (!myPlayer.getPlaying()){
						model.resetPlaylistMarker();
						myPlayer.loadSong(model.getCurrentSong());
						System.out.println ("PLAY: "+ model.getCurrentSong().getFile().getName());
					}else{
						// TODO: if Playlist empty: myplayer.playing = false;
						myPlayer.pausePlayback();
						System.out.println ("PLAY (UNPAUSE)");
					}
					
					break;
				case ObserverMessage.PAUSE:
					myPlayer.pausePlayback();
					System.out.println ("PAUSE");
					break;
				case ObserverMessage.NEXT_SONG:
					myPlayer.stopPlayback();
					myPlayer.loadSong(model.getNextSong());
					System.out.println ("NEXTSONG");
					break;
				case ObserverMessage.RESCAN_LIBRARY:
					rescanDatabase2();
					System.out.println ("RESCAN");
					break;
				case ObserverMessage.SEARCHTEXT_CHANGED:
					filterSongLibrary(((AutoDJView) view).getSearchText());
					break;
				case ObserverMessage.ADD_SONG_TO_PLAYLIST:
					selectedSongs=((AutoDJView) view).getSelectedLibrarySongs();
					playlistSongs=model.getPlaylist();
					for (Song selectedSong:selectedSongs) {
						if (!playlistSongs.contains(selectedSong)) {
							model.addToPlaylist(selectedSong);
						} else {
							model.setLogtext("Song " + selectedSong.getArtist() +
									" - " + selectedSong.getTitle() + " not added" +
									" to playlist, because it already contains it.");
						}
					}
					break;
				case ObserverMessage.ADD_RANDOM_SONG_TO_PLAYLIST:
					
					selectedSongs=((AutoDJView) view).getLibrarySongs();
					playlistSongs=model.getPlaylist();
					System.out.print("ADD RANDOM SONG out of " + selectedSongs.size() + ": ");
					Song randomSong = selectedSongs.get(random.nextInt(selectedSongs.size()));
					if (!playlistSongs.contains(randomSong)) {
						model.addToPlaylist(randomSong);
					} else {
						model.setLogtext("Song " + randomSong.getArtist() +
								" - " + randomSong.getTitle() + " not added" +
								" to playlist, because it already contains it.");
					}
					break;
				case ObserverMessage.REMOVE_SONG_FROM_PLAYLIST:
					model.removeFromPlaylist(((AutoDJView) view).getSelectedPlaylistSongs());
					break;
				case ObserverMessage.MOVE_SONG_DOWN_IN_PLAYLIST:
					index = model.moveSongInPlaylist(((AutoDJView) view).getSelectedPlaylistIndex(), 1);
					((AutoDJView) view).setSelectedPlaylistIndex(index);
					break;
				case ObserverMessage.MOVE_SONG_UP_IN_PLAYLIST:
					index = model.moveSongInPlaylist(((AutoDJView) view).getSelectedPlaylistIndex(), -1);
					((AutoDJView) view).setSelectedPlaylistIndex(index);
					break;
				default:
					System.out.println ("Unknown Observer-Message caught!");
			}
		}
	}
}
