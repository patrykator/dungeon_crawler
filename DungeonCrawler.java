import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

public class DungeonCrawler extends JPanel {

    private static final int TILE_SIZE = 32;
    private static final int WIDTH = 42; // 16:9 ratio for larger size
    private static final int HEIGHT = 24; // 16:9 ratio for larger size
    private static final int FLOORS = 5; // Zmiana na 5 pięter
    private static final int PLAYER_START_FLOOR = 2; // Gracz zaczyna na 3. piętrze
    private static final int LEGEND_WIDTH = 200;

    private char[][][] dungeons;
    private int currentFloor;
    private int playerX, playerY;
    private int goalX, goalY;
    private boolean gameWon;
    private boolean onStairTile;
    private boolean autoPlay;
    private Queue<int[]> autoPath;

    public DungeonCrawler() {
        setPreferredSize(new Dimension((WIDTH * TILE_SIZE) + LEGEND_WIDTH, HEIGHT * TILE_SIZE));
        setBackground(Color.BLACK);
        generateDungeons();
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!autoPlay) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                        if (onStairTile) {
                            changeFloor();
                        }
                    } else {
                        movePlayer(e.getKeyCode());
                    }
                    repaint();
                }
            }
        });

        // Zapytaj gracza czy chce grać ręcznie, czy automatycznie
        int option = JOptionPane.showOptionDialog(this,
                "Wybierz tryb gry",
                "Dungeon Crawler",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"Manual", "Auto"},
                "Manual");

        autoPlay = (option == 1);

        if (autoPlay) {
            autoPath = findShortestPathToGoal();
            autoPlayGame();
        }
    }

    private void restartGame() {
        generateDungeons();
        repaint();
        if (autoPlay) {
            autoPath = findShortestPathToGoal();
            autoPlayGame();
        }
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
            List<int[]> neighbors = new ArrayList<>();

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
                showVictoryDialog();
            }

            if (!onStairTile) {
                dungeons[currentFloor][playerY][playerX] = 'P';
            }
        }
    }

    private void changeFloor() {
        char tile = dungeons[currentFloor][playerY][playerX];
        if (tile == 'U' && currentFloor < FLOORS - 1) {
            currentFloor++;
        } else if (tile == 'D' && currentFloor > 0) {
            currentFloor--;
        }

        do {
            playerX = new Random().nextInt(WIDTH);
            playerY = new Random().nextInt(HEIGHT);
        } while (dungeons[currentFloor][playerY][playerX] == '#');
        onStairTile = false;
        dungeons[currentFloor][playerY][playerX] = 'P';
        System.out.println("Nowe piętro: " + currentFloor);
        repaint();

        if (autoPlay) {
            autoPath = findShortestPathToGoal();
        }
    }

    private Queue<int[]> findShortestPathToGoal() {
        Queue<int[]> path = new LinkedList<>();
        boolean[][] visited = new boolean[HEIGHT][WIDTH];
        int[][][] parent = new int[HEIGHT][WIDTH][2];

        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{playerX, playerY});
        visited[playerY][playerX] = true;
        boolean goalFound = false;
        boolean stairFound = false;

        int[][] directions = {
                {0, 1}, {1, 0}, {0, -1}, {-1, 0}
        };

        int stairX = -1;
        int stairY = -1;

        while (!queue.isEmpty() && !goalFound) {
            int[] current = queue.poll();
            int x = current[0];
            int y = current[1];

            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];

                if (nx >= 0 && nx < WIDTH && ny >= 0 && ny < HEIGHT && !visited[ny][nx] && dungeons[currentFloor][ny][nx] != '#') {
                    queue.add(new int[]{nx, ny});
                    visited[ny][nx] = true;
                    parent[ny][nx][0] = x;
                    parent[ny][nx][1] = y;

                    if (dungeons[currentFloor][ny][nx] == 'G') {
                        goalFound = true;
                        goalX = nx;
                        goalY = ny;
                        break;
                    } else if (!stairFound && dungeons[currentFloor][ny][nx] == 'U') {
                        stairFound = true;
                        stairX = nx;
                        stairY = ny;
                    }
                }
            }
        }

        int cx, cy;

        if (goalFound) {
            cx = goalX;
            cy = goalY;
        } else if (stairFound) {
            cx = stairX;
            cy = stairY;
        } else {
            return path; // Empty path if no goal or stairs found
        }

        while (cx != playerX || cy != playerY) {
            path.add(new int[]{cx, cy, currentFloor});
            int px = parent[cy][cx][0];
            int py = parent[cy][cx][1];
            cx = px;
            cy = py;
        }
        Collections.reverse((LinkedList<int[]>) path); // Reverse the path to start from the player
        return path;
    }

    private void autoPlayGame() {
        if (autoPath == null || autoPath.isEmpty()) {
            return;
        }

        javax.swing.Timer timer = new javax.swing.Timer(100, e -> {
            if (autoPath.isEmpty()) {
                ((javax.swing.Timer) e.getSource()).stop();
                if (gameWon) {
                    showVictoryDialog();
                } else {
                    showGameOverDialog();
                }
                return;
            }

            int[] nextStep = autoPath.poll();
            if (nextStep.length != 3) {
                return;
            }

            int nextX = nextStep[0];
            int nextY = nextStep[1];
            int nextFloor = nextStep[2];

            if (nextFloor != currentFloor) {
                changeFloor();
                autoPath = findShortestPathToGoal(); // Recalculate path after changing floor
                return;
            }

            playerX = nextX;
            playerY = nextY;
            char currentTile = dungeons[currentFloor][playerY][playerX];
            onStairTile = (currentTile == 'U' || currentTile == 'D');

            if (onStairTile && !gameWon) {
                changeFloor(); // Simulate pressing space
                autoPath = findShortestPathToGoal(); // Recalculate path after changing floor
                return;
            }

            if (!onStairTile) {
                dungeons[currentFloor][playerY][playerX] = 'P';
            }

            repaint();
            if (gameWon || autoPath.isEmpty()) {
                ((javax.swing.Timer) e.getSource()).stop();
                if (gameWon) {
                    showVictoryDialog();
                } else {
                    showGameOverDialog();
                }
            }
        });
        timer.start();
    }

    private void showGameOverDialog() {
        int option = JOptionPane.showOptionDialog(this,
                "You have reached the end of the auto path. Do you want to play again?",
                "Game Over",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new Object[]{"Yes", "No"},
                "Yes");

        if (option == JOptionPane.YES_OPTION) {
            restartGame();
        } else {
            System.exit(0);
        }
    }


    private void showVictoryDialog() {
        int option = JOptionPane.showOptionDialog(this,
                "Congratulations! You've reached the goal. Do you want to play again?",
                "Victory!",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new Object[]{"Yes", "No"},
                "Yes");

        if (option == JOptionPane.YES_OPTION) {
            restartGame();
        } else {
            System.exit(0);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw dungeon tiles
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                switch (dungeons[currentFloor][y][x]) {
                    case '#':
                        g.setColor(Color.DARK_GRAY);
                        break;
                    case '.':
                        g.setColor(Color.LIGHT_GRAY);
                        break;
                    case 'P':
                        g.setColor(Color.BLUE);
                        break;
                    case 'G':
                        g.setColor(Color.GREEN);
                        break;
                    case 'U':
                        g.setColor(Color.ORANGE); // Stairs up in orange
                        break;
                    case 'D':
                        g.setColor(Color.PINK); // Stairs down in pink
                        break;
                }
                g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }

        // Draw floor counter
        drawFloorCounter(g);

        // Draw legend
        drawLegend(g);
    }

    private void drawFloorCounter(Graphics g) {
        int legendX = WIDTH * TILE_SIZE + 10;
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 25));
        g.drawString("Current Floor: " + (currentFloor + 1), legendX, 300); // Display the current floor at the top-left corner
    }

    private void drawLegend(Graphics g) {
        int legendX = WIDTH * TILE_SIZE + 10;

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Legend", legendX, 20);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(legendX, 40, TILE_SIZE, TILE_SIZE);
        g.setColor(Color.WHITE);
        g.drawString("Wall", legendX + TILE_SIZE + 10, 60);

        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(legendX, 80, TILE_SIZE, TILE_SIZE);
        g.setColor(Color.BLACK);
        g.drawString("Path", legendX + TILE_SIZE + 10, 100);

        g.setColor(Color.BLUE);
        g.fillRect(legendX, 120, TILE_SIZE, TILE_SIZE);
        g.setColor(Color.WHITE);
        g.drawString("Player", legendX + TILE_SIZE + 10, 140);

        g.setColor(Color.GREEN);
        g.fillRect(legendX, 160, TILE_SIZE, TILE_SIZE);
        g.setColor(Color.WHITE);
        g.drawString("Goal", legendX + TILE_SIZE + 10, 180);

        g.setColor(Color.ORANGE);
        g.fillRect(legendX, 200, TILE_SIZE, TILE_SIZE);
        g.setColor(Color.WHITE);
        g.drawString("Stairs Up", legendX + TILE_SIZE + 10, 220);

        g.setColor(Color.PINK);
        g.fillRect(legendX, 240, TILE_SIZE, TILE_SIZE);
        g.setColor(Color.WHITE);
        g.drawString("Stairs Down", legendX + TILE_SIZE + 10, 260);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Dungeon Crawler");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            DungeonCrawler game = new DungeonCrawler();
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
