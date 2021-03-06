package sample;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import sample.utils.Device;
import sample.utils.ElementBound;
import sample.utils.Engine;
import sample.utils.ShellUtils;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

public class Controller implements Initializable {
    private static Random random;
    private static Logger log;
    private static ObservableList<String> devices;
    public static String selected_serial;
    private static boolean if_get_screencap;
    private Engine engine;
    private Node rootNode;
    private TreeItem<Node> selectTreeItem;
    private ArrayList<TreeItem<Node>> searchedTreeItems;
    private Image originalImage;
    private ArrayList<sample.utils.Element> touchableElements;
    private Scene messageWindow;
    private int globalIndex;

    @FXML
    ComboBox<String> combobox_devices;
    @FXML
    TreeView<Node> treeView;
    @FXML
    Button button_search;
    @FXML
    Button button_test;
    @FXML
    Button button_sync;
    @FXML
    Button button_refresh;
    @FXML
    TextField textfield_pattern;
    @FXML
    TextField textfield_fullpath;
    @FXML
    ImageView imageView;
    @FXML
    Label label_coordination;
    @FXML
    TableColumn<TableItem, String> col_key;
    @FXML
    TableColumn<TableItem, String> col_value;
    @FXML
    TableView<TableItem> tableView;
    @FXML
    AnchorPane rootAnchorPane;
    @FXML
    ProgressIndicator progressIndicator;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            random = new Random();
            log = sample.utils.Logger.getLogger();
            log.info("Initializing...");
        } catch (IOException e) {
            e.printStackTrace();
        }

        combobox_devices.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            log.info("??????????????????" + newValue);
            selected_serial = newValue;
            try {
                int port = random.nextInt(1000);
                engine = new Engine("127.0.0.1", 53001 + port, selected_serial);
                Engine.engine = engine;
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // ???????????????
            Node node = newValue.getValue();
            textfield_fullpath.setText(node.fullpath);
            Clipboard clipboard = Clipboard.getSystemClipboard();
            HashMap<DataFormat, Object> map = new HashMap<>();
            map.put(DataFormat.PLAIN_TEXT, node.fullpath);
            clipboard.setContent(map);

            // ????????????
            sample.utils.Element e = null;
            ElementBound eb = null;
            try {
                e = new sample.utils.Element(node.name, node.id);
                eb = e.getElementBound();
                assert eb != null;
                signImageWithRectangle(originalImage, eb);
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            // ????????????
            ObservableList<TableItem> data = FXCollections.observableArrayList();
            data.add(new TableItem("name", node.name));
            data.add(new TableItem("id", String.valueOf(node.id)));
            data.add(new TableItem("components", node.attrsInfo()));
            try {
                assert eb != null;
                data.add(new TableItem("location", eb.getLocationInfo()));
                data.add(new TableItem("bounds", eb.getBounds()));
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            col_key.setCellValueFactory(new PropertyValueFactory<>("key"));
            col_value.setCellValueFactory(new PropertyValueFactory<>("value"));
            tableView.setItems(data);
        });

        imageView.setOnMouseMoved(e -> {
            if (originalImage == null) return;
            int x = (int) e.getX();
            int y = (int) e.getY();

//            log.info("y="+y+","+e.getSceneY()+","+e.getScreenY());

            double imageWidth = originalImage.getWidth();
            double imageHeight = originalImage.getHeight();

            x = (int) (x * (imageWidth / imageView.getFitWidth()));
            y = (int) (y * (imageHeight / imageView.getFitHeight()));

            StringBuilder sb = new StringBuilder();
            label_coordination.setText(sb.append(x).append(",  ").append(y).toString());
        });

        imageView.setOnMouseClicked(e -> {
            int x = (int) e.getX();
            int y = (int) e.getY();

            double imageWidth = originalImage.getWidth();
            double imageHeight = originalImage.getHeight();

            x = (int) (x * (imageWidth / imageView.getFitWidth()));
            y = (int) (y * (imageHeight / imageView.getFitHeight()));

            log.info("?????????:(" + x + "," + y + ")");
            List<ElementBound> nowEBS = new ArrayList<ElementBound>();
            List<sample.utils.Element> nowEB = new ArrayList<sample.utils.Element>();
            for (sample.utils.Element element : touchableElements) {
                ElementBound eb = null;
                try {
                    eb = element.getElementBound();
                } catch (Exception exception) {
                    exception.printStackTrace();
                    continue;
                }
                if (eb.ifCoordinationInBound(x, y)) {
                    nowEB.add(element);
                    nowEBS.add(eb);
                }
            }

            globalIndex = globalIndex + 1 >= nowEB.size() ? 0 : globalIndex + 1;
            signImageWithRectangle(originalImage, nowEBS.get(globalIndex));
            boolean ret = focusOnElement(treeView.getRoot(), nowEB.get(globalIndex));
        });

        textfield_pattern.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                searchNodeHandler();
            }
        });

        try {
            refreshDevices();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean focusOnElement(TreeItem<Node> treeItem, sample.utils.Element element) {
        if (treeItem.getValue().fullpath.equals(element.objectName)) {
            treeView.getSelectionModel().select(treeItem);
            int index = treeView.getSelectionModel().getSelectedIndex();
            treeView.scrollTo(index);
            return true;
        }
        ObservableList<TreeItem<Node>> children = treeItem.getChildren();
        for (TreeItem<Node> child : children) {
            boolean ret = focusOnElement(child, element);
            if (ret) {
                return true;
            }
        }
        return false;
    }

    void signImageWithRectangle(Image image, ElementBound eb) {
        if (image == null || eb == null) {
            log.warning("image or elementbound is null");
            return;
        }
        signImageWithRectangle(image, (int) (eb.x + eb.width / 2), (int) (eb.y + eb.height / 2), (int) eb.width, (int) eb.height);
    }

    void signImageWithRectangle(Image image, int x, int y, int recWidth, int recHeight) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        x = x - recWidth / 2;
        y = y - recHeight / 2;

        WritableImage newImage = new WritableImage(width, height);
        PixelReader pixelReader = image.getPixelReader();
        PixelWriter pixelWriter = newImage.getPixelWriter();

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if (((i == x || i == x + recWidth) && (j >= y && j <= y + recHeight)) || ((j == y || j == y + recHeight) && (i >= x && i <= x + recWidth))
                        || ((i == x + 1 || i == x + recWidth - 1) && (j >= y + 1 && j <= y + recHeight - 1)) || ((j == y + 1 || j == y - 1 + recHeight) && (i >= x + 1 && i <= x - 1 + recWidth))
                        || ((i == x + 2 || i == x + recWidth - 2) && (j >= y + 2 && j <= y + recHeight - 2)) || ((j == y + 2 || j == y - 2 + recHeight) && (i >= x + 2 && i <= x - 2 + recWidth))) {
                    pixelWriter.setColor(i, j, Color.RED);
                    continue;
                }
                Color color = pixelReader.getColor(i, j);
                pixelWriter.setColor(i, j, color);
            }
        }

        imageView.setImage(newImage);
    }

    /// ??????????????????
    @FXML
    void eventRefreshDevices(ActionEvent actionEvent) throws IOException {
        refreshDevices();
    }

    void refreshDevices() throws IOException {
        rootAnchorPane.setMouseTransparent(true);
        String cmd = "devices -l";
        String ret = ShellUtils.execAdb(cmd, "");
        log.info(ret);

        devices = FXCollections.observableArrayList();
        String[] lines = ret.split("\n");
        for (String line : lines) {
            if (line.contains("device product") && !line.contains("offline")) {
                System.out.println(line);
                String device_serial = line.split(" ", -1)[0];
                devices.add(device_serial);
            }
        }
        if (devices.size() == 0) {
            MessageWindow.display("?????????????????????????????????\n???????????????adb");
            rootAnchorPane.setMouseTransparent(false);
            return;
        }
        combobox_devices.setItems(devices);
        combobox_devices.getSelectionModel().select(0);
        selected_serial = combobox_devices.getSelectionModel().getSelectedItem();
        rootAnchorPane.setMouseTransparent(false);
    }

    /// ?????????????????????????????????????????????
    @FXML
    void syncGameState(ActionEvent actionEvent) throws Exception {
        rootAnchorPane.setMouseTransparent(true);
        progressIndicator.setProgress(0);
        progressIndicator.setVisible(true);
        if_get_screencap = false;

        new Thread(new Runnable() {
            @Override
            public void run() {
                String xmlStr;
                try {
                    xmlStr = engine.getDumpTree();
                    updateProgress(0.3);

//                    log.info("xmlStr = \n" + xmlStr);
                    xmlStr = xmlStr.replace("&", "_");

                    // ???????????????
                    rootNode = new Node();
                    Document document = DocumentHelper.parseText(xmlStr);
                    Element root = document.getRootElement();
                    rootNode.name = root.getName();
                    rootNode.fullpath = "";
                    log.info("root element: " + root.getName());
                    traverseXML(root, rootNode);

                    updateProgress(0.5);

                    buildTreeView();

                    Device.screenshot(selected_serial);
                    Image image = new Image("file:///D:screenshot.png");
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {

                            // ????????????
                            log.info("????????????...");
                            try {
                                imageView.setImage(image);
                                imageView.setPreserveRatio(true);
                                imageView.setFitHeight(image.getHeight() * (imageView.getFitWidth() / image.getWidth()));
                                originalImage = image;
                                if_get_screencap = true;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    updateProgress(0.6);

                    // ??????????????????????????????????????????
                    touchableElements = new ArrayList<>();
                    ArrayList<sample.utils.Element> elements = engine.getTouchableElements();
                    for (sample.utils.Element e : elements) {
                        engine.getElementBound(e);
                        touchableElements.add(e);
                    }
                    updateProgress(1.0);
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            MessageWindow.display("????????????????????????\n?????????????????????????????????\n????????????????????????\nGA sdk??????????????????");
                            if (!if_get_screencap) {
                                try {
                                    Device.screenshot(selected_serial);
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                                Image image = new Image("file:///D:screenshot.png");
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        // ????????????
                                        log.info("????????????...");
                                        try {
                                            imageView.setImage(image);
                                            imageView.setPreserveRatio(true);
                                            imageView.setFitHeight(image.getHeight() * (imageView.getFitWidth() / image.getWidth()));
                                            originalImage = image;
                                            if_get_screencap = true;
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        progressIndicator.setVisible(false);
                                    }
                                });
                            }
                            progressIndicator.setVisible(false);
                        }
                    });
                }
            }
        }).start();
        rootAnchorPane.setMouseTransparent(false);
    }

    void updateProgress(double progress) {
        log.info("????????????" + progress);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (progress >= 1.0) {
                    progressIndicator.setVisible(false);
                    return;
                }
                progressIndicator.setProgress(progress);

            }
        });
    }

    void traverseXML(Element root, Node node) {
        Iterator<Element> iterator = root.elementIterator();
        if (iterator.hasNext()) node.children = new ArrayList<Node>();
        while (iterator.hasNext()) {
            Node childNode = new Node();
            childNode.attrs = new HashMap<String, String>();

            Element child = iterator.next();
            List<Attribute> attrs = child.attributes();
            for (Attribute attr : attrs) {
                String name = attr.getName();
                if (attr.getName().equals("name")) childNode.name = attr.getValue();
                else if (attr.getName().equals("id")) {
                    childNode.id = Integer.parseInt(attr.getValue());
                } else {
                    String value = attr.getValue();
                    value = attr.getStringValue();
                    childNode.attrs.put(attr.getName(), attr.getValue());
                }
            }
            childNode.fullpath = node.fullpath + "/" + childNode.name;
            node.children.add(childNode);
            traverseXML(child, childNode);
        }
    }

    void buildTreeView() {
        TreeItem<Node> rootItem = new TreeItem<>();
        rootItem.setValue(rootNode);
        buildTreeItem(rootItem, rootNode);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                treeView.setRoot(rootItem);
            }
        });

    }

    void buildTreeItem(TreeItem<Node> treeItem, Node node) {
        if (node.children == null) return;
        for (Node childNode : node.children) {
            TreeItem<Node> childTreeItem = new TreeItem<Node>(childNode);
            buildTreeItem(childTreeItem, childNode);
            treeItem.getChildren().add(childTreeItem);
        }
    }

    @FXML
    public void searchNode(ActionEvent actionEvent) {
        searchNodeHandler();
    }

    public void searchNodeHandler() {
        TreeItem<Node> treeItem = treeView.getRoot();
        String pattern = textfield_pattern.getText();
        searchedTreeItems = new ArrayList<>();

        searchTreeItem(treeItem, pattern.toLowerCase());
        if (searchedTreeItems.size() > 0) treeView.getSelectionModel().select(searchedTreeItems.get(0));
    }

    void searchTreeItem(TreeItem<Node> treeItem, String pattern) {
        Node node = treeItem.getValue();
        if (node.name.toLowerCase().contains(pattern) || node.attrsInfo().toLowerCase().contains(pattern)) {
            searchedTreeItems.add(treeItem);
            if (!node.name.toLowerCase().contains(pattern)) {
                log.info("?????????name????????????????????????" + node.attrsInfo());
            }
        }
        ObservableList<TreeItem<Node>> children = treeItem.getChildren();
        for (TreeItem<Node> child : children) {
            searchTreeItem(child, pattern);
        }
    }

    @FXML
    void onButtonTestClick(ActionEvent actionEvent) throws Exception {
        sample.utils.Element e = engine.findElement("/UIRoot/UIHang/LoginWindow(Clone)/Center/AccountGroup.GO/Account.InputField");
        ElementBound eb = engine.getElementBound(e);
        log.info(eb.toString());
        int y = 4;
        int height = 5;
        float ret = y / (float) height;
        log.info(String.valueOf(ret));
    }
}
