package se.peter.solution_viewer.puzzle;

import com.jme3.math.Transform;

import java.util.Iterator;
import java.util.List;

public class Assembly {
    private final int assemblyNumber;
    private final int solutionNumber;
    private final List<int[][][]> voxelsByPiece;
    private final List<List<Transform>> piecePositionsByMove;

    public Assembly(int assemblyNumber, int solutionNumber, List<int[][][]> voxelsByPiece, List<List<Transform>> piecePositionsByMove) {
        this.assemblyNumber = assemblyNumber;
        this.solutionNumber = solutionNumber;
        this.voxelsByPiece = voxelsByPiece;
        this.piecePositionsByMove = piecePositionsByMove;

        List<Transform> previous = null;
        for (Iterator<List<Transform>> it = this.piecePositionsByMove.iterator(); it.hasNext(); ) {
            List<Transform> transforms = it.next();
            if (previous != null && previous.equals(transforms)) {
                it.remove();
                System.err.println("Removed " + transforms);
            }
            previous = transforms;
        }
    }

    public List<int[][][]> getVoxelsByPiece() {
        return voxelsByPiece;
    }

//    public List<Transform> getPositionState() {
//        return positionState;
//    }

    public int getAssemblyNumber() {
        return assemblyNumber;
    }

    public int getSolutionNumber() {
        return solutionNumber;
    }

    public List<List<Transform>> getPiecePositionsByMove() {
        return piecePositionsByMove;
    }
}
