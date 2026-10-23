package GameClasses;

import game2D.*;

public class Penguin extends Sprite {

    private float moveSpeed = -0.25f; // fast left movement

    public Penguin(Animation anim) {
        super(anim);
    }

    @Override
    public void update(long elapsed) {
        setVelocityX(moveSpeed);
        super.update(elapsed);
    }
}