import ui.*;

public class ScrubberThread extends Thread {
    private final PlayerWindow playerWindow;
    private final Player playerObject;
    private int t0;

    public ScrubberThread(PlayerWindow playerWindow, Player playerObject){
        this.playerWindow = playerWindow;
        this.playerObject = playerObject;
    }

    @Override
    public void run() {
        try{
            // Utilizando scrubber value pra continuar de onde parou ou começar do zero numa nova música
            int p = this.playerWindow.getScrubberValue();
            this.t0 = this.playerObject.lastId == this.playerWindow.getSelectedSongID() ? p : 0;
            int tf = Integer.parseInt(this.playerObject.currentSong[5]);
            if (this.playerObject.currentlyPlaying) {
                Thread.sleep(1000);
                while (this.t0 < tf) {
                    this.t0 += 1;
                    this.playerWindow.updateMiniplayer(true,
                            true,
                            false,
                            this.t0,
                            tf,
                            this.playerObject.currentSongIndex,
                            this.playerObject.amountSongs);
                    Thread.sleep(1000);
                }
                // Caso a música acabar
                this.playerWindow.updatePlayPauseButton(false);
                this.playerObject.currentlyPlaying = false;
                this.playerWindow.updateMiniplayer(true,
                        false,
                        false,
                        0,
                        tf,
                        this.playerObject.currentSongIndex,
                        this.playerObject.amountSongs);
            }
        } catch (InterruptedException e) {

        }
    }
}
