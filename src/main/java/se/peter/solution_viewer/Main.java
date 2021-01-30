package se.peter.solution_viewer;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import se.peter.solution_viewer.importer.Importer;
import se.peter.solution_viewer.puzzle.Assembly;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main extends SimpleApplication {

    private static final String PAUSE_MAPPING_NAME = "asdfas";

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
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(50);

        List<Assembly> assemblies = new Importer().loadAssemblies(new File("Y:\\Peter\\puzzles\\Own\\There and back again (Level 12 and 21 moves)\\There and back again.xmpuzzle"));

        Assembly assembly = assemblies.get(0);
        List<Node> pieceNodes = new ArrayList<>();
        ColorRGBA[] colors = new ColorRGBA[]{ColorRGBA.Blue, ColorRGBA.Cyan, ColorRGBA.Yellow, ColorRGBA.Orange, ColorRGBA.Red, ColorRGBA.Brown, ColorRGBA.Pink};
        for (int pieceNumber = 0; pieceNumber < assembly.getVoxelsByPiece().size(); pieceNumber++) {
            int[][][] piece = assembly.getVoxelsByPiece().get(pieceNumber);
            Node pieceNode = new Node();
            pieceNodes.add(pieceNode);
            //Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            //mat.setColor("Color", colors[pieceNumber]);
            mat.setTexture("DiffuseMap",
                    assetManager.loadTexture("Textures/Terrain/Pond/Pond.jpg"));
            mat.setTexture("NormalMap",
                    assetManager.loadTexture("Textures/Terrain/Pond/Pond_normal.png"));
            mat.setBoolean("UseMaterialColors", true);
            mat.setColor("Diffuse", colors[pieceNumber]);
            mat.setColor("Specular", ColorRGBA.White);
            mat.setFloat("Shininess", 64f);  // [0,128]

            //mat.getAdditionalRenderState().setWireframe(true);
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
                    Node node = pieceNodes.get(1);
                    node.setLocalTranslation(node.getLocalTranslation().x + 0.1f, node.getLocalTranslation().y, node.getLocalTranslation().z);
                }
            }
        }, PAUSE_MAPPING_NAME);
    }

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
    }

}