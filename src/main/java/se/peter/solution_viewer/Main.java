package se.peter.solution_viewer;

import com.jme3.app.SimpleApplication;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import se.peter.solution_viewer.importer.Importer;
import se.peter.solution_viewer.puzzle.Assembly;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.lang.Math.floor;

public class Main extends SimpleApplication {

    private static final String PAUSE_MAPPING_NAME = "asdfas";
    public static final float SPEED = 2.5f;
    private List<List<Transform>> moves;
    private float time = 0;
    private Assembly assembly;
    private List<Node> pieceNodes;
    private boolean running = false;

    public static void main(String[] args) {

        Main app = new Main();
        app.setShowSettings(false);

        AppSettings settings = new AppSettings(true);
        settings.put("Width", 2000);
        settings.put("Height", 1500);
        settings.put("VSync", true);


        settings.put("Samples", 4);
        settings.setTitle("Solution viewer");
        app.setSettings(settings);

        app.start();

    }

    @Override
    public void simpleInitApp() {
        Importer importer = new Importer();
        //File file = new File("Y:\\Peter\\puzzles\\Own\\There and back again (Level 12 and 21 moves)\\test\\test");
        //File file = new File("Y:\\Peter\\puzzles\\Others\\Alfons Eyckmans\\Cuckold.xmpuzzle");
        //File file = new File("Y:\\Peter\\puzzles\\Others\\Juno\\Keep_I_on_the_Burr_SolutionFile.xmpuzzle");
        File file = new File("Y:\\Peter\\puzzles\\Others\\Andrew Crowell\\X_TIC_SolutionFile.xmpuzzle");
        // File file = new File("Y:\\Peter\\puzzles\\Others\\Stephen Baumeggar\\excaliburr.xmpuzzle");

        List<Assembly> assemblies = importer.loadAssemblies(file);

        assembly = assemblies.get(assemblies.size() - 1);
        System.out.println("Assembly " + assembly.getAssemblyNumber() + ", solution " + assembly.getSolutionNumber());

        int pieceCount = assembly.getVoxelsByPiece().size();
        moves = assembly.getMoves();

//        moves.forEach(m -> {
//            System.out.println("--------");
//            for (int i = 0; i < assembly.getVoxelsByPiece().size(); i++)
//                System.out.println(m.get(i).getTranslation());
//        });

        pieceNodes = new ArrayList<>();
        ColorRGBA[] colors = IntStream.range(0, pieceCount).mapToObj(i -> ColorRGBA.randomColor()).toArray(ColorRGBA[]::new);

        for (int pieceNumber = 1; pieceNumber <= pieceCount; pieceNumber++) {
            int[][][] piece = assembly.getVoxelsByPiece().get(pieceNumber - 1);
            Node pieceNode = new Node();
            pieceNodes.add(pieceNode);
            Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");

            mat.setTexture("DiffuseMap", assetManager.loadTexture("Pond.jpg"));
            mat.setTexture("NormalMap", assetManager.loadTexture("Pond_normal.png"));
            mat.setBoolean("UseMaterialColors", true);
            mat.setColor("Diffuse", colors[pieceNumber - 1]);
            mat.setColor("Specular", ColorRGBA.White);
            mat.setFloat("Shininess", 0f);  // [0,128]

            //  mat.getAdditionalRenderState().setWireframe(true);
            for (int x = 0; x < piece.length; x++)
                for (int y = 0; y < piece[0].length; y++)
                    for (int z = 0; z < piece[0][0].length; z++) {
                        if (piece[x][y][z] == 1) {
                            Box box = new Box(0.5f, 0.5f, 0.5f);
                            Geometry geom = new Geometry("Box", box);
                            geom.setLocalTranslation(x, y, z);
                            geom.setMaterial(mat);
                            pieceNode.attachChild(geom);
                        }
                    }
            rootNode.attachChild(pieceNode);

            pieceNode.setLocalTransform(assembly.getPositionState().get(pieceNumber - 1));
        }
        viewPort.setBackgroundColor(ColorRGBA.White);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(1, 1, 1).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);

        DirectionalLight sun2 = new DirectionalLight();
        sun2.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        sun2.setColor(ColorRGBA.White);
        rootNode.addLight(sun2);

        inputManager.addMapping(PAUSE_MAPPING_NAME, new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(new ActionListener() {

            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (!isPressed) {
                    running = !running;
                }
            }
        }, PAUSE_MAPPING_NAME);

        flyCam.setEnabled(false);
        ChaseCamera chaseCam = new ChaseCamera(cam, rootNode, inputManager);
        chaseCam.setDefaultDistance(50f);
        chaseCam.setDragToRotate(true);

        attachCoordinateAxes(rootNode);
    }

    int lastMoveIndex = 0;

    private void movePieces() {
        int moveIndex = (int) floor(time * SPEED);
        if (moveIndex < moves.size()) {
            float fraction = (time * SPEED - moveIndex);
            for (int piece = 0; piece < assembly.getVoxelsByPiece().size(); piece++) {
                Transform t0;
                if (moveIndex > 0) {
                    t0 = moves.get(moveIndex - 1).get(piece);
                } else {
                    t0 = assembly.getPositionState().get(piece);
                }
                Transform t1 = moves.get(moveIndex).get(piece);

                Node node = pieceNodes.get(piece);

                Quaternion q = new Quaternion();
                q.slerp(t0.getRotation(), t1.getRotation(), fraction);

                Vector3f t = new Vector3f();
                t.interpolateLocal(t0.getTranslation(), t1.getTranslation(), fraction);
                node.setLocalRotation(q);
                node.setLocalTranslation(t);

//                if (piece == 1) {
//                    System.out.println("Piece " + (piece + 1));
//                    System.out.println(t0.getTranslation());
//                    System.out.println(t0.getRotation());
//                    System.out.println(t1.getTranslation());
//                    System.out.println(t1.getRotation());
//                    System.out.println(t);
//                    System.out.println(q);
//
//                }
            }
            lastMoveIndex = moveIndex;
        } else {
            time = 0;
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (running) {
            time += tpf;
            movePieces();
        }
    }

    private void putShape(Node n, Mesh shape, ColorRGBA color) {
        Geometry g = new Geometry("coordinate axis", shape);
        Material mat = new Material(getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        RenderState additionalRenderState = mat.getAdditionalRenderState();
        additionalRenderState.setWireframe(true);
        additionalRenderState.setLineWidth(4);
        mat.setColor("Color", color);
        g.setMaterial(mat);
        n.attachChild(g);
    }

    private void attachCoordinateAxes(Node n) {
        Arrow arrow = new Arrow(Vector3f.UNIT_X);
        putShape(n, arrow, ColorRGBA.Red);

        arrow = new Arrow(Vector3f.UNIT_Y);
        putShape(n, arrow, ColorRGBA.Green);

        arrow = new Arrow(Vector3f.UNIT_Z);
        putShape(n, arrow, ColorRGBA.Blue);
    }
}