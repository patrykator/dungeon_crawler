import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;

public class DungeonCrawler extends JPanel {

    private static final int TILE_SIZE = 32;
    private static final int WIDTH = 42; // 16:9 ratio for larger size
    private static final int HEIGHT = 24; // 16:9 ratio for larger size
    private static final int FLOORS = 5; // Zmiana na 5 pięter
    private static final int PLAYER_START_FLOOR = 2; // Gracz zaczyna na 3. piętrze

    private char[][][] dungeons;
    private int currentFloor;
    private int playerX, playerY;
    private int goalX, goalY;
    private boolean gameWon;
    private boolean onStairTile;

    public DungeonCrawler() {
        setPreferredSize(new Dimension(WIDTH * TILE_SIZE, HEIGHT * TILE_SIZE));
        setBackground(Color.BLACK);
        generateDungeons();
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    if (onStairTile) {
                        changeFloor();
                    }
                } else {
                    movePlayer(e.getKeyCode());
                }
                repaint();
            }
        });
    }

    private void restartGame() {
        generateDungeons();
        repaint();
    }

    private void generateDungeons() {
        dungeons = new char[FLOORS][HEIGHT][WIDTH];
        Random rand = new Random();

        for (int f = 0; f < FLOORS; f++) {
            generateDungeon(dungeons[f], f == FLOORS - 1);
        }

        // Add stairs between floors
        for (int f = 0; f < FLOORS - 1; f++) {
            addStairs(dungeons[f], dungeons[f + 1]);
        }

        // Start position on the selected floor
        currentFloor = PLAYER_START_FLOOR;
        do {
            playerX = rand.nextInt(WIDTH);
            playerY = rand.nextInt(HEIGHT);
        } while (dungeons[currentFloor][playerY][playerX] == '#');
        dungeons[currentFloor][playerY][playerX] = 'P';

        gameWon = false;
        onStairTile = false;
    }

    private void generateDungeon(char[][] dungeon, boolean isGoalFloor) {
        for (int y = 0; y < HEIGHT; y++) {
            Arrays.fill(dungeon[y], '#');
        }

        // Use DFS to generate a maze
        Stack<int[]> stack = new Stack<>();
        Random rand = new Random();

        int startX = rand.nextInt(WIDTH);
        int startY = rand.nextInt(HEIGHT);
        stack.push(new int[]{startX, startY});
        dungeon[startY][startX] = '.';

        int[][] directions = {
                {0, 1}, {1, 0}, {0, -1}, {-1, 0}
        };

        while (!stack.isEmpty()) {
            int[] current = stack.peek();
            java.util.List<int[]> neighbors = new ArrayList<>();

            for (int[] dir : directions) {
                int nx = current[0] + dir[0] * 2;
                int ny = current[1] + dir[1] * 2;
                if (nx >= 0 && nx < WIDTH && ny >= 0 && ny < HEIGHT && dungeon[ny][nx] == '#') {
                    neighbors.add(new int[]{nx, ny});
                }
            }

            if (neighbors.isEmpty()) {
                stack.pop();
            } else {
                int[] chosen = neighbors.get(rand.nextInt(neighbors.size()));
                int betweenX = (current[0] + chosen[0]) / 2;
                int betweenY = (current[1] + chosen[1]) / 2;
                dungeon[betweenY][betweenX] = '.';
                dungeon[chosen[1]][chosen[0]] = '.';
                stack.push(chosen);
            }
        }

        if (isGoalFloor) {
            // Ensure goal position is reachable and not the same as the player's starting position
            do {
                goalX = rand.nextInt(WIDTH);
                goalY = rand.nextInt(HEIGHT);
            } while (dungeon[goalY][goalX] == '#' || (goalX == startX && goalY == startY));
            dungeon[goalY][goalX] = 'G';
        }
    }

    private void addStairs(char[][] currentDungeon, char[][] nextDungeon) {
        Random rand = new Random();
        int upX, upY, downX, downY;

        // Ensure stairs up and down are reachable on current dungeon
        do {
            upX = rand.nextInt(WIDTH);
            upY = rand.nextInt(HEIGHT);
        } while (currentDungeon[upY][upX] != '.' || !isReachable(currentDungeon, playerX, playerY, upX, upY));
        currentDungeon[upY][upX] = 'D';

        // Ensure stairs up and down are reachable on next dungeon
        do {
            downX = rand.nextInt(WIDTH);
            downY = rand.nextInt(HEIGHT);
        } while (nextDungeon[downY][downX] != '.' || !isReachable(nextDungeon, downX, downY, downX, downY));
        nextDungeon[downY][downX] = 'U';
    }

    private boolean isReachable(char[][] dungeon, int startX, int startY, int goalX, int goalY) {
        boolean[][] visited = new boolean[HEIGHT][WIDTH];
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startX, startY});
        visited[startY][startX] = true;

        int[][] directions = {
                {0, 1}, {1, 0}, {0, -1}, {-1, 0}
        };

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int x = current[0];
            int y = current[1];

            if (x == goalX && y == goalY) {
                return true;
            }

            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];

                if (nx >= 0 && nx < WIDTH && ny >= 0 && ny < HEIGHT && !visited[ny][nx] && dungeon[ny][nx] != '#') {
                    queue.add(new int[]{nx, ny});
                    visited[ny][nx] = true;
                }
            }
        }
        return false;
    }

    private void movePlayer(int keyCode) {
        if (gameWon) return;

        int newX = playerX;
        int newY = playerY;

        switch (keyCode) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                newY--;
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                newY++;
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                newX--;
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                newX++;
                break;
        }

        if (newX >= 0 && newX < WIDTH && newY >= 0 && newY < HEIGHT) {
            char destination = dungeons[currentFloor][newY][newX];

            if (destination == '#') return;

            if (!onStairTile) {
                dungeons[currentFloor][playerY][playerX] = '.';
            }

            playerX = newX;
            playerY = newY;

            onStairTile = (destination == 'U' || destination == 'D');
            System.out.println("Nowa pozycja gracza: (" + playerX + ", " + playerY + ")");

            if (destination == 'G') {
                gameWon = true;
                int option = JOptionPane.showConfirmDialog(this, "You won! Do you want to play again?", "Congratulations", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    restartGame();
                }
            }

            if (!onStairTile) {
                dungeons[currentFloor][playerY][playerX] = 'P';
            }
        }
    }

    private void changeFloor() {
        char tile = dungeons[currentFloor][playerY][playerX];
        if (tile == 'U' && currentFloor > 0) {
            dungeons[currentFloor][playerY][playerX] = (dungeons[currentFloor][playerY][playerX] == 'U') ? 'U' : 'D';
            currentFloor--;
            moveToFloor(currentFloor);
            System.out.println("Zmieniono piętro na: " + currentFloor);
            System.out.println("Nowa pozycja gracza: (" + playerX + ", " + playerY + ")");
        } else if (tile == 'D' && currentFloor < FLOORS - 1) {
            dungeons[currentFloor][playerY][playerX] = (dungeons[currentFloor][playerY][playerX] == 'U') ? 'U' : 'D';
            currentFloor++;
            moveToFloor(currentFloor);
            System.out.println("Zmieniono piętro na: " + currentFloor);
            System.out.println("Nowa pozycja gracza: (" + playerX + ", " + playerY + ")");
        }

        // Aktualizuj flagę onStairTile na podstawie nowej pozycji gracza
        if (dungeons[currentFloor][playerY][playerX] == 'U' || dungeons[currentFloor][playerY][playerX] == 'D') {
            onStairTile = true;
        } else {
            onStairTile = false;
        }
    }

    private void moveToFloor(int floor) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (dungeons[floor][y][x] == 'P') {
                    playerX = x;
                    playerY = y;
                    return;
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        char[][] dungeon = dungeons[currentFloor];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                switch (dungeon[y][x]) {
                    case '#':
                        g.setColor(Color.DARK_GRAY);
                        break;
                    case '.':
                        g.setColor(Color.LIGHT_GRAY);
                        break;
                    case 'P':
                        g.setColor(Color.RED);
                        break;
                    case 'G':
                        g.setColor(Color.GREEN);
                        break;
                }
                g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }

        // Rysowanie pól przejścia niezależnie od obecnej lokalizacji gracza
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (dungeon[y][x] == 'U' || dungeon[y][x] == 'D') {
                    g.setColor((dungeon[y][x] == 'U') ? Color.CYAN : Color.MAGENTA);
                    g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }

        // Indicate current floor
        g.setColor(Color.WHITE);
        g.drawString("Floor: " + (currentFloor + 1), 10, 10);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Dungeon Crawler");
        DungeonCrawler game = new DungeonCrawler();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

