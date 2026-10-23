package GameClasses;

import game2D.*;

public class Player extends Sprite {

    private float gravity = 0.0008f;
    private float jumpStrength = -0.40f;
    private float moveSpeed = 0.20f;
    private float maxFallSpeed = 0.6f;
    private int jumpCount = 0;
    private int maxJumps = 2;

    private boolean onGround = false;
    private boolean movingRight = false;
    private boolean movingLeft = false;

    public Player(Animation anim) {
        super(anim);
        jumpCount = 0;
    }

    public void moveLeft(boolean b) {
        movingLeft = b;
    }

    public void moveRight(boolean b) {
        movingRight = b;
    }

    public boolean jump() {
    if (jumpCount < maxJumps) {
        setVelocityY(jumpStrength);
        jumpCount++;
        return true;
    }
    return false;
}

    public void resetJumpCount() {
    jumpCount = 0;
}

    public void setOnGround(boolean value) {
        onGround = value;
    }

    public boolean isOnGround() {
        return onGround;
    }

    @Override
    public void update(long elapsed) {

        // Horizontal movement
        if (movingRight) {
            setVelocityX(moveSpeed);
        } else if (movingLeft) {
            setVelocityX(-moveSpeed);
        } else {
            setVelocityX(0);
        }

        // Apply gravity
        float vy = getVelocityY() + gravity * elapsed;

        if (vy > maxFallSpeed) {
            vy = maxFallSpeed;
        }

        setVelocityY(vy);

        super.update(elapsed);
    }
}