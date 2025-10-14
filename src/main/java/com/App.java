package com;

import com.controllers.Controller;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * App
 * Grid (передаем координату, там внутри он смотрит очередь и обрабатывает клик как желает нужно по типу Event)
 * Map<Coordinate, GraphObject>
 * EventQueue
 * Panel (генерирует Event-ы в очередь событий)
 * List<Button> (вершина, дуга, текст)
 * EventQueue
 */
public class App extends Application {

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1024, 768);
        stage.setTitle("Редактор графа");
        stage.setScene(scene);
        stage.show();

        initGraph(fxmlLoader);
    }

    private void initGraph(FXMLLoader fxmlLoader) {
        if (fxmlLoader.getController() instanceof Controller) {
            ((Controller) fxmlLoader.getController()).initGraphView();
        } else {
            System.out.println("Exception!!! Controller can't init graph view.");
        }
    }

}