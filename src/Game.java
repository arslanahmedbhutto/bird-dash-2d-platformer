
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import GameClasses.*;

import game2D.*;

// Game demonstrates how we can override the GameCore class
// to create our own 'game'. We usually need to implement at
// least 'draw' and 'update' (not including any local event handling)
// to begin the process. You should also add code to the 'init'
// method that will initialise event handlers etc. 

@SuppressWarnings("serial")

public class Game extends GameCore implements MouseListener {
    // Useful game constants
    static int screenWidth = 512;
    static int screenHeight = 384;

    // Game constants
    // float lift = 0.005f;
    // float gravity = 0.0001f;
    // float fly = -0.04f;
    // float moveSpeed = 0.05f;

    // Game state flags
    // boolean flap = false;
    boolean moveRight = false;
    boolean debug = false; // press B to show tile borders

    // Game resources
    Animation landing, walk;
    Animation enemyAnim;

    // Sprite player = null;
    Player player;
    ArrayList<Enemy> enemies = new ArrayList<>();
    Camera camera = new Camera();
    SoundManager soundManager = new SoundManager();
    LevelManager levelManager;
    ArrayList<Sprite> clouds = new ArrayList<Sprite>();
    ArrayList<Tile> collidedTiles = new ArrayList<Tile>();

    TileMap tmap = new TileMap(); // Our tile map, note that we load it in init()

    long total; // The score will be the total time elapsed since a crash

    ArrayList<Penguin> penguins = new ArrayList<>();
    Animation penguinAnim;

    long penguinSpawnTimer = 0;
    long penguinSpawnDelay = 5000; // 5 seconds

    Sprite goal;
    Animation goalAnim;

    MidiPlayer midi = new MidiPlayer();

    // parallax background variables
    double parallaxTime = 0;
    Image bgSky;
    Image bgHillsFar;
    Image bgHillsMid;
    Image bgTrees;
    Image bgClouds;
    Image bgGround;

    // level 2 variables
    int currentLevel = 1;
    int maxLevel = 2;
    boolean showLevelComplete = false;
    int completedLevel = 0;

    /**
     * The obligatory main method that creates
     * an instance of our class and starts it running
     * 
     * @param args The list of parameters this program might use (ignored)
     */
    public static void main(String[] args) {

        Game gct = new Game();
        gct.init();
        // Start in windowed mode with the given screen height and width
        gct.run(false, screenWidth, screenHeight);
    }

    enum GameState {
        MENU,
        PLAYING,
        LEVEL_COMPLETE,
        GAME_OVER
    }

    GameState gameState = GameState.MENU;

