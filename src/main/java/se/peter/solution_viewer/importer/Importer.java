package se.peter.solution_viewer.importer;

import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import se.peter.solution_viewer.puzzle.Assembly;
import se.peter.solution_viewer.puzzle.Position;
import se.peter.solution_viewer.util.Rotations;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

public class Importer {
    private static Rotations rot = new Rotations();

    public Document read(File puzzleFile) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Could not create XML document builder.", e);
        }

        try {
            GZIPInputStream iis = new GZIPInputStream(openFile(puzzleFile));
            return readXML(iis, dBuilder);
        } catch (ZipException e) {
            // Fall through to read as XML
        } catch (IOException | SAXException e) {
            throw new IllegalArgumentException("Error reading " + puzzleFile.getAbsolutePath(), e);
        }
        try {
            return readXML(openFile(puzzleFile), dBuilder);
        } catch (SAXException e) {
            throw new IllegalArgumentException("Error reading " + puzzleFile.getAbsolutePath(), e);
        }
    }

    private FileInputStream openFile(File fXmlFile) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(fXmlFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return in;
    }

    private Document readXML(InputStream in, DocumentBuilder dBuilder) throws SAXException {
        Document doc = null;
        try {
            doc = dBuilder.parse(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;
    }

    public List<Assembly> loadAssemblies(File file) {
        Document doc = read(file);

        org.w3c.dom.Node puzzle = getChild(doc, "puzzle");
        List<int[][][]> shapes = new ArrayList<>();
        org.w3c.dom.Node shapesNode = getChild(puzzle, "shapes");

        for (int i = 0; i < shapesNode.getChildNodes().getLength(); i++) {
            org.w3c.dom.Node item = shapesNode.getChildNodes().item(i);
            if (item.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                int maxX = Integer.parseInt(item.getAttributes().getNamedItem("x").getNodeValue());
                int maxY = Integer.parseInt(item.getAttributes().getNamedItem("y").getNodeValue());
                int maxZ = Integer.parseInt(item.getAttributes().getNamedItem("z").getNodeValue());
                int[][][] shape = new int[maxX][maxY][maxZ];
                String text = item.getTextContent();
                for (int x = 0; x < maxX; x++) {
                    for (int y = 0; y < maxY; y++) {
                        for (int z = 0; z < maxZ; z++) {
                            char piece = text.charAt(z * maxY * maxX + y * maxX + x);
                            shape[x][y][z] = (piece == '#' || Character.isDigit(piece)) ? 1 : 0;
                        }
                    }
                }
                shapes.add(shape);
            }
        }

        org.w3c.dom.Node problem = getChild(puzzle, "problems", "problem");
        org.w3c.dom.Node problem_shapes = getChild(problem, "shapes");

        List<Integer> shapesInProblem = new ArrayList<>();
        for (int i = 0; i < problem_shapes.getChildNodes().getLength(); i++) {
            org.w3c.dom.Node item = problem_shapes.getChildNodes().item(i);
            if (item.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                for (int j = 0; j < Integer.parseInt(item.getAttributes().getNamedItem("count").getNodeValue()); j++)
                    shapesInProblem.add(Integer.parseInt(item.getAttributes().getNamedItem("id").getNodeValue()));
            }
        }

        List<Assembly> result = new ArrayList<>();
        org.w3c.dom.Node solutions = getChild(problem, "solutions");
        if (solutions == null) {
            throw new IllegalArgumentException("File contains no solutions.");
        }
        for (int nodeIndex = 0; nodeIndex < solutions.getChildNodes().getLength(); nodeIndex++) {
            Node solution = solutions.getChildNodes().item(nodeIndex);
            if (solution.getNodeType() == 1) {
                org.w3c.dom.Node assembly = getChild(solution, "assembly");
                StringTokenizer tokenizer = new StringTokenizer(assembly.getTextContent(), " ");
                int shape = 0;

                List<int[][][]> shapesInAssembly = new ArrayList<>();
                List<Position> shapeOffsetsInAssembly = new ArrayList<>();
                while (tokenizer.hasMoreTokens()) {
                    int x = Integer.parseInt(tokenizer.nextToken());
                    int y = Integer.parseInt(tokenizer.nextToken());
                    int z = Integer.parseInt(tokenizer.nextToken());
                    int originalRotation = Integer.parseInt(tokenizer.nextToken());
                    Integer[] rotationMap = new Integer[]{
                            0, 11, 12, 23,
                            1, 8, 15, 22,
                            2, 9, 14, 21,
                            3, 10, 13, 20,
                            7, 4, 5, 6,
                            17, 18, 19, 16
                    };
                    int rotation = rotationMap[originalRotation];

                    int[][][] shape1 = shapes.get(shapesInProblem.get(shape));
                    int[][][] shape2 = rotate(shape1, rot.getRotatedPosition(shape1.length, shape1[0].length, shape1[0][0].length)[rotation]);
                    Position originPos = rot.getRotatedPosition(shape1.length, shape1[0].length, shape1[0][0].length)[rotation][0][0][0];
                    shapesInAssembly.add(shape2);
                    shapeOffsetsInAssembly.add(new Position(x - originPos.getX(), y - originPos.getY(), z - originPos.getZ()));

                    shape++;
                }

                int sizeX = 0;
                int sizeY = 0;
                int sizeZ = 0;
                for (int[][][] s : shapesInAssembly) {
                    sizeX = Integer.max(sizeX, s.length);
                    sizeY = Integer.max(sizeY, s[0].length);
                    sizeZ = Integer.max(sizeZ, s[0][0].length);
                }
                List<int[][][]> voxelsByPiece = new ArrayList<>();
                List<Transform> piecePosition = new ArrayList<>();
                for (int i = 0; i < shapesInAssembly.size(); i++) {
                    voxelsByPiece.add(shapesInAssembly.get(i));
                    Position offset = shapeOffsetsInAssembly.get(i);
                    piecePosition.add(new Transform(new Vector3f(offset.getX(), offset.getY(), offset.getZ())));
                }
                result.add(new Assembly(voxelsByPiece, piecePosition));
            }
        }
        return result;
    }

    public List<List<Transform>> loadMoves(File file, List<Transform> initial) {
        Document doc = read(file);
        List<List<Transform>> moves = new ArrayList<>();
        org.w3c.dom.Node solutions = getChild(doc, "puzzle", "problems", "problem", "solutions");
        if (solutions == null) {
            throw new IllegalArgumentException("File contains no solutions.");
        }
        for (int nodeIndex = 0; nodeIndex < solutions.getChildNodes().getLength(); nodeIndex++) {
            Node solution = solutions.getChildNodes().item(nodeIndex);
            if (solution.getNodeType() == 1) {
                for (int separationIndex = 0; separationIndex < solution.getChildNodes().getLength(); separationIndex++) {
                    Node separation = solution.getChildNodes().item(separationIndex);
                    if (separation.getNodeName().equals("separation")) {
                        moves.addAll(parseSeparation(separation, initial));
                    }
                }
            }
        }
        return moves;
    }

    private List<List<Transform>> parseSeparation(Node separation, List<Transform> previous) {
        List<List<Transform>> moves = new ArrayList<>();

        Node piecesNode = getChild(separation, "pieces");
        int pieceCount = Integer.parseInt(piecesNode.getAttributes().getNamedItem("count").getNodeValue());
        String piecesString = piecesNode.getTextContent();
        List<Integer> pieceIndex = Arrays.stream(piecesString.split(" ")).map(Integer::parseInt).collect(Collectors.toList());

        NodeList list = separation.getChildNodes();

        for (int i = 0; i < list.getLength(); i++) {
            Node child = list.item(i);
            if (child.getNodeName().equals("state")) {
                int[] posX = getInts(getChild(child, "dx").getTextContent());
                int[] posY = getInts(getChild(child, "dy").getTextContent());
                int[] posZ = getInts(getChild(child, "dz").getTextContent());

                List<Transform> transforms = new ArrayList<>(previous);
                for (int k = 0; k < pieceCount; k++) {
                    int piece = pieceIndex.get(k);
                    transforms.set(piece, new Transform(new Vector3f(posX[k], posY[k], posZ[k])));
                }

                if (!transforms.equals(previous)) {
                    moves.add(transforms);
                    previous = transforms;
                }
            } else if (child.getNodeName().equals("separation")) {
                moves.addAll(parseSeparation(child, previous));
            }
        }
        return moves;
    }

    private org.w3c.dom.Node getChild(org.w3c.dom.Node parent, String... nodeName) {
        if (nodeName.length == 0) {
            return parent;
        }
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals(nodeName[0])) {
                return getChild(node, Arrays.copyOfRange(nodeName, 1, nodeName.length));
            }
        }
        return null;
    }

    private int[] getInts(String text) {
        StringTokenizer tokenizer = new StringTokenizer(text);
        List<Integer> result = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            result.add(Integer.parseInt(tokenizer.nextToken()));
        }

        return result.stream().mapToInt(v -> v).toArray();
    }

    private int[][][] rotate(int[][][] shape, Position[][][] rotatedPosition) {
        List<Position> boundaries = List.of(
                rotatedPosition[0][0][0],
                rotatedPosition[0][0][shape[0][0].length - 1],
                rotatedPosition[0][shape[0].length - 1][0],
                rotatedPosition[0][shape[0].length - 1][shape[0][0].length - 1],
                rotatedPosition[shape.length - 1][0][0],
                rotatedPosition[shape.length - 1][0][shape[0][0].length - 1],
                rotatedPosition[shape.length - 1][shape[0].length - 1][0],
                rotatedPosition[shape.length - 1][shape[0].length - 1][shape[0][0].length - 1]);
        int maxX = boundaries.stream().mapToInt(Position::getX).max().getAsInt();
        int maxY = boundaries.stream().mapToInt(Position::getY).max().getAsInt();
        int maxZ = boundaries.stream().mapToInt(Position::getZ).max().getAsInt();

        int[][][] newshape = new int[maxX + 1][maxY + 1][maxZ + 1];
        for (int x = 0; x < shape.length; x++) {
            for (int y = 0; y < shape[0].length; y++) {
                for (int z = 0; z < shape[0][0].length; z++) {
                    Position pos = rotatedPosition[x][y][z];
                    newshape[pos.getX()][pos.getY()][pos.getZ()] = shape[x][y][z];
                }
            }
        }
        return newshape;
    }
}
