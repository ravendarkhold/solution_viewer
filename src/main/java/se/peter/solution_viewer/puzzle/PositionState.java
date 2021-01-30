package se.peter.solution_viewer.puzzle;


import se.peter.solution_viewer.util.Matrix;

import java.util.Arrays;
import java.util.List;

public class PositionState {
    private final List<int[][]> piecePositions;

    public PositionState(List<int[][]> piecePositions) {
        this.piecePositions = piecePositions;
    }


    public int[][] getPieceMatrix(int piece) {
        return piecePositions.get(piece - 1);
    }

    public List<int[][]> getPiecePositions() {
        return piecePositions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PositionState that = (PositionState) o;
        if (piecePositions.size() != that.piecePositions.size())
            return false;
        for (int i = 0; i < piecePositions.size(); i++) {
            if (!Arrays.deepEquals(piecePositions.get(i), that.piecePositions.get(i)))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;

        for (int i = 0; i < piecePositions.size(); ++i) {
            hashCode = 31 * hashCode + Arrays.deepHashCode(piecePositions.get(i));
        }

        return hashCode;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < piecePositions.size(); i++) {
            if (i >0)
                builder.append(",");
            if (piecePositions.get(i)[0][0] == 1 &&
                    piecePositions.get(i)[0][1] == 0 &&
                    piecePositions.get(i)[0][2] == 0 &&
                    piecePositions.get(i)[1][0] == 0 &&
                    piecePositions.get(i)[1][1] == 1 &&
                    piecePositions.get(i)[1][2] == 0 &&
                    piecePositions.get(i)[2][0] == 0 &&
                    piecePositions.get(i)[2][1] == 0 &&
                    piecePositions.get(i)[2][2] == 1 &&
                    piecePositions.get(i)[3][0] == 0 &&
                    piecePositions.get(i)[3][1] == 0 &&
                    piecePositions.get(i)[3][2] == 0 &&
                    piecePositions.get(i)[3][3] == 1) {
                builder.append((i + 1) + ":[" + piecePositions.get(i)[0][3] + "," + piecePositions.get(i)[1][3] + "," + piecePositions.get(i)[2][3] + "]");
            } else {
                builder.append((i + 1) + ":" + Matrix.toString(piecePositions.get(i)));
            }
        }
        return builder.toString();
    }
}