    /**
     * Initialise the class, e.g. set up variables, load images,
     * create animations, register event handlers.
     * 
     * This shows you the general principles but you should create specific
     * methods for setting up your game that can be called again when you wish to
     * restart the game (for example you may only want to load animations once
     * but you could reset the positions of sprites each time you restart the game).
     */
    public void init() {

        Sprite s; // Temporary reference to a sprite

        // Load the tile map and print it out so we can check it is valid
        // tmap.loadMap("maps", "map1.txt");
        // loadLevel(currentLevel);
        currentLevel = 1;
        setSize(screenWidth, screenHeight);

        // setSize(tmap.getPixelWidth() / 4, tmap.getPixelHeight());
        setVisible(true);
        midi.play("sounds/start.mid", true);
        addMouseListener(this);

        levelManager = new LevelManager(tmap);

        penguinAnim = new Animation();
        penguinAnim.addFrame(loadImage("images/penguin.png"), 100);

        goalAnim = new Animation();
        goalAnim.addFrame(loadImage("images/flag.png"), 1000);

        // Create a set of background sprites that we can
        // rearrange to give the illusion of motion

        landing = new Animation();
        landing.loadAnimationFromSheet("images/landbird.png", 4, 1, 60);

        // Example of using loadAnimationSeries to load a sequence of images
        // from a sprite sheet. This will load 5 images from sprite sheet
        // "images/walk.png" starting at position 6 (where the top left image
        // is 0). Parameters (3,4) indicate the sprite sheet consists of a grid
        // of 3 columns and 4 rows. To switch to this animation, press the A key.
        walk = new Animation();
        walk.loadAnimationSeries("images/walk.png", 3, 4, 100, 6, 5);

        enemyAnim = new Animation();
        enemyAnim.addFrame(loadImage("images/pipebot.png"), 1000);

        // penguin = new Penguin(penguinAnim);
        // penguin.setPosition(tmap.getPixelWidth() - 50, 200);
        // penguin.show();

        // Initialise the player with an animation
        player = new Player(landing);

        bgSky = loadImage("images/layer07_Sky.png");
        bgHillsFar = loadImage("images/layer06_Rocks.png");
        bgHillsMid = loadImage("images/layer03_Hills_1.png");
        bgTrees = loadImage("images/layer02_Trees.png");
        bgClouds = loadImage("images/layer05_Clouds.png");
        bgGround = loadImage("images/layer01_Ground.png");

        // The layers are 1920x1080; scale them once to the window height so the
        // hills and trees (in the lower part of each image) are actually visible.
        bgSky = scaleToHeight(bgSky, screenHeight);
        bgHillsFar = scaleToHeight(bgHillsFar, screenHeight);
        bgHillsMid = scaleToHeight(bgHillsMid, screenHeight);
        bgTrees = scaleToHeight(bgTrees, screenHeight);
        bgClouds = scaleToHeight(bgClouds, screenHeight);

        // Load a single cloud animation
        Animation ca = new Animation();
        ca.addFrame(loadImage("images/cloud.png"), 1000);

        // Create 3 clouds at random positions off the screen
        // to the right
        for (int c = 0; c < 3; c++) {
            s = new Sprite(ca);
            s.setX(screenWidth + (int) (Math.random() * 200.0f));
            s.setY(30 + (int) (Math.random() * 150.0f));
            s.setVelocityX(-0.02f);
            s.show();
            clouds.add(s);
        }

        goal = new Sprite(goalAnim);

        // Position at end of map
        float goalX = tmap.getPixelWidth() - 80;
        float goalY = tmap.getPixelHeight() - 160; // adjust to align ground

        goal.setPosition(goalX, goalY);
        goal.show();

        // initialiseGame();
        // spawnEnemies();
        loadLevel(currentLevel);

        // System.out.println(tmap);
        // System.out.println("Map width px: " + tmap.getPixelWidth());
        // System.out.println("Map height px: " + tmap.getPixelHeight());
    }

    /**
     * You will probably want to put code to restart a game in
     * a separate method so that you can call it when restarting
     * the game when the player loses.
     */
    // public void initialiseGame() {
    // total = 0;

    // player.setPosition(200, 200);
    // player.setVelocity(0, 0);
    // player.show();
    // }

    public void spawnEnemies() {
        int tileWidth = tmap.getTileWidth();
        int tileHeight = tmap.getTileHeight();

        for (int x = 0; x < tmap.getMapWidth(); x++) {
            for (int y = 0; y < tmap.getMapHeight(); y++) {

                if (tmap.getTileChar(x, y) == 'E') {

                    float worldX = x * tileWidth;
                    float worldY = y * tileHeight;

                    Enemy enemy = new Enemy(enemyAnim);
                    enemy.setPosition(worldX, worldY);
                    enemy.show();

                    enemies.add(enemy);

                    // Remove the E tile so it becomes empty
                    tmap.setTileChar('.', x, y);
                }
            }
        }
    }

    public void triggerLevelComplete() {

        if (gameState == GameState.LEVEL_COMPLETE)
            return;

        completedLevel = currentLevel;
        soundManager.playSound("sounds/complete.wav");
        midi.stop();

        gameState = GameState.LEVEL_COMPLETE;
        showLevelComplete = true;
    }

