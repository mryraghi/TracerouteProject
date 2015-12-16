package main;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    TextField destination_ip;
    @FXML
    Label label_errbuf;
    @FXML
    Label label_status;
    @FXML
    Button button_stop;
    @FXML
    Button button_start;
    @FXML
    ListView<String> list_results;
    @FXML
    ProgressBar progress;
    @FXML
    LineChart chart;
    LineChart.Series<Double, Double> series1;
    private ObservableList<String> traceroute = FXCollections.observableArrayList(); // Dynamic list of nodes
    private Thread t;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set content to ListView from the beginning
        list_results.setItems(traceroute);

        ObservableList<XYChart.Series<Double, Double>> lineChartData = FXCollections.observableArrayList();

        series1 = new LineChart.Series<>();
        series1.setName("Hello Munsell");

        lineChartData.add(series1);

        chart.setData(lineChartData);
        chart.createSymbolsProperty();

    }

    public void beginTraceroute() throws IOException {
        traceroute.removeAll();
        list_results.getItems().clear();
        series1.getData().clear();
        error();

        status("Loading, this can take some time...");
        // Loader
        progress.setProgress(-1.0f);
        button_start.setDisable(true);
        button_stop.setDisable(false);
        try {
            ProcessBuilder pd = new ProcessBuilder().command("sudo", "python", "trace.py", destination_ip.getText());
            pd.redirectErrorStream(true);
            Process p = pd.start();

            Task task = new Task() {
                @Override
                protected Object call() throws Exception {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                        String line, message = null;

                        while ((line = in.readLine()) != null) {
                            if (!Objects.equals(line.substring(0, 1), "[")) {
                                if (Objects.equals(line.substring(0, 9), "Traceback")) {
                                    message = "Please insert a valid domain name";
                                } else if (Objects.equals(line.substring(0, 4), "sudo")) {
                                    message = "Please run this app with sudo privileges!";
                                } else {
                                    message = "Unknown error occurred!";
                                }
                                break;
                            } else {
                                traceroute.add(line);
                            }
                        }

                        final String finalMessage = message;
                        Platform.runLater(() -> {
                            stopTraceroute();
                            if (finalMessage != null) {
                                error(finalMessage);
                                series1.getData().clear();
                            } else {
                                double i = 5.0;
                                int ii = 1;
                                for (String s : traceroute) {
                                    double height = Math.random() * 100;
                                    final XYChart.Data<Double, Double> data = new XYChart.Data<>(i, height);
                                    String[] output = s.split("'", 5);
                                    data.setNode(new HoveredThresholdNode(output[3], i));
                                    series1.getData().add(data);
                                    i += 5;
                                    ii++;
                                }
                            }
                        });
                    } catch (IOException e) {
                        System.out.print(p.getErrorStream());
                    }
                    return null;
                }
            };
            t = new Thread(task);
            t.setDaemon(true);
            t.start();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void stopTraceroute() {
        status("Connection closed");
        progress.setProgress(0f);
        button_stop.setDisable(true);
        button_start.setDisable(false);
        t.interrupt();
    }

    private void print(String s) {
        System.out.println(s);
    }

    private void status(String s) {
        label_status.setText(s);
    }

    private void error(String s) {
        print(s);
        label_errbuf.setText(s);
    }

    private void error() {
        label_errbuf.setText("");
    }
}

class HoveredThresholdNode extends StackPane {
    HoveredThresholdNode(String s, double width) {
        setPrefSize(15, 15);

        final Label label = createDataThresholdLabel(s, width);
        System.out.println(label.getText());

        setOnMouseEntered(mouseEvent -> {
            getChildren().setAll(label);
            setCursor(Cursor.NONE);
            toFront();
        });
        setOnMouseExited(mouseEvent -> {
            getChildren().clear();
            setCursor(Cursor.CROSSHAIR);
        });
    }

    private Label createDataThresholdLabel(String s, double width) {
        final Label label = new Label(s);
        String styles = "-fx-font-size: 15; -fx-font-weight: bold; -fx-background-color: #fff; -fx-padding: 5px; -fx-border-radius: 3px;";
        if (width > 50) {
            label.setStyle(styles + " -fx-translate-x: -50px");
        } else {
            label.setStyle(styles + " -fx-translate-x: 30px");
        }

        label.setTextFill(Color.BLACK);

        label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        return label;
    }
}
