package AutoDJ.tests;

import java.awt.image.BufferedImage;
import java.io.*;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import AutoDJ.*;
import AutoDJ.audioPlayer.*;
import AutoDJ.metaReader.AudioFileIndexer;
import AutoDJ.prefs.FilePreferencesFactory;
import AutoDJ.prefs.Settings;

/**
 * This class produces a test executeable to check the functionality of various
 * parts of the AutoDJ program.
 * 
 * The binary can be called with the following parameters
 * 
 * -f/--readMp3/--readOgg takes an _absolute_ filename and analyzes a the file
 * prints out the metadata -d/--readDir takes an _absolute_ directory and
 * analyzes all audio files it finds inside. prints out the metadata of each
 * file -p/--play takes an _absolute_ filename and tries to play it
 * -s/--settings tries to write and then read settings to the user's config file
 * 
 * @author Florian Staudacher
 * 
 */


// TODO: more test cases
//TODO: add Test-all option 
public class UnitTests {

	static PlayerThread t;
	static BufferedImage cover;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String command = "";
		// parsing args
		for (String param : args) { // "foreach"
			if (param.startsWith("-")) {
				// remove the "-" and "--"s
				if (param.startsWith("--"))
					param = param.substring(1);
				command = param.substring(1);
			}

			if (command.equalsIgnoreCase("aaa")) {
				out("test");

				// read one or more files
			} else if (command.equalsIgnoreCase("readMp3")
					|| command.equalsIgnoreCase("readOgg")
					|| command.equalsIgnoreCase("f")) {
				// see if the param is not just another command
				if (!param.startsWith("-")) {
					out("reading " + param + " ...");

					AudioFileIndexer audio = AudioFileIndexer
							.initIndexer(param);
					audio.getFileInfo();
					out(audio.toString());
					cover = audio.getCover();
					if (cover != null) {
						out("drawing image");
						JFrame frame = new JFrame("TEST");
						JLabel label = new JLabel(new ImageIcon(cover));
						frame.add(label);
						frame.pack();
						frame.setVisible(true);
					}
				}

				// read a whole directory
			} else if (command.equalsIgnoreCase("d")
					|| command.equalsIgnoreCase("readDir")) {
				// see if the param is not just another command
				if (!param.startsWith("-")) {
					File dir = new File(param);
					FilenameFilter filter = new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return (name.endsWith("ogg")
									|| name.endsWith("oga") || name
									.endsWith("mp3"));
						}
					};
					String[] entries = dir.list(filter);
					for (int i = 0; i < entries.length; i++) {
						out("Now reading: " + entries[i]);
						AudioFileIndexer audio = AudioFileIndexer
								.initIndexer(dir.getAbsolutePath() + "/"
										+ entries[i]);
						audio.getFileInfo();
						out(audio.toString());
						cover = audio.getCover();
						if (cover != null) {
							out("drawing image");
							JFrame frame = new JFrame("TEST");
							JLabel label = new JLabel(new ImageIcon(cover));
							frame.add(label);
							frame.pack();
							frame.setVisible(true);
						}
					}

				}
				// play back an audio file
			} else if (command.equalsIgnoreCase("p")
					|| command.equalsIgnoreCase("play")) {
				if (param.startsWith("-"))
					continue; // just another command, not a filename

				Song test = new Song(new File(param));

				out("Now playing: " + test.getArtist() + " - "
						+ test.getTitle() + " for 5 Seconds");
				try {
					t = new PlayerThread();

					t.start();
					t.loadSong(test);
					if (!t.getCurrent().equals(test.getFile().getName()))
							System.out.println("Mplayer and AutoDJ Filenames don't match!");
					(new Thread() {
						public void run() {
							try {
								Thread.sleep(5000);
							} catch (Exception e) {
							}

							t.kill();
							out("\nPlayback stopped.");
						}
					}).start();
				} catch (Exception e) {
					e.printStackTrace();
				}

			} else if (command.equalsIgnoreCase("s")
					|| command.equalsIgnoreCase("settings")) {

				// initialize preferences
				System.setProperty("java.util.prefs.PreferencesFactory",
						FilePreferencesFactory.class.getName());

				out("## testing settings storage implementation");

				if (!Settings.get("test1").isEmpty()) {
					out("already set: " + Settings.get("test1"));
				}
				Settings.set("test1", "123456789");

				if (!Settings.get("test2").isEmpty()) {
					out("already set: " + Settings.get("test2"));
				}
				Settings.set("test2", "abcdefghi");

				if (!Settings.get("time").isEmpty()) {
					out("already set: " + Settings.get("time"));
				}
				Settings.set("time", String.valueOf(System.currentTimeMillis()));

			} else
				out("unknown command");

		}
	}

	protected static void out(String text) {
		System.out.println(text);
	}

}
