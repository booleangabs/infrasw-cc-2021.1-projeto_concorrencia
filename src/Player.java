import ui.*;

//import javax.swing.*;
//import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
//import java.awt.event.MouseListener;
//import java.awt.event.MouseMotionListener;
//import java.awt.event.WindowListener;

public class Player {
    public boolean playerIsActive= true;
    public boolean currentlyPlaying = true;
    public String windowTitle = "Useless Player";
    String[][] queueArray; // Initialize the array
    public int amountSongs = 0;
    public int currentSongIndex = 0;
    public String[] currentSong;
    public AddSongWindow addSongWindow;
    public WindowListener addSongWindowListener;
    public PlayerWindow player;

    public Player() {

        ActionListener buttonListenerPlayNow =  e -> playNowListener();
        ActionListener buttonListenerStop =  e -> stopListener();
        ActionListener buttonListenerPlayPause =  e -> playPauseListener();
        ActionListener buttonListenerAddSong =  e -> addSongListener();
        ActionListener buttonListenerRemove =  e -> removeSongListener();
        ActionListener buttonListenerNext =  e -> {};
        ActionListener buttonListenerPrevious =  e -> {};
        ActionListener buttonListenerShuffle =  e -> {};
        ActionListener buttonListenerRepeat =  e -> {};

        this.queueArray = new String[1][7];
        this.player = new PlayerWindow(
                                    buttonListenerPlayNow,
                                    buttonListenerRemove,
                                    buttonListenerAddSong,
                                    buttonListenerPlayPause,
                                    buttonListenerStop,
                                    buttonListenerNext,
                                    buttonListenerPrevious,
                                    buttonListenerShuffle,
                                    buttonListenerRepeat,
                                    null,
                                    null,
                                    this.windowTitle,
                                    this.queueArray);
        this.addSongWindowListener = player.getAddSongWindowListener();
        this.player.start();
    }

    private void playNowListener() {
        System.out.println("Start");
        this.playerIsActive = true;
        this.currentlyPlaying = true;
        this.player.updatePlayPauseButton(true);
        this.currentSongIndex = this.player.getSelectedSongID();
        this.currentSong = this.queueArray[currentSongIndex];
        this.player.updateMiniplayer(
                true,
                true,
                false,
                0,
                Integer.parseInt(currentSong[5]),
                currentSongIndex,
                amountSongs);
        this.player.enableScrubberArea();
    }

    private void stopListener() {
        System.out.println("Stop");
        this.playerIsActive = false;
        this.currentlyPlaying = false;
        this.player.resetMiniPlayer();
    }

    private void playPauseListener() {
        if (this.currentlyPlaying) {
            this.currentlyPlaying = false;
            System.out.println("Pause");
        }
        else {
            this.currentlyPlaying = true;
            System.out.println("Play");
        }
        this.player.updatePlayPauseButton(currentlyPlaying);
    }

    private void addSongListener() {
        System.out.println("Add");
        ActionListener addOk = e -> addSongOkListener();
        this.addSongWindow = new AddSongWindow(this.amountSongs, addOk, addSongWindowListener);
    }

    private void removeSongListener() {
        System.out.println("Remove");
        if (amountSongs == 0) {
            System.out.println("No songs to remove");
            return;
        }
        String[][] updatedQueue = new String[this.amountSongs - 1][];
        int toRemove = this.player.getSelectedSongID();
        int j = 0;
        for (int i = 0; i < this.amountSongs; i++) {
            if (i != toRemove) {
                updatedQueue[j] = this.queueArray[i];
                updatedQueue[j][6] = Integer.toString(j);
                j++;
            }
        }
        this.queueArray = updatedQueue;
        this.player.updateQueueList(updatedQueue);
        amountSongs--;
    }

    private void addSongOkListener() {
        String[] newSong = this.addSongWindow.getSong();
        this.queueArray = addToQueue(newSong);
        this.player.updateQueueList(this.queueArray);
    }

    private String[][] addToQueue(String[] newSong) {
        String[][] updatedQueue = new String[this.amountSongs + 1][];
        for (int i = 0; i < this.amountSongs; i++) {
            updatedQueue[i] = this.queueArray[i];
        }
        updatedQueue[this.amountSongs] = newSong;
        this.amountSongs++;
        System.out.println("Added a song!");
        return updatedQueue;
    }

}

