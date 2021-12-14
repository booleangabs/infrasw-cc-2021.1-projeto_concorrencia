import ui.*;

import javax.swing.*;

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
            System.out.println(this.playerObject.lastId);
            System.out.println(this.playerObject.currentSongIndex);
            this.t0 = this.playerObject.lastId == this.playerObject.currentSongIndex ? p : 0;
            int tf = Integer.parseInt(this.playerObject.currentSong[5]);
            if (this.playerObject.currentlyPlaying) {
                while (this.t0 <= tf) {
                    this.playerWindow.updateMiniplayer(true,
                            true,
                            this.playerObject.isRepeating,
                            this.t0,
                            tf,
                            this.playerObject.currentSongIndex,
                            this.playerObject.amountSongs);
                    this.t0 += 1;
                    Thread.sleep(1000);
                }
                // Caso a música acabar
                // Se essa foi a última e não estiver repetindo, parar
                if ((this.playerObject.currentSongIndex == this.playerObject.amountSongs - 1)
                        && !this.playerObject.isRepeating)
                {
                    SwingUtilities.invokeLater(() -> {
                        this.playerWindow.updatePlayPauseButton(false);
                        this.playerObject.currentlyPlaying = false;
                        this.playerWindow.updateMiniplayer(true,
                                false,
                                false,
                                tf,
                                tf,
                                this.playerObject.currentSongIndex,
                                this.playerObject.amountSongs);
                    });
                    System.out.println("Finished");
                }
                // Se for no meio da playlist continue para a próxima
                else {
                    this.playerObject.skipToNextSong();
                }

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
