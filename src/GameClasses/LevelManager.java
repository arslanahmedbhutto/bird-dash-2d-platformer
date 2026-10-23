package GameClasses;

import game2D.TileMap;

public class LevelManager {

    private TileMap map;

    public LevelManager(TileMap map) {
        this.map = map;
    }

    public void loadLevel(String file) {
        map.loadMap("maps", file);
    }
}