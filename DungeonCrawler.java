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

    private int previousFloor = PLAYER_START_FLOOR;

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
        int goalFloor = rand.nextInt(FLOORS); // Randomly select a floor for the goal

        for (int f = 0; f < FLOORS; f++) {
            generateDungeon(dungeons[f], f == goalFloor);
        }

        // Add stairs between floors
        for (int f = 0; f < FLOORS - 1; f++) {
            addStairs(dungeons[f], dungeons[f + 1], f == FLOORS - 2);
        }

        // Add stairs down to the last floor
        addStairs(dungeons[FLOORS - 1], null, true);

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


    private List<int[]> findDeadEnds(char[][] dungeon) {
        List<int[]> deadEnds = new ArrayList<>();
        int[][] directions = { {0, 1}, {1, 0}, {0, -1}, {-1, 0} };

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (dungeon[y][x] == '.') {
                    int wallCount = 0;
                    for (int[] dir : directions) {
                        int nx = x + dir[0];
                        int ny = y + dir[1];
                        if (nx < 0 || nx >= WIDTH || ny < 0 || ny >= HEIGHT || dungeon[ny][nx] == '#') {
                            wallCount++;
                        }
                    }
                    if (wallCount == 3) {
                        deadEnds.add(new int[] { x, y });
                    }
                }
            }
        }

        return deadEnds;
    }

    private void generateDungeon(char[][] dungeon, boolean isGoalFloor) {
        for (int y = 0; y < HEIGHT; y++) {
            Arrays.fill(dungeon[y], '#');
        }

        Stack<int[]> stack = new Stack<>();
        Random rand = new Random();

        int startX = rand.nextInt(WIDTH);
        int startY = rand.nextInt(HEIGHT);
        stack.push(new int[]{startX, startY});
        dungeon[startY][startX] = '.';

        int[][] directions = { {0, 1}, {1, 0}, {0, -1}, {-1, 0} };

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
            do {
                goalX = rand.nextInt(WIDTH);
                goalY = rand.nextInt(HEIGHT);
            } while (dungeon[goalY][goalX] == '#' || (goalX == startX && goalY == startY));
            dungeon[goalY][goalX] = 'G';
        }
    }

    private void addStairs(char[][] currentDungeon, char[][] nextDungeon, boolean isLastFloor) {
        Random rand = new Random();
        List<int[]> currentDeadEnds = findDeadEnds(currentDungeon);

        if (nextDungeon == null) {
            // Last floor, only add down stairs
            int[] downStairs = currentDeadEnds.get(rand.nextInt(currentDeadEnds.size()));
            currentDungeon[downStairs[1]][downStairs[0]] = 'D';
        } else if (currentDungeon == dungeons[0]) {
            // First floor, only add up stairs
            int[] upStairs = currentDeadEnds.get(rand.nextInt(currentDeadEnds.size()));
            currentDungeon[upStairs[1]][upStairs[0]] = 'U';
        } else {
            // General case, add both up and down stairs
            int[] downStairs = currentDeadEnds.get(rand.nextInt(currentDeadEnds.size()));
            currentDungeon[downStairs[1]][downStairs[0]] = 'D';

            List<int[]> nextDeadEnds = findDeadEnds(nextDungeon);
            int[] upStairs = nextDeadEnds.get(rand.nextInt(nextDeadEnds.size()));
            nextDungeon[upStairs[1]][upStairs[0]] = 'U';
        }
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

            // Zmieniamy poprzednią pozycję na 'T' (traveled path) tylko jeśli to 'P' (player)
            if (dungeons[currentFloor][playerY][playerX] == 'P' && !onStairTile) {
                dungeons[currentFloor][playerY][playerX] = 'T';
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
                dungeons[currentFloor][playerY][playerX] = 'P'; // Update player's new position
            }
        }

        repaint(); // Ensure the component is repainted after every move
    }





    private void changeFloor() {
        char tile = dungeons[currentFloor][playerY][playerX];
        if (tile == 'U' && currentFloor < FLOORS - 1) {
            currentFloor++;
        } else if (tile == 'D' && currentFloor > 0) {
            currentFloor--;
        }

        // Zmieniamy poprzednią pozycję gracza na 'T' tylko, jeśli nie wchodzimy po raz pierwszy na nowe piętro
        if (previousFloor == currentFloor) {
            dungeons[previousFloor][playerY][playerX] = 'T';
        } else {
            previousFloor = currentFloor;
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
        boolean[][][] visited = new boolean[FLOORS][HEIGHT][WIDTH];
        int[][][][] parent = new int[FLOORS][HEIGHT][WIDTH][2]; // Corrected parent array definition
        int[][] directions = { {0, 1}, {1, 0}, {0, -1}, {-1, 0} };

        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[] {playerX, playerY, currentFloor});
        visited[currentFloor][playerY][playerX] = true;
        boolean goalFound = false;
        int goalFloor = -1, goalX = -1, goalY = -1;

        while (!queue.isEmpty() && !goalFound) {
            int[] current = queue.poll();
            int x = current[0], y = current[1], floor = current[2];

            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];

                if (nx >= 0 && nx < WIDTH && ny >= 0 && ny < HEIGHT && !visited[floor][ny][nx] && dungeons[floor][ny][nx] != '#') {
                    queue.add(new int[] {nx, ny, floor});
                    visited[floor][ny][nx] = true;
                    parent[floor][ny][nx][0] = x;
                    parent[floor][ny][nx][1] = y;

                    if (dungeons[floor][ny][nx] == 'G') {
                        goalFound = true;
                        goalFloor = floor;
                        goalX = nx;
                        goalY = ny;
                        break;
                    }
                }
            }

            if (!goalFound && dungeons[floor][y][x] == 'U' && floor < FLOORS - 1 && !visited[floor + 1][y][x]) {
                queue.add(new int[] {x, y, floor + 1});
                visited[floor + 1][y][x] = true;
                parent[floor + 1][y][x][0] = x;
                parent[floor + 1][y][x][1] = y;
            } else if (!goalFound && dungeons[floor][y][x] == 'D' && floor > 0 && !visited[floor - 1][y][x]) {
                queue.add(new int[] {x, y, floor - 1});
                visited[floor - 1][y][x] = true;
                parent[floor - 1][y][x][0] = x;
                parent[floor - 1][y][x][1] = y;
            }
        }

        if (goalFound) {
            int cx = goalX, cy = goalY, cf = goalFloor;

            while (cx != playerX || cy != playerY || cf != currentFloor) {
                path.add(new int[] {cx, cy, cf});
                int px = parent[cf][cy][cx][0];
                int py = parent[cf][cy][cx][1];
                if (px == cx && py == cy) {
                    if (cf > currentFloor) cf--; else cf++;
                } else {
                    cx = px;
                    cy = py;
                }
            }
            Collections.reverse((LinkedList<int[]>) path);
        }

        return path;
    }


    private void autoPlayGame() {
        if (autoPath == null || autoPath.isEmpty()) {
            return;
        }

        javax.swing.Timer timer = new javax.swing.Timer(20, e -> {
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

            // Update the previous position to 'T' before moving the player
            if (!onStairTile) {
                dungeons[currentFloor][playerY][playerX] = 'T';
            }

            if (nextFloor != currentFloor) {
                currentFloor = nextFloor;
                playerX = nextX;
                playerY = nextY;
                dungeons[currentFloor][playerY][playerX] = 'P';
                onStairTile = false;
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
                    case 'T':
                        g.setColor(Color.CYAN); // Traveled path in cyan
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
        g.drawString("Current Floor: " + (currentFloor + 1), legendX, 750); // Display the current floor at the top-left corner
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

        g.setColor(Color.CYAN);
        g.fillRect(legendX, 280, TILE_SIZE, TILE_SIZE);
        g.setColor(Color.WHITE);
        g.drawString("Traveled Path", legendX + TILE_SIZE + 10, 300);

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
