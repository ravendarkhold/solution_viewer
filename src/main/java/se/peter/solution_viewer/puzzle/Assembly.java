package se.peter.solution_viewer.puzzle;

import com.jme3.math.Transform;

import java.util.List;

public class Assembly {
    private final List<int[][][]> voxelsByPiece;
    private final List<Transform> positionState;

    public Assembly(List<int[][][]> voxelsByPiece, List<Transform> positionState) {
        this.voxelsByPiece = voxelsByPiece;
        this.positionState = positionState;
    }

    public List<int[][][]> getVoxelsByPiece() {
        return voxelsByPiece;
    }

    public List<Transform> getPositionState() {
        return positionState;
    }
}
