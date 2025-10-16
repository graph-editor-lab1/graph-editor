package com.controllers;

import com.brunomnsilva.smartgraph.graph.*;
import com.brunomnsilva.smartgraph.graphview.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

public class Controller implements Initializable {

  @FXML
  private HBox graphBox;

  private SmartGraphPanel<String, Double> graphView;

  private final List<SmartStylableNode> selected = new ArrayList<>();

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    createGraphView();
  }

  @FXML
  private void onAddVertex() {
    showTextInputDialog(
      "Создание новой вершины",
      "Введите название новой вершины",
      name -> {
        try {
          graphView.getModel().insertVertex(name);
        } catch (InvalidVertexException _ignored) {
          showAlert("Вершина с данным именем уже существует!");
        }

        new ArrayList<>(selected).forEach(this::select);
        graphView.update();
      }
    );
  }

  @FXML
  private void onEditSelected() {
    if (selected.size() == 1) {
      showTextInputDialog(
        "Изменение",
        "Введите новое значение",
        value -> {
          // Library bug with replace
//                          if (item instanceof SmartGraphVertex) {
//                              Vertex<String> vertex = ((SmartGraphVertex<String>) item).getUnderlyingVertex();
//                              graphView.getModel().replace(vertex, value);
//                          } else if (item instanceof SmartGraphEdge) {
//                              Edge<String, String> edge = ((SmartGraphEdge<String, String>) item).getUnderlyingEdge();
//                              graphView.getModel().replace(edge, value);
//                          }

//                          new ArrayList<>(selected).forEach(this::select);
//                          graphView.update();
        }
      );
    } else {
      showAlert("Выберите ровно один элемент для редактирования!");
    }
  }

  @FXML
  private void onConnectSelected() {
    if (selected.size() == 2
      && selected.stream().allMatch(item -> item instanceof SmartGraphVertex)
    ) {
      showTextInputDialog(
        "Создание связи",
        "Введите вес новой связи",
        value -> {
          //noinspection unchecked
          graphView.getModel().insertEdge(
            ((SmartGraphVertex<String>) selected.get(0)).getUnderlyingVertex(),
            ((SmartGraphVertex<String>) selected.get(1)).getUnderlyingVertex(),
            String.valueOf(Double.parseDouble(value))
          );

          new ArrayList<>(selected).forEach(this::select);
          graphView.update();
        }
      );
    } else {
      showAlert("Выберите ровно две вершины для соединения!");
    }
  }

  @FXML
  @SuppressWarnings("unchecked")
  private void onRemoveSelected() {
    selected.forEach(item -> {
      if (item instanceof SmartGraphVertex) {
        graphView.getModel().removeVertex(((SmartGraphVertex<String>) item).getUnderlyingVertex());
      } else if (item instanceof SmartGraphEdge) {
        graphView.getModel().removeEdge(((SmartGraphEdge<String, String>) item).getUnderlyingEdge());
      }
    });

    new ArrayList<>(selected).forEach(this::select);
    graphView.update();
  }

  public void initGraphView() {
    if (graphView != null) {
      graphView.init();
    } else {
      System.out.println("Exception!!! Scene has not yet been initialized.");
    }
  }

  private void createGraphView() {
    SmartPlacementStrategy strategy = new SmartCircularSortedPlacementStrategy();
    graphView = new SmartGraphPanel<>(getDefaultGraph(), strategy);

    graphView.setVertexDoubleClickAction(this::select);
    graphView.setEdgeDoubleClickAction(this::select);

    HBox.setHgrow(graphView, Priority.ALWAYS);
    graphBox.getChildren().add(graphView);
  }

  private Graph<String, String> getDefaultGraph() {
    Digraph<String, String> d = new DigraphEdgeList<>();
    Vertex<String> vA = d.insertVertex("A");
    Vertex<String> vB = d.insertVertex("B");
    Vertex<String> vC = d.insertVertex("C");
    Vertex<String> vD = d.insertVertex("D");

    d.insertEdge(vA, vB, "AB");
    d.insertEdge(vB, vC, "BC");
    d.insertEdge(vC, vD, "CD");
    d.insertEdge(vA, vD, "AD");
    d.insertEdge(vA, vC, "AC");

    return d;
  }

  private void select(SmartStylableNode item) {
    if (selected.contains(item)) {
      selected.remove(item);
      item.setStyleClass(getDefaultStyleClass(item));
    } else {
      selected.add(item);
      item.setStyleClass(getDefaultStyleClass(item) + "-selected");
    }
  }

  private String getDefaultStyleClass(SmartStylableNode item) {
    if (item instanceof SmartGraphVertex) {
      return "vertex";
    } else if (item instanceof SmartGraphEdge) {
      return "edge";
    } else {
      return "";
    }
  }

  private void showTextInputDialog(
    String title,
    String header,
    Consumer<String> action
  ) {
    TextInputDialog dialog = new TextInputDialog("");
    dialog.setTitle(title);
    dialog.setHeaderText(header);
    dialog.setContentText("");

    Optional<String> result = dialog.showAndWait();
    result.ifPresent(action);
  }

  private void showAlert(String header) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Ошибка!");
    alert.setHeaderText(header);
    alert.setContentText("");
    alert.showAndWait();
  }


  @FXML
  private TextArea structuralAnalysisText;

  @FXML
  private void onRunStructuralAnalysis() {
    structuralAnalysisText.clear();

    if (graphView == null) {
      structuralAnalysisText.setText("Граф не инициализирован.");
      return;
    }

    Graph<String, String> graph = graphView.getModel();

    StringBuilder report = new StringBuilder();
    report.append("=== Структурный анализ графа ===\n\n");

    // 1. Количество вершин и рёбер
    int vertexCount = 0;
    for (Vertex<String> v : graph.vertices()) vertexCount++;
    int edgeCount = 0;
    for (Edge<String, String> e : graph.edges()) edgeCount++;

    report.append("Вершин: ").append(vertexCount).append("\n");
    report.append("Рёбер: ").append(edgeCount).append("\n\n");

    // 2. Степени вершин
    if (graph instanceof Digraph) {
      report.append("Степени вершин (входящие / исходящие):\n");
      for (Vertex<String> v : graph.vertices()) {
        int inDegree = 0;
        int outDegree = 0;

        // Подсчёт исходящих рёбер
        for (Edge<String, String> e : graph.outgoingEdges(v)) {
          outDegree++;
        }

        // Подсчёт входящих рёбер
        for (Edge<String, String> e : graph.incomingEdges(v)) {
          inDegree++;
        }

        report.append("  ").append(v.element()).append(": ")
          .append(inDegree).append(" / ").append(outDegree).append("\n");
      }
    } else {
      // Для неориентированного графа — просто считаем все инцидентные рёбра
      report.append("Степени вершин:\n");
      for (Vertex<String> v : graph.vertices()) {
        int degree = 0;
        for (Edge<String, String> e : graph.incidentEdges(v)) {
          degree++;
        }
        report.append("  ").append(v.element()).append(": ").append(degree).append("\n");
      }
    }
    report.append("\n");

    report.append("Анализ завершён.\n");

    structuralAnalysisText.setText(report.toString());
  }
}