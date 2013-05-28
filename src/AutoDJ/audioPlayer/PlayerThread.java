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

/**
 * Handle playback with MPlayer (http://www.mplayerhq.hu)
 * 
 * most of the code is from http://beradrian.wordpress.com/2008/01/30/jmplayer/
 * 
 */
public class PlayerThread extends Thread {

	private PrintStream mplayerIn;
	private BufferedReader mplayerOutErr;
	private Process mplayerProcess;
	private boolean playing = false;
	private boolean paused = false;
	private final File mplayerPath = new File(Settings.get("mplayerPath"));

	// TODO: check if there's a good way to add a song just before the current
	// one finishes of find other ways for MPlayer Playlist

	/**
	 * initialize connection to mplayer via buffers and stdin/stdout
	 */
	public PlayerThread() {

		try {
			if (!mplayerPath.exists())
				throw new IOException("MPlayer not found at \""
						+ mplayerPath.getPath() + "\"!");
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
			new StreamRedirecter(mplayerProcess.getInputStream(), writeTo)
					.start();
			new StreamRedirecter(mplayerProcess.getErrorStream(), writeTo)
					.start();

			// the standard input of MPlayer
			mplayerIn = new PrintStream(mplayerProcess.getOutputStream());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * stop playback and quit mplayer
	 */
	public void kill() {
		mplayerIn.print("quit");
		mplayerIn.print("\n");
		mplayerIn.flush();
		try {
			mplayerProcess.waitFor();
		} catch (InterruptedException e) {

		}
		interrupt();
		mplayerProcess.destroy();
	}

	public void run() {
		// IDLE
	}

	/**
	 * load a song and start playback
	 * 
	 * @param song
	 *            Song object to be loaded into player
	 */
	public void loadSong(Song s) {
		loadSong(s, 0);
	}

	/**
	 * load a song to position
	 * 
	 * @param song
	 *            Song object to be loaded into player
	 * @param position
	 *            indicate position where the song is to be added; if 0 playback
	 *            stops and starts with this song; avoid gaps plz
	 */
	public void loadSong(Song s, int position) {
		String command = "loadfile \"" + s.getFile().getAbsolutePath() + "\" "
				+ position;
		if (command.contains("\\"))
			command = command.replace("\\", "/"); // for Windows paths
		playing = true;
		paused = false;
		mplayerIn.print(command);
		mplayerIn.print("\n");
		mplayerIn.flush();

	}

	/**
	 * pause/unpause playback
	 */
	public void pausePlayback() {
		paused = !paused;
		mplayerIn.print("pause");
		mplayerIn.print("\n");
		mplayerIn.flush();
	}

	/**
	 * stop playback
	 */
	public void stopPlayback() {
		paused = false;
		playing = false;
		mplayerIn.print("stop");
		mplayerIn.print("\n");
		mplayerIn.flush();
	}

	/**
	 * toggle mute
	 */
	public void toggleMute() {
		mplayerIn.print("mute");
		mplayerIn.print("\n");
		mplayerIn.flush();
	}

	/**
	 * get the playing time in seconds as reported by mplayer
	 */
	public int getPlayingTime() {
		// mplayerIn.print("get_property length");
		mplayerIn.print("get_time_length");
		mplayerIn.print("\n");
		mplayerIn.flush();

		String answer;
		int totalTime = -1;
		try {
			String prefix = "ANS_length=";
			while ((answer = mplayerOutErr.readLine()) != null) {
				if (answer.startsWith(prefix)) {
					totalTime = (int) Double.parseDouble(answer
							.substring(prefix.length()));
					break;
				}
			}
		} catch (IOException e) {
		}

		return totalTime;
	}

	public boolean getPlaying() {
		return this.playing;
	}

	public boolean getPaused() {
		return this.paused;
	}

	public static void main(String[] args) {
		PlayerThread myPlayer = new PlayerThread();
		try {
			System.out.println("do");
			File f = new File(
					"D:\\filz\\audio\\soundtrack\\Digimon\\Digimon - Len - Kids In America.mp3");
			// "D:/filz/audio/soundtrack/Digimon/Digimon - Len - Kids In America.mp3"
			System.out.println(f.getName() + " exists: " + f.exists());
			System.out.println(f.getAbsolutePath());
			System.out.println(f.getAbsolutePath());
			System.out.println(f.getCanonicalPath());
			System.out.println(f.getPath());
			myPlayer.loadSong(new Song(f));
			Thread.sleep(10000);
			System.out.println(myPlayer.getPlayingTime());
			System.out.println("pause");
			myPlayer.pausePlayback();
			Thread.sleep(3000);
			System.out.println("resume");
			myPlayer.pausePlayback();
			Thread.sleep(10000);
			System.out.println("stop");
			myPlayer.stopPlayback();
			myPlayer.kill();
			myPlayer = null;
			System.out.println("done");
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
