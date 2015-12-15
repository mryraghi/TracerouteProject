package main;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
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

    private ObservableList<String> traceroute = FXCollections.observableArrayList(); // Dynamic list of nodes
    private Thread t;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set content to ListView from the beginning
        list_results.setItems(traceroute);

    }

    public void beginTraceroute() throws IOException {
        traceroute.removeAll();
        status("Loading, this can take some time...");
        // Loader
        progress.setProgress(-1.0f);
        button_start.setDisable(true);
        button_stop.setDisable(false);
        try {
            ProcessBuilder pd = new ProcessBuilder().command("sudo", "python", "trace.py", destination_ip.getText());
            Process p = pd.start();

            Task task = new Task() {
                @Override
                protected Object call() throws Exception {
                    try {
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                            String line;

                            while ((line = in.readLine()) != null) {
                                traceroute.add(line);
                            }

                            Platform.runLater(() -> {
                                try {
                                    stopTraceroute();
                                } catch (UnknownHostException | SocketException e) {
                                    e.printStackTrace();
                                }
                            });

                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
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

    public void stopTraceroute() throws UnknownHostException, SocketException {
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
        label_errbuf.setText(s);
    }

    private void error() {
        label_errbuf.setText("");
    }
}
