import ui.*;

import javax.swing.*;
import java.awt.event.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class Player {
    public boolean playerIsActive = false;
    public boolean currentlyPlaying = false;
    public String windowTitle = "Useless Player";
    public String[][] queueArray = new String[][] {{"", "", "", "", "", "", ""}};
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
    public boolean wasPaused = false;
    public boolean clickedNextOrPrevious = false;
    public boolean isRepeating = false;
    public boolean isShuffling = false;

    public Player() {

        // ActionListeners
        ActionListener buttonListenerPlayNow = e -> playNowListener();
        ActionListener buttonListenerStop =  e -> stopListener();
        ActionListener buttonListenerPlayPause =  e -> playPauseListener();
        ActionListener buttonListenerAddSong =  e -> addSongListener();
        ActionListener buttonListenerRemove =  e -> removeSongListener();

        ActionListener buttonListenerNext =  e -> nextSongListener();
        ActionListener buttonListenerPrevious =  e -> previousSongListener();
        ActionListener buttonListenerShuffle =  e -> shuffleListener();
        ActionListener buttonListenerRepeat =  e -> repeatListener();

        // Mouse listeners
        MouseListener scrubberListenerClick = new MouseListener(){
            @Override
            public void mousePressed(MouseEvent e){
                onMousePress();
            }

            @Override
            public void mouseReleased(MouseEvent e){
                onMouseRelease();
            }

            @Override
            public void mouseEntered(MouseEvent e){}

            @Override
            public void mouseExited(MouseEvent e){}

            @Override
            public void mouseClicked(MouseEvent e) {}
        };

        MouseMotionListener scrubberListenerMotion = new MouseMotionListener(){
            @Override
            public void mouseDragged(MouseEvent e) {
                onMouseDrag();
            }

            @Override
            public void mouseMoved(MouseEvent e){}
        };

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
                                    scrubberListenerClick,
                                    scrubberListenerMotion,
                                    this.windowTitle,
                                    this.queueArray);
        this.addSongWindowListener = this.playerWindow.getAddSongWindowListener();
        this.playerWindow.start();
    }

    private void onMousePress() {
        System.out.println("Waiting for scrubber drag");
        new Thread(() -> {
            this.lock.lock();
            try {
                while (this.isBusy)
                    this.condition.await();
                this.isBusy = true;

                if (this.currentlyPlaying) {
                    this.wasPaused = false;
                    this.currentlyPlaying = false;
                    this.lastId = currentSongIndex;
                    this.scrubberThread.interrupt();
                    SwingUtilities.invokeLater(() -> {
                        this.playerWindow.updatePlayPauseButton(this.currentlyPlaying);
                    });
                }
                else {
                    this.wasPaused = true;
                }

                this.isBusy = false;
                this.condition.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                this.lock.unlock();
            }
        }).start();
    }

    private void onMouseRelease() {
        if (!this.wasPaused) {
            new Thread(() -> {
                this.lock.lock();
                try {
                    while (this.isBusy)
                        this.condition.await();
                    this.isBusy = true;

                    this.currentlyPlaying = true;
                    this.scrubberThread = new ScrubberThread(this.playerWindow, this);
                    Thread.sleep(500);
                    SwingUtilities.invokeLater(() -> {
                        this.playerWindow.updatePlayPauseButton(this.currentlyPlaying);
                    });
                    this.scrubberThread.start();

                    this.isBusy = false;
                    this.condition.signalAll();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    this.lock.unlock();
                }
            }).start();
        }
        System.out.println("Resume from " + this.playerWindow.getScrubberValue() + "s on.");
    }

    private void onMouseDrag() {
        SwingUtilities.invokeLater(() -> {
            this.playerWindow.updateMiniplayer(true,
                    false,
                    false,
                    this.playerWindow.getScrubberValue(),
                    Integer.parseInt(this.currentSong[5]),
                    this.currentSongIndex,
                    this.amountSongs);
        });
    }

    private void playNowListener() {
        /* Quando o player começar a trabalhar
        * Setar atividade e estado do player para verdadeiro
        * Atualizar as informações ma janela
        * Setar o estado do botao de play e scrubber para verdadeiro
        * Iniciar a thread de update do scrubber
        */
        new Thread(() -> {
            System.out.println("Start");
            this.lock.lock();
            try {
                while (this.isBusy)
                    this.condition.await();
                this.isBusy = true;

                if (this.currentlyPlaying) { this.scrubberThread.interrupt(); }
                this.playerIsActive = true;
                this.currentlyPlaying = true;
                this.playerWindow.updatePlayPauseButton(true);
                this.currentSongIndex = this.playerWindow.getSelectedSongID();
                this.currentSong = this.queueArray[currentSongIndex];
                SwingUtilities.invokeLater(() -> {
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
                });
                this.scrubberThread = new ScrubberThread(this.playerWindow, this);
                this.scrubberThread.start();

                this.isBusy = false;
                this.condition.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                this.lock.unlock();
            }
        }).start();
    }

    private void stopListener() {
        // Basicamente fazer o contrario do playNowListener
        System.out.println("Stop");
        new Thread(() -> {
            this.lock.lock();
            try {
                while (this.isBusy)
                    this.condition.await();
                this.isBusy = true;

                this.playerIsActive = false;
                this.currentlyPlaying = false;
                this.scrubberThread.interrupt();

                SwingUtilities.invokeLater(() -> {
                    this.playerWindow.resetMiniPlayer();
                    this.playerWindow.disableScrubberArea();
                });

                this.isBusy = false;
                this.condition.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                this.lock.unlock();
            }
        }).start();
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
            // Evita começar a música antiga de novo (quando clicar no next/previous enquanto estiver pausado)
            if (this.clickedNextOrPrevious) { this.clickedNextOrPrevious = false;}
            else { this.currentSongIndex = this.playerWindow.getSelectedSongID(); }
            this.currentSong = this.queueArray[this.currentSongIndex];
            SwingUtilities.invokeLater(() -> {
                this.playerWindow.updatePlayingSongInfo(this.currentSong[0], this.currentSong[1], this.currentSong[2]);
            });
            this.scrubberThread = new ScrubberThread(this.playerWindow, this);
            this.scrubberThread.start();
        }
        this.playerWindow.updatePlayPauseButton(this.currentlyPlaying);
    }

    private void addSongListener() {
        System.out.println("Add");
        ActionListener addOk = e -> addSongOkListener();
        this.addSongWindow = new AddSongWindow(this.amountSongs, addOk, addSongWindowListener);
    }

    private void addSongOkListener() {
        // Copiar aqui
        new Thread(() -> {
            this.lock.lock();
            try {
                while (this.isBusy)
                    this.condition.await();
                this.isBusy = true;
                // Insira codigo aqui
                String[] newSong = addSongWindow.getSong();
                queueArray = getUpdatedQueue(newSong);
                SwingUtilities.invokeLater(() -> {
                    playerWindow.updateQueueList(queueArray);
                });

                this.isBusy = false;
                this.condition.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                this.lock.unlock();
            }
        }).start();
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
        // Por planejar utilizar as ids nas features futuras
        // Remover uma música atualiza as ids das que viriam depois da que foi removida
        new Thread(() -> {
            this.lock.lock();
            try {
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

                SwingUtilities.invokeLater(() -> {
                    this.playerWindow.updateQueueList(updatedQueue);
                });
                this.amountSongs--;
                if (toRemove == this.currentSongIndex){
                    this.stopListener();
                }

                this.isBusy = false;
                this.condition.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                this.lock.unlock();
            }
        }).start();
    }

    private void nextSongListener() {
        System.out.println("Next");
        new Thread(() -> {
            this.lock.lock();
            try {
                while (this.isBusy)
                    this.condition.await();
                this.isBusy = true;

                // interromper a thread do scrubber
                this.scrubberThread.interrupt();
                if (!this.currentlyPlaying) { this.clickedNextOrPrevious = true; }
                if ((this.currentSongIndex == this.amountSongs - 1) & this.isRepeating){
                    this.currentSongIndex = 0;
                }
                else {this.currentSongIndex++;}
                System.out.println(this.currentSongIndex);
                this.currentSong = this.queueArray[this.currentSongIndex];
                SwingUtilities.invokeLater(() -> {
                    this.playerWindow.updateMiniplayer(true,
                            false,
                            false,
                            0,
                            Integer.parseInt(this.currentSong[5]),
                            this.currentSongIndex,
                            this.amountSongs);
                    this.playerWindow.updatePlayingSongInfo(this.currentSong[0], this.currentSong[1], this.currentSong[2]);
                });

                //iniciar nova thread
                if (this.currentlyPlaying) {
                    this.scrubberThread = new ScrubberThread(this.playerWindow, this);
                    this.scrubberThread.start();
                }

                this.isBusy = false;
                this.condition.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                this.lock.unlock();
            }
        }).start();
    }

    // Só planejamos utilizar essa função ao fim de uma música
    public void skipToNextSong() {
        this.nextSongListener();
    }

    private void previousSongListener() {
        System.out.println("Previous");
        new Thread(() -> {
            this.lock.lock();
            try {
                while (this.isBusy)
                    this.condition.await();
                this.isBusy = true;

                // interromper a thread do scrubber
                this.scrubberThread.interrupt();
                if (!this.currentlyPlaying) { this.clickedNextOrPrevious = true; }
                if ((this.currentSongIndex == 0) & this.isRepeating){
                    this.currentSongIndex = this.amountSongs - 1;
                }
                else {this.currentSongIndex--;}
                System.out.println(this.currentSongIndex);
                this.currentSong = this.queueArray[this.currentSongIndex];
                SwingUtilities.invokeLater(() -> {
                    this.playerWindow.updateMiniplayer(true,
                            false,
                            false,
                            0,
                            Integer.parseInt(this.currentSong[5]),
                            this.currentSongIndex,
                            this.amountSongs);
                    this.playerWindow.updatePlayingSongInfo(this.currentSong[0], this.currentSong[1], this.currentSong[2]);
                });

                //iniciar nova thread
                if (this.currentlyPlaying) {
                    this.scrubberThread = new ScrubberThread(this.playerWindow, this);
                    this.scrubberThread.start();
                }

                this.isBusy = false;
                this.condition.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                this.lock.unlock();
            }
        }).start();
    }

    private void repeatListener() {
        System.out.println("Repeat");
        new Thread(() -> {
            this.lock.lock();
            try {
                while (this.isBusy)
                    this.condition.await();
                this.isBusy = true;

                this.isRepeating = !this.isRepeating;

                this.isBusy = false;
                this.condition.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                this.lock.unlock();
            }
        }).start();
    }

    private void shuffleListener() {
        System.out.println("Shuffle");
        new Thread(() -> {
            this.lock.lock();
            try {
                while (this.isBusy)
                    this.condition.await();
                this.isBusy = true;

                this.isShuffling = !this.isShuffling;

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