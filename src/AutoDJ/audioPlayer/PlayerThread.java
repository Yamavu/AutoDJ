/**
 * PlayerThread.java
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

package AutoDJ.audioPlayer;

import AutoDJ.*;
import AutoDJ.prefs.Settings;
import java.io.*;
import java.util.List;
import java.util.Observable;

/**
 * Handle playback with MPlayer (http://www.mplayerhq.hu)
 * 
 * most of the code is from http://beradrian.wordpress.com/2008/01/30/jmplayer/
 * 
 */
public class PlayerThread extends Thread{
	
	public Observable obs = new Observable();
	private PrintStream mplayerIn;
	private BufferedReader mplayerOutErr;
	private Process mplayerProcess;
	private boolean playing = false;
	private boolean paused = false;
	private final File mplayerPath = new File(Settings.get("mplayerPath"));
	//private final File mplayerPath = new File("C:\\progs\\media\\MPlayer\\mplayer.exe");

	// TODO: implement API closer to the specification to enhance capabilities
	// like showing remaining time, playing consecutive songs and other features
	// beyond calling MPlayer with single files

	/**
	 * initialize connection to mplayer via buffers and stdin/stdout
	 */
	public PlayerThread() throws IOException {
		if (!mplayerPath.exists())
			throw new IOException("MPlayer not found at \""
					+ mplayerPath.getPath() + "\"!");
	}

	public void run() {
		try {
			mplayerProcess = Runtime.getRuntime().exec(
					mplayerPath.getPath() + " -slave -idle"); // +-quiet

			// create the piped streams where to redirect the standard output
			// and error of MPlayer
			// specify a bigger pipesize than the default of 1024
			PipedInputStream readFrom = new PipedInputStream(256 * 1024);
			PipedOutputStream writeTo = new PipedOutputStream(readFrom);

			mplayerOutErr = new BufferedReader(new InputStreamReader(readFrom));

			// create the threads to redirect the standard output and error of
			// MPlayer
			new StreamRedirecter(mplayerProcess.getInputStream(), writeTo).start();
			new StreamRedirecter(mplayerProcess.getErrorStream(), writeTo).start();

			// the standard input of MPlayer
			mplayerIn = new PrintStream(mplayerProcess.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void runCommand(String cmd){
		mplayerIn.print(cmd+"\n\n");
		mplayerIn.flush();
	}
	
	
	/**
	 * claim an answer from the mplayer out-error-stream
	 * use with caution, because it blocks this command thread if used wrong
	 * if you're not sure use prefix=""
	 */
	private String getAnswer(String prefix){
		String buff;
		try {
			while ((buff = mplayerOutErr.readLine()) != "") {
				if (buff.startsWith(prefix))
					return buff.substring(prefix.length());
			}
		} catch (IOException e) {
			// TODO: more sophisticated error handling
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * stop playback and quit mplayer
	 */
	public void kill() {
		runCommand("quit");
		try {
			mplayerProcess.waitFor();
		} catch (InterruptedException e) {

		}
		mplayerProcess.destroy();
		interrupt();
	}
	

	/**
	 * load a song and start playback
	 * 
	 * @param song
	 *            Song object to be loaded into player
	 */
	public void loadSong(Song s) {
		loadSong(s, -1);
	}

	/**
	 * load a song to position
	 * 
	 * @param song
	 *            Song object to be loaded into player
	 * @param position
	 *            indicate position where the song is to be added; if 0 playback
	 *            stops and starts with this song; gaps in playlist are skipped automatically
	 */
	public void loadSong(Song s, int position) {
		String command = "loadfile \"" + s.getFile().getAbsolutePath()+ "\" ";
		if (position>0){
			command += position;
			playing = true;
		}else {
			command += "0";
			playing = true;
			paused = false;
		}
		if (command.contains("\\"))
			command = command.replace("\\", "/"); // for Windows paths
		runCommand(command);
	}

	/**
	 * pause/unpause playback
	 */
	public void pausePlayback() {
		paused = !paused;
		runCommand("pause");
	}

	/**
	 * stop playback
	 */
	public void stopPlayback() {
		paused = false;
		playing = false;
		runCommand("stop");
	}

	/**
	 * toggle mute
	 */
	public void toggleMute() {
		runCommand("mute");
	}

	/**
	 * get the playing time in seconds as reported by mplayer
	 */
	public int getPlayingTime() {
		// mplayerIn.print("get_property length");
		runCommand("get_time_length");
		runCommand("get_time_length");
		double ans = Double.parseDouble(getAnswer("ANS_length="));
		return (int) ans;
	}
	
	/**
	 * get the remaining playing time in percent as reported by mplayer
	 */
	public int getPlaylistPos(List<Song> songs){
		String song = getCurrent();
		for(int i = 0; i<songs.size();i++){
			if (song==songs.get(i).getFile().getName())
				return i;
		}
		return -1;
	}
	
	public void skipCurrent(){
		runCommand("pt_step 0");
	}

	public boolean getPlaying() {
		return this.playing;
	}

	public boolean getPaused() {
		return this.paused;
	}

	public String getCurrent() {
		runCommand("get_file_name");
		//runCommand("");
		String filename = getAnswer("ANS_FILENAME=");
		return filename.substring(1, filename.length()-1);
	}

	public static void main(String[] args) {
		try {
			PlayerThread myPlayer = new PlayerThread();
			myPlayer.run();
			System.out.println("do");
			File f = new File(
					"D:\\filz\\audio\\soundtrack\\Digimon\\Digimon - Len - Kids In America.mp3");
			// "D:/filz/audio/soundtrack/Digimon/Digimon - Len - Kids In America.mp3"
			System.out.println("\""+f.getName() + "\" exists: " + f.exists());
			System.out.println(f.getPath());
			myPlayer.loadSong(new Song(f));
			Thread.sleep(1000);
			Thread.sleep(1000);
			Thread.sleep(1000);
			System.out.println(myPlayer.getCurrent());
			System.out.println("pause");
			myPlayer.pausePlayback();
			Thread.sleep(3000);
			System.out.println("resume");
			myPlayer.pausePlayback();
			Thread.sleep(10000);
			System.out.println("stop");
			myPlayer.stopPlayback();
			myPlayer.kill();
			//myPlayer.join();
			myPlayer = null;
			System.out.println("done");
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
