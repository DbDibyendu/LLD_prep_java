import java.util.*;

/*
 =====================================================
 FUNCTIONAL REQUIREMENTS
 =====================================================
 - Initialize a chess game with two players
 - Enforce turn-based moves (white starts)
 - Validate moves:
     * Move must be inside the board
     * Piece must belong to current player
     * Cannot capture own piece
     * Piece-specific movement rules must be respected
 - Move a piece from one position to another
 - Switch turns after a valid move

 NON-FUNCTIONAL REQUIREMENTS
 =====================================================
 - Extensible design (easy to add new pieces)
 - Clear separation of concerns
 - Maintainable and testable code
 - Object-Oriented Design principles

 CORE ENTITIES
 =====================================================
 - Color
 - Position
 - Piece (abstract)
 - Concrete Pieces (Rook)
 - Player
 - Board
 - Game

 DESIGN PRINCIPLES & PATTERNS USED
 =====================================================
 - SRP (Single Responsibility Principle)
 - OCP (Open/Closed Principle)
 - DIP (Dependency Inversion Principle)
 - Encapsulation
 - Strategy Pattern (piece movement)
 - Polymorphism
 - Facade Pattern (Game orchestrates interaction)
 =====================================================
*/


// =========================
// Color Enum (Low-level)
// =========================
// - Type safety
// - Avoids magic strings
public enum Color {
    WHITE,
    BLACK
}


// =========================
// Position (Value Object)
// =========================
// - Immutable
// - Encapsulates coordinates
// - SRP: Represents board coordinates only
public class Position {
    public final int row;
    public final int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }
}


// =========================
// Piece (Abstraction)
// =========================
// - OCP: Add new pieces without modifying Game
// - Strategy Pattern: movement varies per piece
// - DIP: Game depends on Piece abstraction
public abstract class Piece {

    private final Color color;

    protected Piece(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public abstract boolean isValidMove(
            Position from,
            Position to,
            Board board
    );
}


// =========================
// Rook (Concrete Strategy)
// =========================
// - Implements rook movement logic
// - Polymorphism in action
public class Rook extends Piece {

    public Rook(Color color) {
        super(color);
    }

    @Override
    public boolean isValidMove(Position from, Position to, Board board) {
        return from.row == to.row || from.col == to.col;
    }
}


// =========================
// Player
// =========================
// - SRP: Holds player-related information only
// - Encapsulation: fields are private & immutable
public class Player {

    private final String playerId;
    private final String name;
    private final Color color;

    public Player(String playerId, String name, Color color) {
        this.playerId = playerId;
        this.name = name;
        this.color = color;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }
}


// =========================
// Board
// =========================
// - SRP: Maintains board state only
// - Encapsulation: grid hidden from clients
public class Board {

    private final Piece[][] grid = new Piece[8][8];

    public Board() {
        initialize();
    }

    private void initialize() {
        // Minimal setup for demonstration
        grid[0][0] = new Rook(Color.WHITE);
        grid[7][0] = new Rook(Color.BLACK);
    }

    public boolean isInsideBoard(Position pos) {
        return pos.row >= 0 && pos.row < 8 &&
                pos.col >= 0 && pos.col < 8;
    }

    public Piece getPiece(Position pos) {
        return grid[pos.row][pos.col];
    }

    public void movePiece(Position from, Position to) {
        grid[to.row][to.col] = grid[from.row][from.col];
        grid[from.row][from.col] = null;
    }
}


// =========================
// Game (Orchestrator / Facade)
// =========================
// - SRP: Handles game flow & turn switching only
// - Facade Pattern: hides Board + Piece complexity
public class Game {

    private final Board board;
    private final Player whitePlayer;
    private final Player blackPlayer;
    private Player currentPlayer;

    public Game(Player whitePlayer, Player blackPlayer) {
        this.board = new Board();
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.currentPlayer = whitePlayer; // White starts
    }

    public boolean makeMove(Position from, Position to) {

        // Boundary validation
        if (!board.isInsideBoard(from) || !board.isInsideBoard(to)) {
            return false;
        }

        Piece piece = board.getPiece(from);
        if (piece == null ||
                piece.getColor() != currentPlayer.getColor()) {
            return false;
        }

        Piece target = board.getPiece(to);
        if (target != null &&
                target.getColor() == currentPlayer.getColor()) {
            return false;
        }

        // Strategy Pattern: piece owns its move logic
        if (!piece.isValidMove(from, to, board)) {
            return false;
        }

        board.movePiece(from, to);
        switchTurn();
        return true;
    }

    private void switchTurn() {
        currentPlayer =
                (currentPlayer == whitePlayer)
                        ? blackPlayer
                        : whitePlayer;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }
}


// =========================
// Main (Driver)
// =========================
// - Demonstrates game execution
public class Main {

    public static void main(String[] args) {

        Player white = new Player("P1", "Alice", Color.WHITE);
        Player black = new Player("P2", "Bob", Color.BLACK);

        Game game = new Game(white, black);

        Position from = new Position(0, 0);
        Position to = new Position(0, 5);

        boolean moved = game.makeMove(from, to);

        System.out.println("Move success: " + moved);
        System.out.println(
                "Current player: " +
                        game.getCurrentPlayer().getName() +
                        " (" + game.getCurrentPlayer().getColor() + ")"
        );
    }
}