    /** Draws a centred message panel (used for game over and level complete). */
    private void drawBanner(Graphics2D g, String title, Color colour, String hint) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(w / 2 - 190, h / 2 - 55, 380, 95, 20, 20);
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.setColor(colour);
        g.drawString(title, (w - g.getFontMetrics().stringWidth(title)) / 2, h / 2 - 8);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.setColor(Color.WHITE);
        g.drawString(hint, (w - g.getFontMetrics().stringWidth(hint)) / 2, h / 2 + 22);
    }

    private Image scaleToHeight(Image img, int height) {
        if (img == null || img.getHeight(null) <= 0)
            return img;
        int width = img.getWidth(null) * height / img.getHeight(null);
        java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(width, height,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = scaled.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        sg.drawImage(img, 0, 0, width, height, null);
        sg.dispose();
        return scaled;
    }

    private void drawLayer(Graphics2D g, Image img, double camX, double speed) {

        if (img == null)
            return;

        int imgWidth = img.getWidth(null);

        // Parallax offset
        int x = (int) ((-camX * speed) % imgWidth);

        if (img == bgClouds) {
            x += parallaxTime;
        }

        // Tile horizontally for long maps
        for (int i = -1; i <= 3; i++) {
            g.drawImage(img, x + i * imgWidth, 0, null);
        }
    }

    private void drawParallax(Graphics2D g) {

        // int screenW = getWidth();
        // int screenH = getHeight();

        double camX = camera.getX();

        drawLayer(g, bgSky, camX, 0.1);
        drawLayer(g, bgHillsFar, camX, 0.2);
        drawLayer(g, bgHillsMid, camX, 0.3);
        drawLayer(g, bgTrees, camX, 0.5);
        drawLayer(g, bgClouds, camX, 0.7);
    }

    public void loadLevel(int levelNumber) {

        String fileName = (levelNumber == 1) ? "map1.txt" : "map2.txt";

        // CLEAR
        penguins.clear();
        enemies.clear();
        collidedTiles.clear();

        // LOAD MAP FIRST
        tmap.loadMap("maps", fileName);

        // NOW calculate ground properly
        int tileH = tmap.getTileHeight();
        int spawnTileY = tmap.getMapHeight() - 3;
        int groundY = spawnTileY * tileH - player.getHeight();

        // Spawn enemies
        spawnEnemies();

        // Correct player position
        player.setPosition(150, groundY);
        player.setVelocity(0, 0);
        player.resetJumpCount();

        // Reposition goal correctly
        float goalX = tmap.getPixelWidth() - 80;
        float goalY = tmap.getPixelHeight() - 160;
        goal.setPosition(goalX, goalY);
        goal.show();
    }

    /**
     * Draw the current state of the game. Note the sample use of
     * debugging output that is drawn directly to the game screen.
     */
    public void draw(Graphics2D g) {

        if (gameState == GameState.MENU) {

            g.setColor(new Color(135, 206, 235));
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 34));
            String title = "BIRD DASH";
            g.drawString(title, (getWidth() - g.getFontMetrics().stringWidth(title)) / 2, 100);
            g.setFont(new Font("Arial", Font.PLAIN, 14));
            String subtitle = "Reach the flag. Avoid the penguins, spikes and gaps.";
            g.drawString(subtitle, (getWidth() - g.getFontMetrics().stringWidth(subtitle)) / 2, 130);

            // Level 1 Button
            g.setColor(Color.GREEN);
            g.fillRect(getWidth() / 2 - 100, 170, 200, 50);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString("PLAY LEVEL 1", getWidth() / 2 - 75, 200);

            // Level 2 Button
            g.setColor(Color.ORANGE);
            g.fillRect(getWidth() / 2 - 100, 240, 200, 50);
            g.setColor(Color.BLACK);
            g.drawString("PLAY LEVEL 2", getWidth() / 2 - 75, 270);

            // Controls Info
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString("Controls: (← / →  : Move) | (SPACE : Jump) | (R : Restart) | (M : Back to Menu) ",
                    getWidth() / 2 - 220, getHeight() - 30);

            return;
        }
        // Be careful about the order in which you draw objects - you
        // should draw the background first, then work your way 'forward'

        // First work out how much we need to shift the view in order to
        // see where the player is. To do this, we adjust the offset so that
        // it is relative to the player's position along with a shift
        // int xo = -(int)player.getX() + 200;
        // int yo = -(int)player.getY() + 200;
        camera.update(player.getX(), player.getY(), getWidth(), getHeight());
        int xo = -camera.getX();
        int yo = -camera.getY();

        // g.setColor(Color.white);
        // g.fillRect(0, 0, getWidth(), getHeight());

        drawParallax(g);
        // Apply offsets to sprites then draw them
        for (Sprite s : clouds) {
            s.setOffsets(xo, yo);
            s.draw(g);
        }

        // Apply offsets to tile map and draw it
        tmap.draw(g, xo, yo);

        goal.setOffsets(xo, yo);
        goal.draw(g);

        // Apply offsets to player and draw
        player.setOffsets(xo, yo);
        player.draw(g);
        for (Enemy e : enemies) {
            e.setOffsets(xo, yo);
            e.draw(g);
        }
        for (Penguin p : penguins) {
            p.setOffsets(xo, yo);
            p.draw(g);
        }

        // Show score and status information
        String msg = String.format("Score: %d", total / 100);
        g.drawString(msg, getWidth() - 120, 50);
        g.setColor(Color.darkGray);
        g.drawString("Level: " + currentLevel, 20, 50);

        if (debug && gameState == GameState.PLAYING) {

            // When in debug mode, you could draw borders around objects
            // and write messages to the screen with useful information.
            // Try to avoid printing to the console since it will produce
            // a lot of output and slow down your game.

            g.setStroke(new BasicStroke(1));
            tmap.drawBorder(g, xo, yo, Color.DARK_GRAY);

            g.setStroke(new BasicStroke(1));

            // g.setColor(Color.red);
            // player.drawBoundingBox(g);

            // g.drawString(String.format("Player: %.0f,%.0f", player.getX(),
            // player.getY()),
            // getWidth() - 100, 70);

            // drawCollidedTiles(g, tmap, xo, yo);
        }

        if (gameState == GameState.GAME_OVER) {
            drawBanner(g, "GAME OVER", new Color(220, 38, 38), "Press R to restart or M for the menu");
        }
        if (gameState == GameState.LEVEL_COMPLETE && showLevelComplete) {
            drawBanner(g, "LEVEL " + completedLevel + " COMPLETE!", new Color(22, 163, 74), "Press M to return to the menu");
        }

    }

    public void drawCollidedTiles(Graphics2D g, TileMap map, int xOffset, int yOffset) {
        if (collidedTiles.size() > 0) {
            int tileWidth = map.getTileWidth();
            int tileHeight = map.getTileHeight();

            g.setColor(Color.blue);
            for (Tile t : collidedTiles) {
                g.drawRect(t.getXC() + xOffset, t.getYC() + yOffset, tileWidth, tileHeight);
            }
        }
    }

    public boolean isGoalTouched() {

        Rectangle playerBox = new Rectangle(
                (int) player.getX(),
                (int) player.getY(),
                player.getWidth(),
                player.getHeight());

        // Make smaller hitbox around actual flag pole
        Rectangle goalBox = new Rectangle(
                (int) goal.getX() + goal.getWidth() / 3,
                (int) goal.getY(),
                goal.getWidth() / 3,
                goal.getHeight());

        return playerBox.intersects(goalBox);
    }

    /**
     * Update any sprites and check for collisions
     * 
     * @param elapsed The elapsed time between this call and the previous call of
     *                elapsed
     */

    public void update(long elapsed) {
        // Handle timed penguin spawning
        penguinSpawnTimer += elapsed;

        // parralax movement
        parallaxTime += elapsed * 0.02;

        if (gameState != GameState.PLAYING)
            return;

        if (gameState == GameState.PLAYING) {

            for (Sprite s : clouds)
                s.update(elapsed);
            player.update(elapsed);
            checkTileCollision(player, tmap);

            for (Enemy e : enemies) {
                e.update(elapsed);
                checkTileCollision(e, tmap);
                if (gameState == GameState.PLAYING && boundingBoxCollision(player, e)) {
                    soundManager.playSound("sounds/death.wav");
                    triggerGameOver();
                }
            }
            // Increase score only when moving right
            if (player.getVelocityX() > 0) {
                total += elapsed;
            }

            // Spawn penguins periodically
            // Spawn penguins every 5 seconds
            if (penguinSpawnTimer >= penguinSpawnDelay) {

                // reset timer before spawning to ensure consistent intervals even if spawning
                // takes time
                penguinSpawnTimer = 0;

                Penguin p = new Penguin(penguinAnim);

                // Spawn at ground level
                int randomOffset = 120 + (int) (Math.random() * 51); // 120–170
                int groundY = tmap.getPixelHeight() - player.getHeight() - randomOffset;

                ; // adjust if slightly floating

                p.setPosition(tmap.getPixelWidth() - 10, groundY);
                p.show();
                soundManager.playSound("sounds/caw.wav");

                penguins.add(p);

            }

            // if (boundingBoxCollision(player, goal)) {
            // triggerLevelComplete();
            // }

            // Update all penguins
            for (int i = penguins.size() - 1; i >= 0; i--) {

                Penguin p = penguins.get(i);

                p.update(elapsed);
                // checkTileCollision(p, tmap);

                // Remove only when fully off world
                if (p.getX() + p.getWidth() < 0) {
                    penguins.remove(i);
                    continue;
                }

                // Collision check
                if (boundingBoxCollision(player, p)) {
                    soundManager.playSound("sounds/death.wav");
                    triggerGameOver();
                }
            }

            // Hole check
            if (gameState == GameState.PLAYING && player.getY() > tmap.getPixelHeight()) {
                soundManager.playSound("sounds/death.wav");
                triggerGameOver();
            }
        }
        if (isGoalTouched()) {
            triggerLevelComplete();
            return;
        }
    }

    /**
     * Checks and handles collisions with the edge of the screen. You should
     * generally
     * use tile map collisions to prevent the player leaving the game area. This
     * method
     * is only included as a temporary measure until you have properly developed
     * your
     * tile maps.
     * 
     * @param s       The Sprite to check collisions for
     * @param tmap    The tile map to check
     * @param elapsed How much time has gone by since the last call
     */
    public void handleScreenEdge(Sprite s, TileMap tmap, long elapsed) {
        // This method just checks if the sprite has gone off the bottom screen.
        // Ideally you should use tile collision instead of this approach

        // float difference = s.getY() + s.getHeight() - tmap.getPixelHeight();
        // if (difference > 0)
        // {
        // // Put the player back on the map according to how far over they were
        // s.setY(tmap.getPixelHeight() - s.getHeight() - (int)(difference));

        // // and make them bounce
        // s.setVelocityY(-s.getVelocityY()*0.75f);
        // }
        if (s.getY() > tmap.getPixelHeight()) {
            s.setY(100);
            s.setVelocityY(0);
        }

    }

    public void triggerGameOver() {

        if (gameState == GameState.GAME_OVER)
            return;

        gameState = GameState.GAME_OVER;

        // Switch to dying animation
        player.setAnimation(walk);
        player.setVelocity(0, 0);
        soundManager.playSound("sounds/gameover.wav");
        midi.stop();
    }

    public void restartGame() {

        gameState = GameState.PLAYING;

        // Reset penguins
        penguins.clear();

        penguinSpawnTimer = 0;
        total = 0;

        // Reset player animation back to normal
        player.setAnimation(landing);

        // Reset player state
        player.resetJumpCount();
        player.setVelocity(0, 0);

        loadLevel(currentLevel);
        midi.stop();
        midi.play("sounds/background.midi", true);
    }

    /**
     * Override of the keyPressed event defined in GameCore to catch our
     * own events
     * 
     * @param e The event that has been generated
     */
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch (key) {
            case KeyEvent.VK_RIGHT:
                player.moveRight(true);
                break;

            case KeyEvent.VK_LEFT:
                player.moveLeft(true);
                break;

            case KeyEvent.VK_SPACE:
                if (gameState == GameState.PLAYING) {
                    if (player.jump()) {
                        soundManager.playSound("sounds/jump.wav");
                    }
                }
                break;

            case KeyEvent.VK_R:
                if (gameState == GameState.GAME_OVER) {
                    restartGame();
                }
                break;

            case KeyEvent.VK_ESCAPE:
                stop();
                break;

            case KeyEvent.VK_B:
                debug = !debug;
                break;
            case KeyEvent.VK_M:
                gameState = GameState.MENU;
                penguins.clear();
                enemies.clear();
                collidedTiles.clear();
                player.setAnimation(landing);
                player.setVelocity(0, 0);
                player.resetJumpCount();

                total = 0;
                penguinSpawnTimer = 0;
                showLevelComplete = false;
                midi.stop();
                midi.play("sounds/start.mid", true);
                break;

            default:
                break;
        }
    }

    /**
     * Use the sample code in the lecture notes to properly detect
     * a bounding box collision between sprites s1 and s2.
     * 
     * @return true if a collision may have occurred, false if it has not.
     */
    public boolean boundingBoxCollision(Sprite s1, Sprite s2) {

        Rectangle r1 = new Rectangle(
                (int) s1.getX() + 8,
                (int) s1.getY() + 8,
                s1.getWidth() - 16,
                s1.getHeight() - 16);

        Rectangle r2 = new Rectangle(
                (int) s2.getX() + 8,
                (int) s2.getY() + 8,
                s2.getWidth() - 16,
                s2.getHeight() - 16);

        return r1.intersects(r2);
    }

    /**
     * Check and handles collisions with a tile map for the
     * given sprite 's'. Initial functionality is limited...
     * 
     * @param s    The Sprite to check collisions for
     * @param tmap The tile map to check
     */
    public void checkTileCollision(Sprite p, TileMap map) {
        collidedTiles.clear();

        float x = p.getX();
        float y = p.getY();

        float dx = p.getVelocityX();
        float dy = p.getVelocityY();

        float tileWidth = map.getTileWidth();
        float tileHeight = map.getTileHeight();

        // === HORIZONTAL COLLISION ===
        x += dx;

        int leftTile = (int) (x / tileWidth);
        int rightTile = (int) ((x + p.getWidth() - 1) / tileWidth);
        int topTile = (int) (y / tileHeight);
        int bottomTile = (int) ((y + p.getHeight() - 2) / tileHeight);

        if (dx > 0) { // moving right
            Tile t = map.getTile(rightTile, topTile);
            Tile b = map.getTile(rightTile, bottomTile);

            if ((t != null && t.getCharacter() != '.' && p.getY() + p.getHeight() > t.getYC()) ||
                    (b != null && b.getCharacter() != '.' && p.getY() < b.getYC() + tileHeight)) {

                x = rightTile * tileWidth - p.getWidth();
                dx = 0;
            }
        } else if (dx < 0) { // moving left
            Tile t = map.getTile(leftTile, topTile);
            Tile b = map.getTile(leftTile, bottomTile);

            if ((t != null && t.getCharacter() != '.') ||
                    (b != null && b.getCharacter() != '.')) {

                x = (leftTile + 1) * tileWidth;
                dx = 0;
            }
        }

        // === VERTICAL COLLISION ===
        y += dy;

        leftTile = (int) (x / tileWidth);
        rightTile = (int) ((x + p.getWidth() - 1) / tileWidth);
        topTile = (int) (y / tileHeight);
        bottomTile = (int) ((y + p.getHeight() - 1) / tileHeight);

        if (p instanceof Player) {
            ((Player) p).setOnGround(false);
        }

        if (dy > 0) { // falling
            Tile l = map.getTile(leftTile, bottomTile);
            Tile r = map.getTile(rightTile, bottomTile);

            if ((l != null && l.getCharacter() != '.') ||
                    (r != null && r.getCharacter() != '.')) {

                y = bottomTile * tileHeight - p.getHeight();
                dy = 0;
                // p.setOnGround(true);
                if (p instanceof Player) {
                    Player playerRef = (Player) p;
                    playerRef.setOnGround(true);
                    playerRef.resetJumpCount();
                }

            }
        } else if (dy < 0) { // jumping up
            Tile l = map.getTile(leftTile, topTile);
            Tile r = map.getTile(rightTile, topTile);

            if ((l != null && l.getCharacter() != '.') ||
                    (r != null && r.getCharacter() != '.')) {

                y = (topTile + 1) * tileHeight;
                dy = 0;
            }
        }
        if (y < 0) {
            y = 0;
            dy = 0;
        }

        p.setPosition(x, y);
        p.setVelocity(dx, dy);
    }

    public void keyReleased(KeyEvent e) {

        int key = e.getKeyCode();

        switch (key) {
            case KeyEvent.VK_RIGHT:
                player.moveRight(false);
                break;
            case KeyEvent.VK_LEFT:
                player.moveLeft(false);
                break;
            case KeyEvent.VK_ESCAPE:
                stop();
                break;
            // case KeyEvent.VK_UP : flap = false; break;
            default:
                break;
        }
    }

    private void startSelectedLevel() {

        gameState = GameState.PLAYING;

        // RESET STATE COMPLETELY
        total = 0;
        penguinSpawnTimer = 0;

        penguins.clear();
        enemies.clear();
        collidedTiles.clear();

        player.setAnimation(landing);
        player.setVelocity(0, 0);
        player.resetJumpCount();
        showLevelComplete = false;

        midi.stop();
        midi.play("sounds/background.midi", true);

        loadLevel(currentLevel);
        player.show();
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        if (gameState != GameState.MENU)
            return;

        int mx = e.getX();
        int my = e.getY();

        int centerX = getWidth() / 2 - 100;

        // LEVEL 1 button area
        if (mx >= centerX && mx <= centerX + 200 &&
                my >= 170 && my <= 220) {

            currentLevel = 1;
            startSelectedLevel();
        }

        // LEVEL 2 button area
        if (mx >= centerX && mx <= centerX + 200 &&
                my >= 240 && my <= 290) {

            currentLevel = 2;
            startSelectedLevel();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
