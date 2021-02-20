package se.peter.solution_viewer;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
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

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.floor;
import static java.lang.Math.round;

public class Main extends SimpleApplication {
    private static final boolean DUMP_MOVES = false;

    private float moveSpeed = 1f;
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
        setupLogging();

        if (args.length != 1) {
            System.err.println("Usage: java -jar target\\solution-viewer-1.0-SNAPSHOT-jar-with-dependencies.jar <xmpuzzle file>");
            return;
        }
        File file = new File(args[0]);
        if (!file.exists() || !file.canRead()) {
            System.err.println("Cannot read file " + file.getAbsolutePath());
            return;
        }

        Main app = new Main(file);
        app.setShowSettings(false);

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int width = gd.getDisplayMode().getWidth();
        int height = gd.getDisplayMode().getHeight();

        AppSettings settings = new AppSettings(true);
        settings.put("Width", width / 2);
        settings.put("Height", height / 2);
        settings.setResizable(true);
        settings.put("VSync", true);
        settings.put("Samples", 4);
        settings.setTitle("Solution viewer");
        app.setSettings(settings);

        app.start();
    }

    private static void setupLogging() {
        Arrays.stream(Logger.getLogger("").getHandlers()).forEach(h -> Logger.getLogger("").removeHandler(h));

        try {
            Logger.getLogger("").addHandler(new FileHandler("solution_viewer.log"));
        } catch (IOException e) {
            System.err.println("Error setting up file logging.");
            System.exit(1);
        }

        Logger.getLogger("").setLevel(Level.WARNING);
    }

    @Override
    public void simpleInitApp() {
        Importer importer = new Importer();

        List<Assembly> assemblies = null;
        try {
            assemblies = importer.loadAssemblies(file);
        } catch (Exception e) {
            System.err.println("Error loading solution file " + file.getAbsolutePath());
            e.printStackTrace();
            System.exit(1);
        }

        assembly = assemblies.get(assemblies.size() - 1);
        System.out.println("Assembly " + assembly.getAssemblyNumber() + ", solution " + assembly.getSolutionNumber());

        List<int[][][]> voxelsByPiece = assembly.getVoxelsByPiece();
        int pieceCount = voxelsByPiece.size();
        moves = assembly.getPiecePositionsByMove();

        if (DUMP_MOVES) {
            moves.forEach(m -> {
                System.out.println("--------");
                for (int i = 0; i < voxelsByPiece.size(); i++) {
                    Transform transform = m.get(i);
                    System.out.println("Piece " + i + " " + transform.getTranslation() + " " + transform.getRotation());
                }
            });
        }

        pieceNodes = new ArrayList<>();
        List<Transform> initialPositions = assembly.getPiecePositionsByMove().get(0);

        for (int pieceNumber = 1; pieceNumber <= pieceCount; pieceNumber++) {
            Node pieceNode = createPiece(voxelsByPiece.get(pieceNumber - 1), initialPositions.get(pieceNumber - 1));
            pieceNodes.add(pieceNode);
            rootNode.attachChild(pieceNode);
        }

        viewPort.setBackgroundColor(ColorRGBA.White);
        setupControls();
        setupCamera();

        attachCoordinateAxes(rootNode);

        createUI();
    }

    private void createUI() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        moveText = new BitmapText(guiFont, false);
        moveText.setSize(guiFont.getCharSet().getRenderedSize());
        showMoveIndex();
        moveText.setLocalTranslation(500, moveText.getLineHeight(), 0);
        moveText.setColor(ColorRGBA.Black);
        guiNode.attachChild(moveText);

        rootNode.attachChild(guiNode);
    }

    private Node createPiece(int[][][] piece, Transform initialPosition) {
        Node pieceNode = new Node();

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture("WM_IndoorWood-44_1024.png"));
        mat.setColor("Color", ColorRGBA.randomColor());

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

        pieceNode.setLocalTransform(initialPosition);
        return pieceNode;
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

    private enum State {
        PAUSED, RUNNING, STEPPING
    }
}