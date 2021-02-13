package se.peter.solution_viewer;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static java.lang.Math.floor;
import static java.lang.Math.round;

public class Main extends SimpleApplication {
    public float moveSpeed = 1f;
    private final File file;
    private List<List<Transform>> moves;
    private Assembly assembly;
    private List<Node> pieceNodes;
    private BitmapText moveText;
    private State state = State.PAUSED;
    private float time = 0;
    private int direction = 1;
    private int currentMoveIndex;
    private int stepTo;

    public Main(File file) {
        this.file = file;
    }

    public static void main(String[] args) {
        Logger.getLogger("").setLevel(Level.OFF);

        if (args.length != 1) {
            System.err.println("Usage: java -jar target\\solution-viewer-1.0-SNAPSHOT-jar-with-dependencies.jar <xmpuzzle file>");
            return;
        }
        File file = new File(args[0]);
        if (!file.exists() || !file.canRead()) {
            System.err.println("Cannot read file " + file.getAbsolutePath());
            return;
        }

        // file = new File("Y:\\Peter\\puzzles\\Own\\There and back again (Level 12 and 21 moves)\\test\\test");
        // file = new File("Y:\\Peter\\puzzles\\Others\\Alfons Eyckmans\\Cuckold.xmpuzzle");
        // file = new File("Y:\\Peter\\puzzles\\Others\\Juno\\Keep_I_on_the_Burr_SolutionFile.xmpuzzle");
        // file = new File("Y:\\Peter\\puzzles\\Others\\Andrew Crowell\\X_TIC_SolutionFile.xmpuzzle");
        // file = new File("Y:\\Peter\\puzzles\\Others\\Stephen Baumeggar\\excaliburr.xmpuzzle");

        Main app = new Main(file);
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

        List<Assembly> assemblies = importer.loadAssemblies(file);

        assembly = assemblies.get(assemblies.size() - 1);
        System.out.println("Assembly " + assembly.getAssemblyNumber() + ", solution " + assembly.getSolutionNumber());

        int pieceCount = assembly.getVoxelsByPiece().size();
        moves = assembly.getPiecePositionsByMove();

//        moves.forEach(m -> {
//            System.out.println("--------");
//            for (int i = 0; i < assembly.getVoxelsByPiece().size(); i++) {
//                Transform transform = m.get(i);
//                System.out.println("Piece " + i + " " + transform.getTranslation() + " " + transform.getRotation());
//            }
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

            pieceNode.setLocalTransform(assembly.getPiecePositionsByMove().get(0).get(pieceNumber - 1));
        }
        viewPort.setBackgroundColor(ColorRGBA.White);

        addLights();

        setupControls();

        setupCamera();

        attachCoordinateAxes(rootNode);

        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        moveText = new BitmapText(guiFont, false);
        moveText.setSize(guiFont.getCharSet().getRenderedSize());
        showMoveIndex();
        moveText.setLocalTranslation(500, moveText.getLineHeight(), 0);
        moveText.setColor(ColorRGBA.Black);
        guiNode.attachChild(moveText);

        rootNode.attachChild(guiNode);
    }

    private void addLights() {
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(1, 1, 1).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);

        DirectionalLight sun2 = new DirectionalLight();
        sun2.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        sun2.setColor(ColorRGBA.White);
        rootNode.addLight(sun2);
    }

    private void setupCamera() {
        Node n = new Node();
        n.setLocalTranslation(5, 5, 5); // Should be middle of puzzle
        rootNode.attachChild(n);
        flyCam.setEnabled(false);
        ChaseCamera chaseCam = new ChaseCamera(cam, n, inputManager);
        chaseCam.setDefaultDistance(50f);
        chaseCam.setMaxDistance(1000f);
        chaseCam.setDragToRotate(true);
        chaseCam.setMinVerticalRotation((float) (-0.5 * Math.PI));
    }

    private void setupControls() {
        addKeyAction(KeyInput.KEY_SPACE, (name, isPressed, tpf) -> {
            if (!isPressed) {
                state = (state == State.RUNNING) ? State.PAUSED : State.RUNNING;
            }
        });

        addKeyAction(KeyInput.KEY_RIGHT, (name, isPressed, tpf) -> {
            if (!isPressed) {
                if (state == State.STEPPING && direction > 0) {
                    stepTo++;
                } else {
                    state = State.STEPPING;
                    direction = 1;
                    stepTo = currentMoveIndex + 1;
                }
            }
        });
        addKeyAction(KeyInput.KEY_LEFT, (name, isPressed, tpf) -> {
            if (!isPressed) {
                if (state == State.STEPPING && direction < 0) {
                    stepTo--;
                } else {
                    state = State.STEPPING;
                    direction = -1;
                    stepTo = currentMoveIndex - 1;
                }
            }
        });

        addKeyAction(KeyInput.KEY_UP, (name, isPressed, tpf) -> {
            if (!isPressed) {
                moveSpeed *= 1.5;
            }
        });
        addKeyAction(KeyInput.KEY_DOWN, (name, isPressed, tpf) -> {
            if (!isPressed) {
                moveSpeed /= 1.5;
            }
        });
    }

    private void addKeyAction(int key, ActionListener action) {
        String mapping = "KEY_MAPPING_" + key;
        inputManager.addMapping(mapping, new KeyTrigger(key));
        inputManager.addListener(action, mapping);
    }

    private void movePieces(int moveIndex, float fraction) {
        for (int piece = 0; piece < assembly.getVoxelsByPiece().size(); piece++) {
            Quaternion q;
            Vector3f t;
            if (fraction > 0.0) {
                Transform t0 = moves.get(moveIndex).get(piece);
                Transform t1 = moves.get(moveIndex + 1).get(piece);

                q = new Quaternion();
                q.slerp(t0.getRotation(), t1.getRotation(), fraction);

                t = new Vector3f();
                t.interpolateLocal(t0.getTranslation(), t1.getTranslation(), fraction);
            } else {
                q = moves.get(moveIndex).get(piece).getRotation();
                t = moves.get(moveIndex).get(piece).getTranslation();
            }
            Node node = pieceNodes.get(piece);
            node.setLocalRotation(q);
            node.setLocalTranslation(t);
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (state != State.PAUSED) {
            time += tpf * direction;
            int moveIndex = (int) round(floor(time * moveSpeed));

            if (state == State.STEPPING && direction > 0.0 && moveIndex >= stepTo) {
                moveIndex = stepTo;
                state = State.PAUSED;
                time = stepTo / moveSpeed;
            } else if (state == State.STEPPING && direction < 0.0 && moveIndex < stepTo) {
                state = State.PAUSED;
                time = stepTo / moveSpeed;
                moveIndex = stepTo;
            }

            if (moveIndex < 0) {
                time = 0.0f;
                moveIndex = 0;
                state = State.PAUSED;
            } else if (moveIndex >= moves.size() - 1) {
                time = (moves.size() - 1) / moveSpeed;
                moveIndex = moves.size() - 1;
                state = State.PAUSED;
            }

            float fraction = time * moveSpeed - moveIndex;
            movePieces(moveIndex, fraction);

            if (moveIndex != currentMoveIndex) {
                currentMoveIndex = moveIndex;
                showMoveIndex();
            } else {
                currentMoveIndex = moveIndex;
            }
        }
    }

    private void showMoveIndex() {
        moveText.setText("Move " + (currentMoveIndex + 1));
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

    private static enum State {
        PAUSED, RUNNING, STEPPING;
    }
}