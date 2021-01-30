package se.peter.solution_viewer.util;

import se.peter.solution_viewer.puzzle.Position;

import java.util.HashMap;
import java.util.Map;

public class Rotations {
    public Rotations() {
    }

    private Position rotateX(Position p) {
        return new Position(p.getX(), -p.getZ(), p.getY());
    }

    private Position rotateZ(Position p) {
        return new Position(-p.getY(), p.getX(), p.getZ());
    }

    private Position rotateY(Position p) {
        return new Position(p.getZ(), p.getY(), -p.getX());
    }

    public Position[][][][] getRotatedPosition(int sizeX, int sizeY, int sizeZ) {
        Position[][][][] rotatedPosition = new Position[24][sizeX][sizeY][sizeZ];

        int targetSizeX = sizeX;
        int targetSizeY = sizeY;
        int targetSizeZ = sizeZ;
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    Position p = new Position(center(x, sizeX), center(y, sizeY), center(z, sizeZ));
                    for (int i = 0; i < 6; i++) {
                        rotatedPosition[i * 4][x][y][z] = new Position(back(p.getX(), targetSizeX), back(p.getY(), targetSizeY), back(p.getZ(), targetSizeZ));
                        for (int j = 0; j < 3; j++) {
                            p = rotateY(p);
                            int temp = targetSizeX;
                            targetSizeX = targetSizeZ;
                            targetSizeZ = temp;

                            if ((i & 1) == 0) {
                                p = rotateY(p);
                                p = rotateY(p);
                            }
                            rotatedPosition[i * 4 + j + 1][x][y][z] = new Position(back(p.getX(), targetSizeX), back(p.getY(), targetSizeY), back(p.getZ(), targetSizeZ));
                        }
                        p = rotateX(p);
                        int temp = targetSizeY;
                        targetSizeY = targetSizeZ;
                        targetSizeZ = temp;
                    }
                }
            }
        }
        Map<String, Integer> s = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            StringBuilder b = new StringBuilder();
            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < sizeY; y++) {
                    for (int z = 0; z < sizeZ; z++) {
                        b.append(rotatedPosition[i][x][y][z]);
                    }
                }
            }
            if (s.containsKey(b.toString())) {
                System.out.println("Duplicate rotations(" + sizeX + "," + sizeY + "," + sizeZ + "): " + i + " and " + s.get(b.toString()) + ": " + b);
            }
            s.put(b.toString(), i);
        }
        return rotatedPosition;
    }

    private int center(int coord, int size) {
        return 2 * coord - (size - 1);
    }

    private int back(int coord, int size) {
        return (coord + (size - 1)) / 2;
    }
}
