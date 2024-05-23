import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class DungeonCrawler extends JPanel {

    private static final int TILE_SIZE = 32;
    private static final int WIDTH = 42; // 16:9 ratio for larger size
    private static final int HEIGHT = 24; // 16:9 ratio for larger size
    private char[][] dungeon;
    private int playerX, playerY;
    private int goalX, goalY;
    private boolean gameWon;

    public DungeonCrawler() {
        setPreferredSize(new Dimension(WIDTH * TILE_SIZE, HEIGHT * TILE_SIZE));
        setBackground(Color.BLACK);
        generateDungeon();
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                movePlayer(e.getKeyCode());
                repaint();
            }
        });
    }

    private void restartGame() {
        generateDungeon();
        repaint();
    }

    private void generateDungeon() {
        dungeon = new char[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++) {
            Arrays.fill(dungeon[y], '#');
        }

        // Use DFS to generate a maze
        Stack<int[]> stack = new Stack<>();
        Random rand = new Random();

        // Start position for DFS
        playerX = rand.nextInt(WIDTH);
        playerY = rand.nextInt(HEIGHT);
        stack.push(new int[]{playerX, playerY});
        dungeon[playerY][playerX] = '.';

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

        // Ensure goal position is reachable and not the same as the player's starting position
        do {
            goalX = rand.nextInt(WIDTH);
            goalY = rand.nextInt(HEIGHT);
        } while (dungeon[goalY][goalX] == '#' || (goalX == playerX && goalY == playerY));
        dungeon[goalY][goalX] = 'G';

        dungeon[playerY][playerX] = 'P';
        gameWon = false;
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

        if (newX >= 0 && newX < WIDTH && newY >= 0 && newY < HEIGHT && dungeon[newY][newX] != '#') {
            dungeon[playerY][playerX] = '.';
            playerX = newX;
            playerY = newY;

            if (playerX == goalX && playerY == goalY) {
                gameWon = true;
                int option = JOptionPane.showConfirmDialog(this, "You won! Do you want to play again?", "Congratulations", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    restartGame();
                }
            } else {
                dungeon[playerY][playerX] = 'P';
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (dungeon[y][x] == '#') {
                    g.setColor(Color.DARK_GRAY);
                } else if (dungeon[y][x] == '.') {
                    g.setColor(Color.LIGHT_GRAY);
                } else if (dungeon[y][x] == 'P') {
                    g.setColor(Color.RED);
                } else if (dungeon[y][x] == 'G') {
                    g.setColor(Color.GREEN);
                }
                g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
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
