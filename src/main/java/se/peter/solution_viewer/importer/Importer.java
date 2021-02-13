package se.peter.solution_viewer.importer;

import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import se.peter.solution_viewer.puzzle.Assembly;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import static java.lang.Character.isDigit;

public class Importer {
    private static final int[][] rotationMatrices = {
            {1, 0, 0, 0, 1, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, -1, 0, 1, 0},
            {1, 0, 0, 0, -1, 0, 0, 0, -1},
            {1, 0, 0, 0, 0, 1, 0, -1, 0},
            {0, 0, -1, 0, 1, 0, 1, 0, 0},
            {0, -1, 0, 0, 0, -1, 1, 0, 0},
            {0, 0, 1, 0, -1, 0, 1, 0, 0},
            {0, 1, 0, 0, 0, 1, 1, 0, 0},
            {-1, 0, 0, 0, 1, 0, 0, 0, -1},
            {-1, 0, 0, 0, 0, -1, 0, -1, 0},
            {-1, 0, 0, 0, -1, 0, 0, 0, 1},
            {-1, 0, 0, 0, 0, 1, 0, 1, 0},
            {0, 0, 1, 0, 1, 0, -1, 0, 0},
            {0, 1, 0, 0, 0, -1, -1, 0, 0},
            {0, 0, -1, 0, -1, 0, -1, 0, 0},
            {0, -1, 0, 0, 0, 1, -1, 0, 0},
            {0, -1, 0, 1, 0, 0, 0, 0, 1},
            {0, 0, 1, 1, 0, 0, 0, 1, 0},
            {0, 1, 0, 1, 0, 0, 0, 0, -1},
            {0, 0, -1, 1, 0, 0, 0, -1, 0},
            {0, 1, 0, -1, 0, 0, 0, 0, 1},
            {0, 0, -1, -1, 0, 0, 0, 1, 0},
            {0, -1, 0, -1, 0, 0, 0, 0, -1},
            {0, 0, 1, -1, 0, 0, 0, -1, 0}
    };

    public Document read(File puzzleFile) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
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

                int skipped = 0;
                for (int j = 0; j < text.length(); j++) {
                    char piece = text.charAt(j);
                    if (isDigit(piece)) {
                        skipped++;
                    } else {
                        int pos = j - skipped;
                        int x = pos % maxX;
                        int y = (pos / maxX) % maxY;
                        int z = pos / maxY / maxX;

                        shape[x][y][z] = (piece == '#' || piece == '+') ? 1 : 0;
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
                NamedNodeMap attributes = item.getAttributes();
                Node count = attributes.getNamedItem("count");
                int piecesCountInProblem = Integer.parseInt((count != null ? count : attributes.getNamedItem("max")).getNodeValue());
                for (int j = 0; j < piecesCountInProblem; j++)
                    shapesInProblem.add(Integer.parseInt(attributes.getNamedItem("id").getNodeValue()));
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

                List<int[][][]> voxelsByPiece = getVoxelsByPiece(shapes, shapesInProblem, assembly);

                List<Transform> piecePosition = getPiecePositions(assembly);

                int assemblyNumber = getIntegerAttribute(solution, "asmNum");
                int solutionNumber = getIntegerAttribute(solution, "solNum");

                List<List<Transform>> moves = loadMoves(solution, piecePosition.stream().map(t -> new Transform().setRotation(t.getRotation())).collect(Collectors.toList()));
                result.add(new Assembly(assemblyNumber, solutionNumber, voxelsByPiece, moves));
            }
        }
        if (result.stream().allMatch(a -> a.getPiecePositionsByMove().isEmpty())) {
            Assembly assembly = loadAssemblyGeneratedByAndrew(result, solutions);
            result.add(assembly);
        }
        return result;
    }

    private Assembly loadAssemblyGeneratedByAndrew(List<Assembly> result, Node solutions) {
        List<int[][][]> voxelsByPiece = result.get(0).getVoxelsByPiece();

        result.clear();
        List<List<Transform>> positions = new ArrayList<>();
        for (int nodeIndex = 0; nodeIndex < solutions.getChildNodes().getLength(); nodeIndex++) {
            Node solution = solutions.getChildNodes().item(nodeIndex);
            if (solution.getNodeType() == 1) {
                Node assembly = getChild(solution, "assembly");
                positions.add(getPiecePositions(assembly).stream().map(t -> t != null ? t : new Transform(new Vector3f(100, 100, 100))).collect(Collectors.toList()));
            }
        }
        Assembly assembly = new Assembly(1, 1, voxelsByPiece, positions);
        return assembly;
    }

    private List<int[][][]> getVoxelsByPiece(List<int[][][]> shapes, List<Integer> shapesInProblem, Node assembly) {
        List<int[][][]> voxelsByPiece = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(assembly.getTextContent(), " ");
        int shape = 0;
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (!token.equals("x")) {
                tokenizer.nextToken();
                tokenizer.nextToken();
                tokenizer.nextToken();

                voxelsByPiece.add(shapes.get(shapesInProblem.get(shape)));
            }
            shape++;
        }
        return voxelsByPiece;
    }

    private List<Transform> getPiecePositions(Node assembly) {
        StringTokenizer tokenizer = new StringTokenizer(assembly.getTextContent(), " ");
        List<Transform> piecePosition = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (!token.equals("x")) {
                int x = Integer.parseInt(token);
                int y = Integer.parseInt(tokenizer.nextToken());
                int z = Integer.parseInt(tokenizer.nextToken());
                int rotation = Integer.parseInt(tokenizer.nextToken());
                int[] rotationMatrix = rotationMatrices[rotation];
                Quaternion q = new Quaternion().fromRotationMatrix(
                        rotationMatrix[0], rotationMatrix[1], rotationMatrix[2],
                        rotationMatrix[3], rotationMatrix[4], rotationMatrix[5],
                        rotationMatrix[6], rotationMatrix[7], rotationMatrix[8]
                );

                Transform transform = new Transform(new Vector3f(x, y, z));
                transform.setRotation(q);
                piecePosition.add(transform);
            } else {
                piecePosition.add(null);
            }
        }
        return piecePosition;
    }

    private int getIntegerAttribute(Node solution, String name) {
        Node node = solution.getAttributes().getNamedItem(name);
        return (node != null) ? Integer.parseInt(node.getNodeValue()) + 1 : 1;
    }

    private List<List<Transform>> loadMoves(Node solution, List<Transform> initial) {
        List<List<Transform>> moves = new ArrayList<>();
        if (solution.getNodeType() == 1) {
            for (int separationIndex = 0; separationIndex < solution.getChildNodes().getLength(); separationIndex++) {
                Node separation = solution.getChildNodes().item(separationIndex);
                if (separation.getNodeName().equals("separation")) {
                    moves.addAll(parseSeparation(separation, initial));
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
                    Vector3f translation = new Vector3f(posX[k], posY[k], posZ[k]);
                    System.out.println(piece + " " + translation);
                    Transform transform = new Transform(translation);
                    transform.setRotation(previous.get(piece).getRotation());
                    transforms.set(piece, transform);
                    System.out.println(piece + " " + transform.getTranslation());
                }

                if (!transforms.equals(previous)) {
                    moves.add(transforms.stream().filter(Objects::nonNull).collect(Collectors.toList()));
                    previous = transforms;
                }
            } else if (child.getNodeName().equals("separation") && child.getAttributes().getNamedItem("type").getNodeValue().equals("left")) {
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
}
