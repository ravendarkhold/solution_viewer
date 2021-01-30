package se.peter.solution_viewer.puzzle;

import java.util.List;

public class Assembly {
    private final List<int[][][]> voxelsByPiece;
    private final PositionState positionState;

    public Assembly(List<int[][][]> voxelsByPiece, PositionState positionState) {
        this.voxelsByPiece = voxelsByPiece;
        this.positionState = positionState;
    }

    public List<int[][][]> getVoxelsByPiece() {
        return voxelsByPiece;
    }

    public PositionState getPositionState() {
        return positionState;
    }
}
