package GameClasses;

import game2D.*;

public class Enemy extends Sprite {

    private float moveSpeed = 0.08f;
    private boolean movingRight = true;

    public Enemy(Animation anim) {
        super(anim);
    }

    @Override
    public void update(long elapsed) {

        if (movingRight) {
            setVelocityX(moveSpeed);
        } else {
            setVelocityX(-moveSpeed);
        }

        super.update(elapsed);
    }

    public void reverseDirection() {
        movingRight = !movingRight;
    }
}