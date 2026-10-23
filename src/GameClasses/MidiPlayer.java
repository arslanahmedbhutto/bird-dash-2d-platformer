package GameClasses;

import javax.sound.midi.*;
import java.io.File;

public class MidiPlayer {

    private Sequencer sequencer;

    public void play(String file, boolean loop) {
        close(); // release the previous sequencer before starting a new track
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            Sequence sequence = MidiSystem.getSequence(new File(file));
            sequencer.setSequence(sequence);

            if (loop)
                sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);

            sequencer.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (sequencer != null && sequencer.isRunning())
            sequencer.stop();
    }

    public void close() {
        if (sequencer != null) {
            sequencer.close();
            sequencer = null;
        }
    }
}