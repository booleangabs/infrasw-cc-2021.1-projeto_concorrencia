import ui.*;

import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class Player {
    public boolean playerIsActive = true;
    public boolean currentlyPlaying = true;
    public String windowTitle = "Useless Player";
    public String[][] queueArray;
    public int amountSongs = 0;
    public int currentSongIndex = 0;
    public int lastId = -1;
    public String[] currentSong;

    public AddSongWindow addSongWindow;
    public WindowListener addSongWindowListener;
    public PlayerWindow playerWindow;
    public Lock lock = new ReentrantLock();
    public Condition condition = lock.newCondition();
    public boolean isBusy = false;
    public Thread scrubberThread;

    public Player() {

        // ActionListeners
        ActionListener buttonListenerPlayNow = e -> playNowListener();
        ActionListener buttonListenerStop =  e -> stopListener();
        ActionListener buttonListenerPlayPause =  e -> playPauseListener();
        ActionListener buttonListenerAddSong =  e -> addSongListener();
        ActionListener buttonListenerRemove =  e -> removeSongListener();
        ActionListener buttonListenerNext =  e -> {};
        ActionListener buttonListenerPrevious =  e -> {};
        ActionListener buttonListenerShuffle =  e -> {};
        ActionListener buttonListenerRepeat =  e -> {};

        this.queueArray = new String[1][7]; // Tava dizendo que queueArray era nulo, inicializar aqui resolveu
        this.playerWindow = new PlayerWindow(
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
        this.addSongWindowListener = this.playerWindow.getAddSongWindowListener();
        this.playerWindow.start();
    }

    private void playNowListener() {
        /* Quando o player começar a trabalhar
        * Setar atividade e estado do player para verdadeiro
        * Atualizar as informações ma janela
        * Setar o estado do botao de play e scrubber para verdadeiro
        * Iniciar a thread de update do scrubber
        */
        System.out.println("Start");
        this.playerIsActive = true;
        this.currentlyPlaying = true;
        this.playerWindow.updatePlayPauseButton(true);
        this.currentSongIndex = this.playerWindow.getSelectedSongID();
        this.currentSong = this.queueArray[currentSongIndex];
        this.playerWindow.updateMiniplayer(
                true,
                true,
                false,
                0,
                Integer.parseInt(currentSong[5]),
                currentSongIndex,
                amountSongs);
        this.playerWindow.updatePlayingSongInfo(this.currentSong[0], this.currentSong[1], this.currentSong[2]);
        this.playerWindow.enableScrubberArea();
        this.scrubberThread = new ScrubberThread(this.playerWindow, this);
        this.scrubberThread.start();
    }

    private void stopListener() {
        // Basicamente fazer o contrario do playNowListener
        System.out.println("Stop");
        this.playerIsActive = false;
        this.currentlyPlaying = false;
        this.playerWindow.resetMiniPlayer();
        this.scrubberThread.interrupt();
        this.playerWindow.disableScrubberArea();
    }

    private void playPauseListener() {
        /* Ao clicar
        * - Caso esteja tocando: pausar e interromper a thread to scrubber
        * - Caso pausado: setar o estado do player e iniciar a thread to scrubber */
        if (this.currentlyPlaying) {
            this.currentlyPlaying = false;
            System.out.println("Pause");
            this.lastId = currentSongIndex;
            this.scrubberThread.interrupt();
        }
        else {
            this.currentlyPlaying = true;
            System.out.println("Play");
            this.currentSongIndex = this.playerWindow.getSelectedSongID();
            this.currentSong = this.queueArray[this.currentSongIndex];
            this.playerWindow.updatePlayingSongInfo(this.currentSong[0], this.currentSong[1], this.currentSong[2]);
            this.scrubberThread = new ScrubberThread(this.playerWindow, this);
            this.scrubberThread.start();
        }
        this.playerWindow.updatePlayPauseButton(currentlyPlaying);
    }

    private void addSongListener() {
        System.out.println("Add");
        ActionListener addOk = e -> addSongOkListener();
        this.addSongWindow = new AddSongWindow(this.amountSongs, addOk, addSongWindowListener);
    }

    private void addSongOkListener() {
        // Copiar aqui
        new Thread(() -> {
            try {
                this.lock.lock();
                while (this.isBusy)
                    this.condition.await();
                this.isBusy = true; // até aqui
                // Insira codigo aqui
                String[] newSong = addSongWindow.getSong();
                queueArray = getUpdatedQueue(newSong);
                playerWindow.updateQueueList(queueArray);
                // Copiar daqui
                this.isBusy = false;
                this.condition.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                this.lock.unlock();
            }
        }).start();//ate aqui
    }

    private String[][] getUpdatedQueue(String[] newSong) {
        // Constroi uma playlist atualizada a partir da queue atual
        String[][] updatedQueue = new String[this.amountSongs + 1][];
        for (int i = 0; i < this.amountSongs; i++) {
            updatedQueue[i] = this.queueArray[i];
        }
        updatedQueue[this.amountSongs] = newSong;
        this.amountSongs++;
        System.out.println("Added a song!");
        return updatedQueue;
    }

    private void removeSongListener() {
        System.out.println("Remove");
        if (amountSongs == 0) {
            System.out.println("No songs to remove");
            return;
        }
        // Por planejar utilizar as ids nas features futuras
        // Remover uma música atualiza as ids das que viriam depois da que foi removida
        new Thread(() -> {
            try {
                this.lock.lock();
                while (this.isBusy)
                    this.condition.await();
                this.isBusy = true;
                String[][] updatedQueue = new String[this.amountSongs - 1][];
                int toRemove = this.playerWindow.getSelectedSongID();
                int j = 0;
                for (int i = 0; i < this.amountSongs; i++) {
                    if (i != toRemove) {
                        updatedQueue[j] = this.queueArray[i];
                        updatedQueue[j][6] = Integer.toString(j);
                        j++;
                    }
                }
                this.queueArray = updatedQueue;
                this.playerWindow.updateQueueList(updatedQueue);
                this.amountSongs--;
                if (toRemove == this.currentSongIndex)
                    this.stopListener();
                this.isBusy = false;
                this.condition.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                this.lock.unlock();
            }
        }).start();
    }

}