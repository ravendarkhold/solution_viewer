package se.peter.solution_viewer.puzzle;

import com.jme3.math.Transform;

import java.util.List;

public class Assembly {
    private final int assemblyNumber;
    private final int solutionNumber;
    private final List<int[][][]> voxelsByPiece;
    private final List<Transform> positionState;
    private final List<List<Transform>> moves;

    public Assembly(int assemblyNumber, int solutionNumber, List<int[][][]> voxelsByPiece, List<Transform> positionState, List<List<Transform>> moves) {
        this.assemblyNumber = assemblyNumber;
        this.solutionNumber = solutionNumber;
        this.voxelsByPiece = voxelsByPiece;
        this.positionState = positionState;
        this.moves = moves;
    }

    public List<int[][][]> getVoxelsByPiece() {
        return voxelsByPiece;
    }

    public List<Transform> getPositionState() {
        return positionState;
    }

    public int getAssemblyNumber() {
        return assemblyNumber;
    }

    public int getSolutionNumber() {
        return solutionNumber;
    }

    public List<List<Transform>> getMoves() {
        return moves;
    }
}
