package se.peter.solution_viewer.puzzle;

import se.peter.solution_viewer.util.Matrix;

public class PieceRotation {
    private final int pieces;
    private int[][] matrix;
    private String description;

    public PieceRotation(int pieces, int[][] rotationMatrix) {
        this.pieces = pieces;
        matrix = rotationMatrix;
        description = Matrix.toString(rotationMatrix);
    }

    public int getPieces() {
        return pieces;
    }

    public String toString() {

        return description;
    }

    public int[][] getMatrix() {
        return matrix;
    }
}
