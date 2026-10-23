package GameClasses;

public class Camera {

    private int x;
    private int y;

    public void update(float playerX, float playerY, int screenW, int screenH) {
        x = (int)playerX - screenW / 2;
        y = (int)playerY - screenH / 2;
    }

    public int getX() { return x; }
    public int getY() { return y; }
}