package se.peter.solution_viewer.util;

public class Matrix {
    public static void printMatrix(int[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                System.out.print(String.format("%3d", matrix[i][j]));
            }
            System.out.println();
        }
    }

    public static int[] mult(int[][] matrix, int[] vector) {
        int[] result = new int[4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                result[i] += matrix[i][j] * vector[j];
            }
        }
        return result;
    }

    public static int[][] rotateXPositive() {
        return new int[][]{{1, 0, 0, 0}, {0, 0, 1, 0}, {0, -1, 0, 0}, {0, 0, 0, 1}};
    }

    public static int[][] rotateXNegative() {
        return new int[][]{{1, 0, 0, 0}, {0, 0, -1, 0}, {0, 1, 0, 0}, {0, 0, 0, 1}};
    }

    public static int[][] rotateYPositive() {
        return new int[][]{{0, 0, -1, 0}, {0, 1, 0, 0}, {1, 0, 0, 0}, {0, 0, 0, 1}};
    }

    public static int[][] rotateYNegative() {
        return new int[][]{{0, 0, 1, 0}, {0, 1, 0, 0}, {-1, 0, 0, 0}, {0, 0, 0, 1}};
    }

    public static int[][] rotateZPositive() {
        return new int[][]{{0, 1, 0, 0}, {-1, 0, 0, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}};
    }

    public static int[][] rotateZNegative() {
        return new int[][]{{0, -1, 0, 0}, {1, 0, 0, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}};
    }

    public static int[][] translate(int dx, int dy, int dz) {
        return new int[][]{{1, 0, 0, dx}, {0, 1, 0, dy}, {0, 0, 1, dz}, {0, 0, 0, 1}};
    }

    public static int[][] scale2(int dx, int dy, int dz) {
        return new int[][]{{2, 0, 0, 0}, {0, 2, 0, 0}, {0, 0, 2, 0}, {0, 0, 0, 1}};
    }

    public static int[][] mult(int[][] m1, int[][] m2) {
        int[][] result = new int[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    result[i][j] += m1[i][k] * m2[k][j];
                }
            }
        }
        return result;
    }

    public static String toString(int[][] matrix) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < matrix.length; i++) {
            if (buffer.length() > 0)
                buffer.append(',');
            buffer.append('[');
            for (int j = 0; j < matrix[0].length; j++) {
                buffer.append(String.format("%3d", matrix[i][j]));
            }
            buffer.append(']');
        }
        return buffer.toString();
    }

    public static String toString(long[][] matrix) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (matrix[i][j] != 0)
                    builder.append("[" + i + "][" + j + "]=" + matrix[i][j] + ";");
            }
        }
        return builder.toString();
    }

    public static int[][] createIdentityMatrix() {
        int[][] piecePosition = new int[4][4];
        for (int i = 0; i < 4; i++) {
            piecePosition[i][i] = 1;
        }
        return piecePosition;
    }
}
