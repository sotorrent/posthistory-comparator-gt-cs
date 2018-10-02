package de.unitrier.st.soposthistory.gt.ComparatorAppOfGroundTruthAndComputedSimilarity;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.sotorrent.posthistoryextractor.blocks.PostBlockVersion;
import org.sotorrent.posthistoryextractor.gt.PostBlockLifeSpanVersion;
import org.sotorrent.posthistoryextractor.version.PostVersion;
import org.sotorrent.posthistoryextractor.version.PostVersionList;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller {

    /* GUI items */

    @FXML private VBox frame;

    @FXML private TextField textFieldPostId;

    @FXML private CheckBox checkBoxShowConnectionsOfGroundTruth;
    @FXML private CheckBox checkBoxShowConnectionsOfComputedSimilarity;
    @FXML private RadioButton radioButtonShowNoDiffs;
    @FXML private RadioButton radioButtonShowDiffsOfGroundTruth;
    @FXML private RadioButton radioButtonShowDiffsOfComputedSimilarity;

    @FXML private VBox leftVBox;
    @FXML private VBox rightVBox;
    @FXML private Pane connectionsPane;

    @FXML private Label bottomLabel;


    private enum BlockBorderColorStatus {blockConnectionNotSet, blockConnectionSet, blockMarked}

    private final Color colorForTextNotConnected = new Color(128. / 255, 212. / 255, 255. / 255, 1.0);
    private final Color colorForCodeNotConnected = new Color(255. / 255, 204. / 255, 128. / 255, 1.0);
    private final Color colorForTextWithSetConnection = new Color(196. / 255, 236. / 255, 255. / 255, 0.5);
    private final Color colorForCodeWithSetConnection = new Color(255. / 255, 233. / 255, 199. / 255, 0.5);
    private final Color colorForClickedBlock = new Color(255. / 255, 114. / 255, 252. / 255, 1.0);


    /* intern variables */
    private Path pathToSelectedRootOfPostVersionLists;
    private Map<Integer, File> allIndexedPostVersionLists;
    private PostVersionList currentPostVersionList;
    private boolean[] postVersionsWithDifferentLinks;

    private List<javafx.scene.Node> allElementsInGUI = new ArrayList<>();

    private List<BlockPair> blockPairs_groundTruth = new LinkedList<>();
    private List<BlockPair> blockPairs_computedSimilarity = new LinkedList<>();

    private int currentLeftVersionInViewedPost = 0;

    private ToggleGroup group = new ToggleGroup();


    @FXML
    private void initialize(){

        changeDirectoryOfPostVersionLists();

        unionRadioButtonsForDiffToSameGroup();
    }


    /* Setup of directory to post version lists */
    @FXML
    private void changeDirectoryOfPostVersionLists() {
        setPathToRootOfPostVersionLists();
        indexPostVersionListsInSelectedPath();
    }

    // helping methods for setup of directory
    private void setPathToRootOfPostVersionLists() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        try {
            directoryChooser.setInitialDirectory(pathToSelectedRootOfPostVersionLists.toFile());
        } catch (Exception ignored) {
        }
        directoryChooser.setTitle("Select directory of your Post Version Lists");
        File selectedDirectory = directoryChooser.showDialog(new Stage());
        pathToSelectedRootOfPostVersionLists = Paths.get(String.valueOf(selectedDirectory));
    }

    private void indexPostVersionListsInSelectedPath() {
        File[] postVersionListsInCSVFiles = pathToSelectedRootOfPostVersionLists.toFile().listFiles((directory, name) -> name.matches("\\d+\\.csv"));
        if (postVersionListsInCSVFiles == null) {
            return;
        }

        allIndexedPostVersionLists = new HashMap<>();
        for (File postVersionListsInCSVFile : postVersionListsInCSVFiles) {
            allIndexedPostVersionLists.put(Integer.valueOf(postVersionListsInCSVFile.getName().replace(".csv", "")), postVersionListsInCSVFile);
        }
    }

    private void unionRadioButtonsForDiffToSameGroup() {
        radioButtonShowNoDiffs.setToggleGroup(group);
        radioButtonShowDiffsOfGroundTruth.setToggleGroup(group);
        radioButtonShowDiffsOfComputedSimilarity.setToggleGroup(group);

        radioButtonShowNoDiffs.setSelected(true);
    }



    /* GUI */
    private void visualizeInGUI() {
        resetGUI();

        visualizePostBlocksOnLeftSide();
        visualizePostBlocksOnRightSide();

        updateVisualizationAtBottomLabel();

        repaintAllConnectionsViaWorkaround(); // FIXME: Find solution to make this workaround superfluous

        Platform.runLater(this::visualizeRelationsBetweenPostBlocks);
    }

    // helping methods for setup of GUI
    private void resetGUI() {
        leftVBox.getChildren().clear();
        rightVBox.getChildren().clear();
        connectionsPane.getChildren().clear();
    }

    private void visualizePostBlocksOnLeftSide() {
        List<PostBlockVersion> leftPostBlocks = currentPostVersionList.get(currentLeftVersionInViewedPost).getPostBlocks();
        for (PostBlockVersion leftPostBlock : leftPostBlocks) {
            PostBlockWebView postBlockWebView = new PostBlockWebView(leftPostBlock);

            leftVBox.getChildren().add(postBlockWebView.webViewFitContent.webview);

            String convertedMarkdownText = convertMarkdownToHTML(
                    leftPostBlock,
                    BlockBorderColorStatus.blockConnectionNotSet);

            postBlockWebView.webViewFitContent.setContent(convertedMarkdownText);
            postBlockWebView.webViewFitContent.webEngine.loadContent(convertedMarkdownText);

            synchronizeBlockPairsWithGUI_leftSide(leftPostBlock, postBlockWebView);

        }
    }

    private void synchronizeBlockPairsWithGUI_leftSide(PostBlockVersion leftPostBlock, PostBlockWebView postBlockWebView) {
        for (BlockPair blockPair : blockPairs_groundTruth) {
            if (blockPair.leftBlock.postBlock.getPostHistoryId().equals(leftPostBlock.getPostHistoryId())
                    && blockPair.leftBlock.postBlock.getLocalId().equals(leftPostBlock.getLocalId())) {
                blockPair.leftBlock = postBlockWebView;
            }
        }
        for (BlockPair blockPair : blockPairs_computedSimilarity) {
            if (blockPair.leftBlock.postBlock.getPostHistoryId().equals(leftPostBlock.getPostHistoryId())
                    && blockPair.leftBlock.postBlock.getLocalId().equals(leftPostBlock.getLocalId())) {
                blockPair.leftBlock = postBlockWebView;
            }
        }
    }

    private void visualizePostBlocksOnRightSide() {
        List<PostBlockVersion> rightPostBlocks = currentPostVersionList.get(currentLeftVersionInViewedPost + 1).getPostBlocks();
        for (PostBlockVersion rightPostBlock : rightPostBlocks) {
            PostBlockWebView postBlockWebView = new PostBlockWebView(rightPostBlock);

            rightVBox.getChildren().add(postBlockWebView.webViewFitContent.webview);

            String convertedMarkdownText = convertMarkdownToHTML(
                    rightPostBlock,
                    BlockBorderColorStatus.blockConnectionNotSet);

            postBlockWebView.webViewFitContent.setContent(convertedMarkdownText);
            postBlockWebView.webViewFitContent.webEngine.loadContent(convertedMarkdownText);

            synchronizeBlockPairsWithGUI_rightSide(rightPostBlock, postBlockWebView);

        }
    }

    private void synchronizeBlockPairsWithGUI_rightSide(PostBlockVersion rightPostBlock, PostBlockWebView postBlockWebView) {
        for (BlockPair blockPair : blockPairs_groundTruth) {
            if (blockPair.rightBlock.postBlock.getPostHistoryId().equals(rightPostBlock.getPostHistoryId())
                    && blockPair.rightBlock.postBlock.getLocalId().equals(rightPostBlock.getLocalId())) {
                blockPair.rightBlock = postBlockWebView;
            }
        }
        for (BlockPair blockPair : blockPairs_computedSimilarity) {
            if (blockPair.rightBlock.postBlock.getPostHistoryId().equals(rightPostBlock.getPostHistoryId())
                    && blockPair.rightBlock.postBlock.getLocalId().equals(rightPostBlock.getLocalId())) {
                blockPair.rightBlock = postBlockWebView;
            }
        }
    }

    private void updateVisualizationAtBottomLabel() {
        bottomLabel.setText(
                "Post ID: " + currentPostVersionList.getFirst().getPostId()
                        + " ### number of versions: " + currentPostVersionList.size()
                        + " ### you are now comparing the versions " + (currentLeftVersionInViewedPost + 1) + " and " + (currentLeftVersionInViewedPost + 2));
    }

    private void repaintAllConnectionsViaWorkaround() {
        addAllDecedents(frame, allElementsInGUI);
        for (javafx.scene.Node child : allElementsInGUI) {
            child.setOnMouseMoved(event -> visualizeRelationsBetweenPostBlocks());
            child.setOnScroll(event -> visualizeRelationsBetweenPostBlocks());
            child.setOnKeyReleased(event -> visualizeRelationsBetweenPostBlocks());
        }
    }

    // https://stackoverflow.com/a/24986845
    private static void addAllDecedents(javafx.scene.Parent parent, List<javafx.scene.Node> childrenInGUI) {
        for (javafx.scene.Node guiElement : parent.getChildrenUnmodifiable()) {
            childrenInGUI.add(guiElement);
            if (guiElement instanceof javafx.scene.Parent)
                addAllDecedents((javafx.scene.Parent)guiElement, childrenInGUI);
        }
    }


    /* paint connections */
    @FXML
    private void visualizeRelationsBetweenPostBlocks() {

        connectionsPane.getChildren().clear();

        paintConnectionsBetweenPostBlocks();

        visualizeDiffsOnRightSide();

    }

    // helping methods to paint connections
    private void paintConnectionsBetweenPostBlocks() {
        if (checkBoxShowConnectionsOfGroundTruth.isSelected()) {
            for (BlockPair blockPair : blockPairs_groundTruth) {
                if (blockPair.leftVersion == currentLeftVersionInViewedPost) {

                    blockPair.leftBlock.webViewFitContent.setContent(convertMarkdownToHTML(blockPair.leftBlock.postBlock, BlockBorderColorStatus.blockConnectionSet));
                    blockPair.leftBlock.webViewFitContent.webview.getEngine().loadContent(convertMarkdownToHTML(blockPair.leftBlock.postBlock, BlockBorderColorStatus.blockConnectionSet));

                    Polygon polygon = paintPolygonOfConnections(blockPair.rightBlock, blockPair.leftBlock);
                    connectionsPane.getChildren().add(polygon);
                }
            }
        }

        if (checkBoxShowConnectionsOfComputedSimilarity.isSelected()) {
            for (BlockPair blockPair : blockPairs_computedSimilarity) {
                if (blockPair.leftVersion == currentLeftVersionInViewedPost) {

                    blockPair.leftBlock.webViewFitContent.setContent(convertMarkdownToHTML(blockPair.leftBlock.postBlock, BlockBorderColorStatus.blockConnectionSet));
                    blockPair.leftBlock.webViewFitContent.webview.getEngine().loadContent(convertMarkdownToHTML(blockPair.leftBlock.postBlock, BlockBorderColorStatus.blockConnectionSet));

                    Line line = paintLineOfConnections(blockPair.rightBlock, blockPair.leftBlock);

                    connectionsPane.getChildren().add(line);
                }
            }
        }
    }

    private Line paintLineOfConnections(PostBlockWebView leftBlock, PostBlockWebView rightBlock) {
        Line line = new Line(
                leftBlock.webViewFitContent.webview.getLayoutX() + leftBlock.webViewFitContent.webview.getWidth(),
                leftBlock.webViewFitContent.webview.getLayoutY() + leftBlock.webViewFitContent.webview.getHeight() / 2,
                rightBlock.webViewFitContent.webview.getLayoutX(),
                rightBlock.webViewFitContent.webview.getLayoutY() + rightBlock.webViewFitContent.webview.getHeight() / 2
        );

        line.setStrokeWidth(10);
        line.setStroke(Color.GRAY);

        return line;
    }

    private Polygon paintPolygonOfConnections(PostBlockWebView leftBlock, PostBlockWebView rightBlock) {
        Polygon polygon = new Polygon();

        polygon.getPoints().addAll(
                leftBlock.webViewFitContent.webview.getLayoutX() + leftBlock.webViewFitContent.webview.getWidth(),
                leftBlock.webViewFitContent.webview.getLayoutY(),

                rightBlock.webViewFitContent.webview.getLayoutX(),
                rightBlock.webViewFitContent.webview.getLayoutY(),

                rightBlock.webViewFitContent.webview.getLayoutX(),
                rightBlock.webViewFitContent.webview.getLayoutY() + rightBlock.webViewFitContent.webview.getHeight(),

                leftBlock.webViewFitContent.webview.getLayoutX() + leftBlock.webViewFitContent.webview.getWidth(),
                leftBlock.webViewFitContent.webview.getLayoutY() + leftBlock.webViewFitContent.webview.getHeight()
        );


        polygon.setFill(leftBlock.postBlock.getPostBlockTypeId() == 1 ? colorForTextWithSetConnection : leftBlock.postBlock.getPostBlockTypeId() == 2 ? colorForCodeWithSetConnection : null);

        return polygon;
    }

    private void visualizeDiffsOnRightSide() {
        if (group.getSelectedToggle() == radioButtonShowDiffsOfGroundTruth
                && checkBoxShowConnectionsOfGroundTruth.isSelected()) {
            paintRightSideWithoutDiffs();
            for (BlockPair blockPair : blockPairs_groundTruth) {
                if (blockPair.leftVersion == currentLeftVersionInViewedPost) {
                    visualizeRightSideOfPostBlockWithDiffs(
                            blockPair,
                            true
                    );
                }
            }

        } else if (group.getSelectedToggle() == radioButtonShowDiffsOfComputedSimilarity
                && checkBoxShowConnectionsOfComputedSimilarity.isSelected()) {
            paintRightSideWithoutDiffs();
            for (BlockPair blockPair : blockPairs_computedSimilarity) {
                if (blockPair.leftVersion == currentLeftVersionInViewedPost) {
                    visualizeRightSideOfPostBlockWithDiffs(
                            blockPair,
                            true
                    );
                }
            }
        } else {
            paintRightSideWithoutDiffs();
        }
    }

    private void paintRightSideWithoutDiffs() {
        for (BlockPair blockPair : blockPairs_groundTruth) {
            if (blockPair.leftVersion == currentLeftVersionInViewedPost) {
                visualizeRightSideOfPostBlockWithDiffs(
                        blockPair,
                        false
                );
            }
        }
        for (BlockPair blockPair : blockPairs_computedSimilarity) {
            if (blockPair.leftVersion == currentLeftVersionInViewedPost) {
                visualizeRightSideOfPostBlockWithDiffs(
                        blockPair,
                        false
                );
            }
        }
    }

    private void visualizeRightSideOfPostBlockWithDiffs(BlockPair blockPair, boolean visualizeDiffs) {
        if (!visualizeDiffs) {
            blockPair.rightBlock.webViewFitContent.setContent(convertMarkdownToHTML(blockPair.rightBlock.postBlock, BlockBorderColorStatus.blockConnectionSet));
            blockPair.rightBlock.webViewFitContent.webview.getEngine().loadContent(convertMarkdownToHTML(blockPair.rightBlock.postBlock, BlockBorderColorStatus.blockConnectionSet));
        } else {
            blockPair.rightBlock.webViewFitContent.setContent(wrapPostBlockWithBorderColor(
                    Util.computeDiffs(blockPair),
                    blockPair.rightBlock.postBlock.getPostBlockTypeId() == 1 ? colorForTextWithSetConnection : blockPair.rightBlock.postBlock.getPostBlockTypeId() == 2 ? colorForCodeWithSetConnection : Color.gray(0)));
            blockPair.rightBlock.webViewFitContent.webview.getEngine().loadContent(
                    wrapPostBlockWithBorderColor(
                            Util.computeDiffs(blockPair),
                            blockPair.rightBlock.postBlock.getPostBlockTypeId() == 1 ? colorForTextWithSetConnection : blockPair.rightBlock.postBlock.getPostBlockTypeId() == 2 ? colorForCodeWithSetConnection : Color.gray(0)));
        }
    }

    private String convertMarkdownToHTML(PostBlockVersion postBlock, BlockBorderColorStatus blockBorderColorStatus) {
        String convertedMarkdownText = convertMarkdownToHTMLViaCommonmarkMark(
                postBlock.getContent()
        );

        switch (blockBorderColorStatus) {
            case blockMarked:
                convertedMarkdownText = wrapPostBlockWithBorderColor(convertedMarkdownText, colorForClickedBlock);
                break;
            case blockConnectionNotSet:
                convertedMarkdownText = wrapPostBlockWithBorderColor(convertedMarkdownText, postBlock.getPostBlockTypeId() == 1 ? colorForTextNotConnected : postBlock.getPostBlockTypeId() == 2 ? colorForCodeNotConnected : Color.gray(0));
                break;
            case blockConnectionSet:
                convertedMarkdownText = wrapPostBlockWithBorderColor(convertedMarkdownText, postBlock.getPostBlockTypeId() == 1 ? colorForTextWithSetConnection : postBlock.getPostBlockTypeId() == 2 ? colorForCodeWithSetConnection : Color.gray(0));
                break;
        }

        return convertedMarkdownText;
    }

    static String convertMarkdownToHTMLViaCommonmarkMark(String markdownText) {   // https://github.com/atlassian/commonmark-java
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdownText);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(document);
    }

    private String wrapPostBlockWithBorderColor(String text, Color color) {

        return
                "<head>\n" +
                        "<style>\n" +
                        "div.borderColor {\n" +
                        "    border-style: solid;\n" +
                        "    border-radius: 5px;\n" +
                        "    border-width: medium;\n" +
                        "    border-color: " + String.format("#%02x%02x%02x",
                        (int) (color.getRed() * 255),
                        (int) (color.getGreen() * 255),
                        (int) (color.getBlue() * 255)) + ";\n" +
                        "}\n" +
                        "</style>\n" +
                        "</head>" +
                        "<body>" +
                        "<div class=\"borderColor\">\n" +
                        text +
                        "</div>\n" +
                        "</body>";
    }



    /* Buttons */
    @FXML
    private void loadButtonClicked() {
        blockPairs_groundTruth.clear();
        blockPairs_computedSimilarity.clear();
        currentLeftVersionInViewedPost = 0;

        int postId = Integer.valueOf(textFieldPostId.getText());
        try {
            this.currentPostVersionList = PostVersionList.readFromCSV(Paths.get(String.valueOf(allIndexedPostVersionLists.get(postId))).getParent(), postId, (byte) 2);
        } catch (Exception e) {
            System.err.println("Could not parse CSV with post id " + postId + "." + "\n" + "Make sure the file is well-formed and listed in the stated path.");
            return;
        }

        importConnectionsOfGroundTruth();
        importConnectionsOfComputedSimilarity();

        postVersionsWithDifferentLinks = new boolean[currentPostVersionList.size()];
        identifyPostHistoriesWithIdenticalConnectionsOfGroundTruthAndComputedSimilarity();

        visualizeInGUI();
    }

    // helping methods to realize function of load button
    private void importConnectionsOfGroundTruth() {

        try {
            // extract connections from CSV
            String completedFileCSV = new String(Files.readAllBytes(Paths.get(pathToSelectedRootOfPostVersionLists.toString(), "completed_" + currentPostVersionList.getPostId() + ".csv")), StandardCharsets.UTF_8);

            Pattern groundTruthCSVRegex = Pattern.compile("(\\d+);(\\d+);([12]);(\\d+);(null|\\d+);(null|\\d+);");
            Matcher matcher = groundTruthCSVRegex.matcher(completedFileCSV);

            List<PostBlockLifeSpanVersion> postBlockLifeSpanVersionList = new LinkedList<>();

            while (matcher.find()) {
                int postId = Integer.parseInt(matcher.group(1));
                int postHistoryId = Integer.parseInt(matcher.group(2));
                byte postBlockTypeId = Byte.parseByte(matcher.group(3));
                int localId = Integer.parseInt(matcher.group(4));

                Integer predLocalId = null;
                Integer succLocalId = null;

                try {
                    predLocalId = Integer.parseInt(matcher.group(5));
                } catch (Exception ignored) {
                }

                try {
                    succLocalId = Integer.parseInt(matcher.group(6));
                } catch (Exception ignored) {
                }


                postBlockLifeSpanVersionList.add(
                        new PostBlockLifeSpanVersion(
                                postId,
                                postHistoryId,
                                postBlockTypeId,
                                localId,
                                predLocalId,
                                succLocalId
                        )
                );
            }

            // sort lines for easier assignment
            postBlockLifeSpanVersionList.sort((o1, o2) -> {
                if (o1.getPostId() < o2.getPostId()) {
                    return -1;
                } else if (o1.getPostId() > o2.getPostId()) {
                    return 1;
                } else if (o1.getPostHistoryId() < o2.getPostHistoryId()) {
                    return -1;
                } else if (o1.getPostHistoryId() > o2.getPostHistoryId()) {
                    return 1;
                }

                return Integer.compare(o1.getLocalId(), o2.getLocalId());
            });

            // add connections to block pairs
            int version = 0;
            Integer lastPostHistoryId = null;
            for (PostBlockLifeSpanVersion postBlockLifeSpanVersion : postBlockLifeSpanVersionList) {

                if (lastPostHistoryId != null && postBlockLifeSpanVersion.getPostHistoryId() > lastPostHistoryId)
                    version++;

                if (postBlockLifeSpanVersion.getSuccLocalId() != null) {
                    blockPairs_groundTruth.add(
                            new BlockPair(
                                    new PostBlockWebView(
                                            currentPostVersionList.get(version).getPostBlocks().get(postBlockLifeSpanVersion.getLocalId() - 1)
                                    ),
                                    new PostBlockWebView(
                                            currentPostVersionList.get(version + 1).getPostBlocks().get(postBlockLifeSpanVersion.getSuccLocalId() - 1)
                                    ),
                                    version
                            )
                    );

                    lastPostHistoryId = postBlockLifeSpanVersion.getPostHistoryId();
                }
            }
        } catch (IOException e) {
            System.err.println("Could not import the connections of the Ground Truth." + "\n" + "Make sure the file is well-formed and listed in the stated path.");
        }

    }

    private void importConnectionsOfComputedSimilarity() {
        currentPostVersionList.processVersionHistory();

        for (int i = 0; i< currentPostVersionList.size()-1 ; i++) {
            for (int j = 0; j< currentPostVersionList.get(i).getPostBlocks().size(); j++) {
                if (currentPostVersionList.get(i).getPostBlocks().get(j).getSucc() != null) {
                    blockPairs_computedSimilarity.add(
                            new BlockPair(
                                    new PostBlockWebView(
                                            currentPostVersionList.get(i).getPostBlocks().get(j)
                                    ),
                                    new PostBlockWebView(
                                            currentPostVersionList.get(i).getPostBlocks().get(j).getSucc()
                                    ),
                                    i
                            )
                    );
                }
            }
        }
    }

    private void identifyPostHistoriesWithIdenticalConnectionsOfGroundTruthAndComputedSimilarity(){
        for (int i=0; i<currentPostVersionList.size()-1; i++) {
            PostVersion postVersion = currentPostVersionList.get(i);

            List<BlockPair> blockPairsInCurrentPostVersionOfGT = new ArrayList<>();
            for (BlockPair blockPairsGT : blockPairs_groundTruth) {
                if (blockPairsGT.leftBlock.postBlock.getPostHistoryId().equals(postVersion.getPostHistoryId())) {
                    blockPairsInCurrentPostVersionOfGT.add(blockPairsGT);
                }
            }

            List<BlockPair> blockPairsInCurrentPostVersionOfCS = new ArrayList<>();
            for (BlockPair blockPairsCS : blockPairs_computedSimilarity) {
                if (blockPairsCS.leftBlock.postBlock.getPostHistoryId().equals(postVersion.getPostHistoryId())) {
                    blockPairsInCurrentPostVersionOfCS.add(blockPairsCS);
                }
            }


            List<String> connectionsInGT = new ArrayList<>();
            for (BlockPair blockPair : blockPairsInCurrentPostVersionOfGT) {
                connectionsInGT.add(
                                blockPair.leftBlock.postBlock.getPostId() + ", " +
                                blockPair.leftBlock.postBlock.getPostHistoryId() + ", " +
                                blockPair.leftBlock.postBlock.getLocalId() + " -> " +
                                blockPair.rightBlock.postBlock.getLocalId()
                );
            }

            List<String> connectionsInCS = new ArrayList<>();
            for (BlockPair blockPair : blockPairsInCurrentPostVersionOfCS) {
                connectionsInCS.add(
                        blockPair.leftBlock.postBlock.getPostId() + ", " +
                                blockPair.leftBlock.postBlock.getPostHistoryId() + ", " +
                                blockPair.leftBlock.postBlock.getLocalId() + " -> " +
                                (blockPair.leftBlock.postBlock.getSucc() != null ? blockPair.leftBlock.postBlock.getSucc().getLocalId() : null)
                );
            }

            if (!connectionsInGT.equals(connectionsInCS)) {
                postVersionsWithDifferentLinks[i] = true;
            }
        }

        popUpWindowIfNoDifferencesAreFound();

        postVersionsWithDifferentLinks[0] = true; // to show at least the first two versions
    }

    private void popUpWindowIfNoDifferencesAreFound() {
        boolean differenceInConnectionsFound = false;
        for (boolean differenceInConnection : postVersionsWithDifferentLinks) {
            if (differenceInConnection) {
                differenceInConnectionsFound = true;
                break;
            }
        }

        if (!differenceInConnectionsFound) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information about the connections");
            alert.setHeaderText(null);
            alert.setContentText("Connections of Ground Truth and computed Similarity are equal!");

            alert.showAndWait();
        }
    }


    @FXML
    public void buttonBackClicked() {
        if (currentLeftVersionInViewedPost > 0) {
            int tmp = currentLeftVersionInViewedPost;
            tmp--;

            while (tmp > 0 && !postVersionsWithDifferentLinks[tmp]) {
                tmp--;
            }

            if (tmp >= 0) {
                currentLeftVersionInViewedPost = tmp;
                visualizeInGUI();
            }

        }
    }

    @FXML
    public void buttonNextClicked() {
        if (currentLeftVersionInViewedPost < currentPostVersionList.size() - 2) {
            int tmp = currentLeftVersionInViewedPost;
            tmp++;

            while(tmp < currentPostVersionList.size() -1 && !postVersionsWithDifferentLinks[tmp]) {
                tmp++;
            }

            if (tmp < currentPostVersionList.size() - 1) {
                currentLeftVersionInViewedPost = tmp;
                visualizeInGUI();
            }

        }
    }
}