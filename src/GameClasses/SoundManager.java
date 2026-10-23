package GameClasses;

public class SoundManager {

    public void playSound(String filename) {
        game2D.Sound s = new game2D.Sound(filename);
        s.start();
    }
}