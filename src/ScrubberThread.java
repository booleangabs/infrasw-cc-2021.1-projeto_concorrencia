import ui.*;

public class ScrubberThread extends Thread {
    public PlayerWindow playerWindow;
    public Player playerObject;
    int t0;

    public ScrubberThread(PlayerWindow playerWindow, Player playerObject){
        this.playerWindow = playerWindow;
        this.playerObject = playerObject;
    }

    @Override
    public void run() {
        try{
            this.t0 = this.playerWindow.getScrubberValue();
            int tf = Integer.parseInt(this.playerObject.currentSong[5]);
            if (this.playerObject.currentlyPlaying) {
                while (this.t0 < tf) {
                    this.playerWindow.updateMiniplayer(true,
                            true,
                            false,
                            this.t0,
                            tf,
                            this.playerObject.currentSongIndex,
                            this.playerObject.amountSongs);
                    Thread.sleep(1000);
                    this.t0 += 1;
                }
                this.playerWindow.updatePlayPauseButton(false);
                this.playerObject.currentlyPlaying = false;
                this.playerWindow.updateMiniplayer(false,
                        false,
                        false,
                        t0,
                        tf,
                        this.playerObject.currentSongIndex,
                        this.playerObject.amountSongs);
            }
        } catch (InterruptedException e) {}

    }
}
