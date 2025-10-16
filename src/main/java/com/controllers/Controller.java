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
import java.util.stream.Collectors;

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
    if (selected.size() == 2 && selected.stream().allMatch(item -> item instanceof SmartGraphVertex)) {
      showTextInputDialog(
        "Создание связи",
        "Введите вес связи (число от -1 до 1)",
        value -> {
          try {
            double weight = Double.parseDouble(value.trim());
            if (weight < -1.0 || weight > 1.0) {
              showAlert("Вес должен быть в диапазоне от -1 до 1!");
              return;
            }

            graphView.getModel().insertEdge(
              ((SmartGraphVertex<String>) selected.get(0)).getUnderlyingVertex(),
              ((SmartGraphVertex<String>) selected.get(1)).getUnderlyingVertex(),
              weight
            );

            new ArrayList<>(selected).forEach(this::select);
            graphView.update();
          } catch (NumberFormatException e) {
            showAlert("Некорректное число!");
          }
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
        graphView.getModel().removeEdge(((SmartGraphEdge<Double, String>) item).getUnderlyingEdge());
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

  private Graph<String, Double> getDefaultGraph() {
    Digraph<String, Double> d = new DigraphEdgeList<>();
    Vertex<String> vA = d.insertVertex("A");
    Vertex<String> vB = d.insertVertex("B");
    Vertex<String> vC = d.insertVertex("C");
    Vertex<String> vD = d.insertVertex("D");

    d.insertEdge(vA, vB, 0.5);
    d.insertEdge(vB, vC, -0.3);
    d.insertEdge(vC, vD, 0.7);
    d.insertEdge(vD, vA, -0.2);
    d.insertEdge(vA, vC, 0.4);

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

    Graph<String, Double> graph = graphView.getModel();

    StringBuilder report = new StringBuilder();
    report.append("=== Структурный анализ графа ===\n\n");

    int vertexCount = graph.vertices().size();
    int edgeCount = graph.edges().size();
    report.append("Вершин: ").append(vertexCount).append("\n");
    report.append("Рёбер: ").append(edgeCount).append("\n\n");

    if (!(graph instanceof Digraph)) {
      report.append("Анализ циклов поддерживается только для ориентированных графов.\n");
      structuralAnalysisText.setText(report.toString());
      return;
    }

    Digraph<String, Double> digraph = (Digraph<String, Double>) graph;

    // Найти все циклы
    List<List<Vertex<String>>> cycles = findAllCycles(digraph);
    report.append("Найдено циклов: ").append(cycles.size()).append("\n");

    int negativeCycles = 0;
    for (int i = 0; i < cycles.size(); i++) {
      List<Vertex<String>> cycle = cycles.get(i);
      boolean isNegative = isNegativeCycle(digraph, cycle);
      if (isNegative) negativeCycles++;

      report.append("\nЦикл ").append(i + 1).append(": ");
      report.append(cycle.stream()
        .limit(cycle.size() - 1) // последний элемент — дубль первого
        .map(Vertex::element)
        .collect(Collectors.joining(" → ")));
      report.append(" (").append(isNegative ? "отрицательный" : "положительный").append(")");
    }

    report.append("\n\nОтрицательных циклов: ").append(negativeCycles).append("\n");
    report.append("Анализ завершён.\n");

    structuralAnalysisText.setText(report.toString());
  }

  private List<List<Vertex<String>>> findAllCycles(Digraph<String, Double> digraph) {
    List<List<Vertex<String>>> allCycles = new ArrayList<>();
    List<Vertex<String>> vertices = new ArrayList<>(digraph.vertices());

    for (Vertex<String> start : vertices) {
      List<Vertex<String>> path = new ArrayList<>();
      Set<Vertex<String>> pathSet = new HashSet<>(); // для быстрой проверки
      findCyclesFrom(digraph, start, start, path, pathSet, allCycles);
    }

    // Уберём дубликаты (например, A→B→C→A и B→C→A→B)
    return removeDuplicateCycles(allCycles);
  }

  private void findCyclesFrom(
    Digraph<String, Double> digraph,
    Vertex<String> start,
    Vertex<String> current,
    List<Vertex<String>> path,
    Set<Vertex<String>> pathSet,
    List<List<Vertex<String>>> allCycles
  ) {
    path.add(current);
    pathSet.add(current);

    try {
      for (Edge<Double, String> edge : digraph.outboundEdges(current)) {
        Vertex<String> neighbor = digraph.opposite(current, edge);

        if (neighbor.equals(start) && path.size() > 1) {
          // Нашли цикл
          List<Vertex<String>> cycle = new ArrayList<>(path);
          cycle.add(start); // замыкаем
          allCycles.add(cycle);
        } else if (!pathSet.contains(neighbor)) {
          // Продолжаем DFS
          findCyclesFrom(digraph, start, neighbor, path, pathSet, allCycles);
        }
        // Если neighbor уже в path — игнорируем (избегаем не простых циклов)
      }
    } catch (InvalidVertexException e) {
      e.printStackTrace();
    }

    // Backtrack
    path.remove(path.size() - 1);
    pathSet.remove(current);
  }

  private List<List<Vertex<String>>> removeDuplicateCycles(List<List<Vertex<String>>> cycles) {
    Set<List<Vertex<String>>> uniqueCycles = new HashSet<>();

    for (List<Vertex<String>> cycle : cycles) {
      // Убираем последний элемент (дубль первого)
      List<Vertex<String>> simpleCycle = new ArrayList<>(cycle.subList(0, cycle.size() - 1));

      // Нормализуем: сдвигаем, чтобы начинать с минимальной вершины (по имени)
      int minIndex = 0;
      String minName = simpleCycle.get(0).element();
      for (int i = 1; i < simpleCycle.size(); i++) {
        String name = simpleCycle.get(i).element();
        if (name.compareTo(minName) < 0) {
          minName = name;
          minIndex = i;
        }
      }

      // Создаём нормализованный цикл
      List<Vertex<String>> normalized = new ArrayList<>();
      for (int i = 0; i < simpleCycle.size(); i++) {
        normalized.add(simpleCycle.get((minIndex + i) % simpleCycle.size()));
      }

      uniqueCycles.add(normalized);
    }

    // Возвращаем в исходном виде (с замыканием)
    List<List<Vertex<String>>> result = new ArrayList<>();
    for (List<Vertex<String>> cycle : uniqueCycles) {
      List<Vertex<String>> fullCycle = new ArrayList<>(cycle);
      fullCycle.add(cycle.get(0)); // замыкаем
      result.add(fullCycle);
    }

    return result;
  }

  private boolean isNegativeCycle(Digraph<String, Double> digraph, List<Vertex<String>> cycle) {
    double product = 1.0;
    try {
      for (int i = 0; i < cycle.size() - 1; i++) {
        Vertex<String> from = cycle.get(i);
        Vertex<String> to = cycle.get(i + 1);
        Edge<Double, String> edge = findEdgeBetween(digraph, from, to);
        if (edge != null) {
          product *= edge.element();
        }
      }
    } catch (InvalidVertexException e) {
      e.printStackTrace();
    }
    return product < 0;
  }

  private Edge<Double, String> findEdgeBetween(Digraph<String, Double> digraph, Vertex<String> u, Vertex<String> v) {
    try {
      for (Edge<Double, String> edge : digraph.outboundEdges(u)) {
        if (digraph.opposite(u, edge).equals(v)) {
          return edge;
        }
      }
    } catch (InvalidVertexException e) {
      e.printStackTrace();
    }
    return null;
  }
}