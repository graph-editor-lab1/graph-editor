package com.controllers;

import com.EdgeWeight;
import com.brunomnsilva.smartgraph.graph.*;
import com.brunomnsilva.smartgraph.graphview.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Controller implements Initializable {

  @FXML
  private HBox graphBox;
  private SmartGraphPanel<String, EdgeWeight> graphView;
  private final List<SmartStylableNode> selected = new ArrayList<>();

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    createGraphView();
    setupImpulseChart();
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
  @SuppressWarnings("unchecked")
  private void onEditSelected() {
    if (selected.size() == 1) {
      SmartStylableNode item = selected.get(0);
      showTextInputDialog(
          "Изменение",
          "Введите новое значение",
          value -> {
            if (item instanceof SmartGraphVertex) {
              Vertex<String> vertex = ((SmartGraphVertex<String>) item).getUnderlyingVertex();
              Vertex<String> newVertex = graphView.getModel().insertVertex(value);
              graphView.updateAndWait();
              SmartGraphVertex<String> newItem = (SmartGraphVertex<String>) graphView.getStylableVertex(newVertex);
              newItem.setPosition(
                  ((SmartGraphVertex<String>) item).getPositionCenterX(),
                  ((SmartGraphVertex<String>) item).getPositionCenterY()
              );

              graphView
                  .getModel()
                  .edges()
                  .stream()
                  .filter(e -> Arrays.asList(e.vertices()).contains(vertex))
                  .collect(Collectors.toList())
                  .forEach(edge -> {
                    if (vertex.equals(edge.vertices()[0])) {
                      Edge<EdgeWeight, String> newEdge = graphView
                          .getModel()
                          .insertEdge(
                              newVertex,
                              edge.vertices()[1],
                              new EdgeWeight(edge.element().getValue())
                          );
                      setEdgeStyle(newEdge);
                      graphView.getModel().removeEdge(edge);
                    } else if (vertex.equals(edge.vertices()[1])) {
                      Edge<EdgeWeight, String> newEdge = graphView
                          .getModel()
                          .insertEdge(
                              edge.vertices()[0],
                              newVertex,
                              new EdgeWeight(edge.element().getValue())
                          );
                      setEdgeStyle(newEdge);
                      graphView.getModel().removeEdge(edge);
                    }
                  });
              graphView.getModel().removeVertex(vertex);
            } else if (item instanceof SmartGraphEdge) {
              Edge<EdgeWeight, String> edge = ((SmartGraphEdge<EdgeWeight, String>) item).getUnderlyingEdge();

              Edge<EdgeWeight, String> newEdge = graphView
                  .getModel()
                  .insertEdge(edge.vertices()[0], edge.vertices()[1], new EdgeWeight(Double.parseDouble(value.trim())));

              setEdgeStyle(newEdge);
              graphView.getModel().removeEdge(edge);
            }

            new ArrayList<>(selected).forEach(this::select);
            graphView.update();
          }
      );
    } else {
      showAlert("Выберите ровно один элемент для редактирования!");
    }
  }

  @FXML
  @SuppressWarnings("unchecked")
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

              Edge<EdgeWeight, String> edge = graphView.getModel().insertEdge(
                  ((SmartGraphVertex<String>) selected.get(0)).getUnderlyingVertex(),
                  ((SmartGraphVertex<String>) selected.get(1)).getUnderlyingVertex(),
                  new EdgeWeight(weight)
              );

              setEdgeStyle(edge);
              new ArrayList<>(selected).forEach(this::select);
            } catch (NumberFormatException e) {
              showAlert("Некорректное число!");
            } catch (InvalidVertexException | InvalidEdgeException e) {
              showAlert("Не удалось создать ребро: " + e.getMessage());
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
      if (item instanceof SmartGraphVertex
          && graphView.getModel().vertices().contains(((SmartGraphVertex<String>) item).getUnderlyingVertex())
      ) {
        graphView.getModel().removeVertex(((SmartGraphVertex<String>) item).getUnderlyingVertex());
      } else if (item instanceof SmartGraphEdge
          && graphView.getModel().edges().contains(((SmartGraphEdge<EdgeWeight, String>) item).getUnderlyingEdge())
      ) {
        graphView.getModel().removeEdge(((SmartGraphEdge<EdgeWeight, String>) item).getUnderlyingEdge());
      }
    });

    new ArrayList<>(selected).forEach(this::select);
    graphView.update();
  }

  @SuppressWarnings("unchecked")
  private void setEdgeStyle(Edge<EdgeWeight, String> edge) {
    graphView.updateAndWait();
    SmartGraphEdge<EdgeWeight, String> smartEdge = (SmartGraphEdge<EdgeWeight, String>) graphView
        .getStylableEdge(edge);
    smartEdge.setStyleClass(edge.element().getValue() < 0 ? "edge-dashed" : "edge");
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

    graphView
        .getSmartEdges()
        .stream()
        .filter(e -> e.getUnderlyingEdge().element().getValue() < 0)
        .forEach(e -> e.setStyleClass("edge-dashed"));

    HBox.setHgrow(graphView, Priority.ALWAYS);
    graphBox.getChildren().add(graphView);
  }

  private Graph<String, EdgeWeight> getDefaultGraph() {
    Digraph<String, EdgeWeight> d = new DigraphEdgeList<>();

    // Создаём вершины
    Vertex<String>[] vertices = new Vertex[15];
    for (int i = 0; i < 15; i++) {
      vertices[i] = d.insertVertex(String.valueOf(i + 1));
    }

    // Рёбра из JSON (vertex1 → vertex2, weight)
    double[][] edges = {
        {0, 1, 0.6},
        {10, 0, 0.7},
        {13, 1, 0.8},
        {7, 1, -0.8},
        {5, 1, 0.3},
        {1, 12, 0.7},
        {1, 8, 0.9},
        {6, 1, 0.7},
        {3, 1, 0.4},
        {1, 4, 0.7},
        {4, 8, 0.5},
        {14, 4, 0.8},
        {14, 13, 0.8},
        {13, 9, 0.5},
        {13, 5, 0.7},
        {13, 10, -0.8},
        {2, 6, 0.6},
        {2, 8, 0.7},
        {14, 2, 0.7},
        {14, 3, 0.9},
        {5, 9, 0.5},
        {6, 9, 0.4},
        {7, 8, 0.9},
        {10, 7, -0.7},
        {8, 14, 0.9},
        {8, 9, 0.5},
        {11, 9, 0.6},
        {9, 12, -0.6},
        {11, 12, -0.7},
        {14, 11, 0.8},
        {12, 8, -0.6}
    };

    for (double[] edge : edges) {
      int from = (int) edge[0];
      int to = (int) edge[1];
      double weight = edge[2];
      try {
        d.insertEdge(vertices[from], vertices[to], new EdgeWeight(weight));
      } catch (InvalidVertexException | InvalidEdgeException e) {
        System.err.println("Ошибка при добавлении ребра: " + e.getMessage());
      }
    }

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

  @SuppressWarnings("unchecked")
  private String getDefaultStyleClass(SmartStylableNode item) {
    if (item instanceof SmartGraphVertex) {
      return "vertex";
    } else if (item instanceof SmartGraphEdge) {
      return ((SmartGraphEdge<EdgeWeight, String>) item).getUnderlyingEdge().element().getValue() < 0
          ? "edge-dashed"
          : "edge";
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

    Graph<String, EdgeWeight> graph = graphView.getModel();

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

    Digraph<String, EdgeWeight> digraph = (Digraph<String, EdgeWeight>) graph;

    List<List<Vertex<String>>> cycles = findAllCycles(digraph);
    report.append("Найдено циклов: ").append(cycles.size()).append("\n");

    int negativeCycles = 0;
    for (int i = 0; i < cycles.size(); i++) {
      List<Vertex<String>> cycle = cycles.get(i);
      boolean isNegative = isNegativeCycle(digraph, cycle);
      if (isNegative) negativeCycles++;

      report.append("\nЦикл ").append(i + 1).append(": ");
      report.append(cycle.stream()
        .map(Vertex::element)
        .collect(Collectors.joining(" → ")));
      report.append(" (").append(isNegative ? "отрицательный" : "положительный").append(")");
    }

    report.append("\n\nОтрицательных циклов: ").append(negativeCycles).append("\n");

    report.append("\nСтруктурная устойчивость: ");
    if (structurallyStableModel(negativeCycles)) {
      report.append("Да");
    } else {
      report.append("Нет");
    }
    report.append("\n");

    report.append("Анализ завершён.\n");
    structuralAnalysisText.setText(report.toString());
  }

  private List<List<Vertex<String>>> findAllCycles(Digraph<String, EdgeWeight> digraph) {
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
    Digraph<String, EdgeWeight> digraph,
    Vertex<String> start,
    Vertex<String> current,
    List<Vertex<String>> path,
    Set<Vertex<String>> pathSet,
    List<List<Vertex<String>>> allCycles
  ) {
    path.add(current);
    pathSet.add(current);

    try {
      for (Edge<EdgeWeight, String> edge : digraph.outboundEdges(current)) {
        Vertex<String> neighbor = digraph.opposite(current, edge);
        if (neighbor.equals(start) && path.size() > 1) {
          List<Vertex<String>> cycle = new ArrayList<>(path);
          cycle.add(start);
          allCycles.add(cycle);
        } else if (!pathSet.contains(neighbor)) {
          findCyclesFrom(digraph, start, neighbor, path, pathSet, allCycles);
        }
      }
    } catch (InvalidVertexException e) {
      e.printStackTrace();
    }

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

  private boolean isNegativeCycle(Digraph<String, EdgeWeight> digraph, List<Vertex<String>> cycle) {
    double product = 1.0;
    try {
      for (int i = 0; i < cycle.size() - 1; i++) {
        Vertex<String> from = cycle.get(i);
        Vertex<String> to = cycle.get(i + 1);
        Edge<EdgeWeight, String> edge = findEdgeBetween(digraph, from, to);
        if (edge != null) {
          product *= edge.element().getValue();
        }
      }
    } catch (InvalidVertexException e) {
      e.printStackTrace();
    }
    return product < 0;
  }

  private Edge<EdgeWeight, String> findEdgeBetween(Digraph<String, EdgeWeight> digraph, Vertex<String> u, Vertex<String> v) {
    try {
      for (Edge<EdgeWeight, String> edge : digraph.outboundEdges(u)) {
        if (digraph.opposite(u, edge).equals(v)) {
          return edge;
        }
      }
    } catch (InvalidVertexException e) {
      e.printStackTrace();
    }
    return null;
  }

  private boolean structurallyStableModel(int numberOfNegativeCycles) {
    return numberOfNegativeCycles % 2 != 0;
  }



  @FXML
  private TextField impulseVectorField;

  @FXML
  private void onRunImpulseSimulation() {
    String input = impulseVectorField.getText().trim();
    if (input.isEmpty()) {
      showAlert("Введите вектор импульса!");
      return;
    }

    try {
      String[] parts = input.split(",");
      double[] vector = new double[parts.length];
      for (int i = 0; i < parts.length; i++) {
        vector[i] = Double.parseDouble(parts[i].trim());
      }

      if (graphView == null || !(graphView.getModel() instanceof Digraph)) {
        showAlert("Граф не инициализирован.");
        return;
      }

      Digraph<String, EdgeWeight> graph = (Digraph<String, EdgeWeight>) graphView.getModel();
      List<Vertex<String>> verticesInOrder = getVerticesInOrder(graph);

      if (vector.length != verticesInOrder.size()) {
        showAlert("Вектор должен содержать " + verticesInOrder.size() + " значений (по числу вершин)!");
        return;
      }

      int steps = 10;
      double[][] history = runImpulseSimulation(vector, steps);

      // Сохраняем для последующих обновлений
      this.lastSimulationResult = history;
      this.lastVerticesInOrder = verticesInOrder;
      this.lastSteps = steps;

      // Создаём/обновляем чекбоксы
      createVertexCheckBoxes(verticesInOrder);

      // Отображаем все вершины по умолчанию
      updateChart();

    } catch (NumberFormatException e) {
      showAlert("Некорректный формат вектора! Используйте числа, разделённые запятыми.");
    } catch (IllegalArgumentException e) {
      showAlert(e.getMessage());
    }
  }

  private List<Vertex<String>> getVerticesInOrder(Digraph<String, EdgeWeight> graph) {
    List<Vertex<String>> vertices = new ArrayList<>(graph.vertices());
    vertices.sort(Comparator.comparing(v -> Integer.parseInt(v.element())));
    return vertices;
  }

  private double[][] runImpulseSimulation(double[] impulseVector, int steps) {
    if (graphView == null || !(graphView.getModel() instanceof Digraph)) {
      return new double[0][0];
    }

    Digraph<String, EdgeWeight> graph = (Digraph<String, EdgeWeight>) graphView.getModel();
    List<Vertex<String>> verticesInOrder = getVerticesInOrder(graph);
    int n = verticesInOrder.size();

    if (impulseVector.length != n) {
      throw new IllegalArgumentException("Длина вектора должна быть " + n + " (по числу вершин).");
    }

    // Маппинг: имя вершины → индекс
    Map<String, Integer> nameToIndex = new HashMap<>();
    for (int i = 0; i < n; i++) {
      nameToIndex.put(verticesInOrder.get(i).element(), i);
    }

    // История состояний v_i(t)
    double[][] vHistory = new double[steps + 1][n];

    // История импульсов p_i(t)
    double[][] pHistory = new double[steps + 1][n];

    // Шаг 0: v(0) = p(0) = impulseVector
    for (int i = 0; i < n; i++) {
      pHistory[0][i] = impulseVector[i];
      vHistory[0][i] = impulseVector[i];
    }

    // Шаги t = 1, 2, ..., steps
    for (int t = 1; t <= steps; t++) {
      // 1. Вычисляем импульсы p(t) на основе p(t-1)
      for (int i = 0; i < n; i++) {
        Vertex<String> v = verticesInOrder.get(i);
        double sum = 0.0;

        try {
          // Входящие рёбра к v: u → v
          for (Edge<EdgeWeight, String> edge : graph.incidentEdges(v)) {
            Vertex<String> u = graph.opposite(v, edge); // откуда пришло ребро
            Integer uIndex = nameToIndex.get(u.element());
            if (uIndex != null) {
              double weight = edge.element().getValue();
              sum += weight * pHistory[t - 1][uIndex];
            }
          }
        } catch (InvalidVertexException e) {
          e.printStackTrace();
        }

        pHistory[t][i] = sum;
      }

      // 2. Обновляем состояния: v(t) = v(t-1) + p(t)
      for (int i = 0; i < n; i++) {
        vHistory[t][i] = vHistory[t - 1][i] + pHistory[t][i];
      }
    }

    // Возвращаем историю СОСТОЯНИЙ (v), а не импульсов (p)
    return vHistory;
  }

  @FXML
  private StackPane chartContainer;
  private LineChart<Number, Number> impulseChart;

  private void setupImpulseChart() {
    if (impulseChart == null) {
      NumberAxis xAxis = new NumberAxis();
      NumberAxis yAxis = new NumberAxis();
      impulseChart = new LineChart<>(xAxis, yAxis);
      impulseChart.setTitle("Импульсное моделирование");
      impulseChart.getXAxis().setLabel("Такт");
      impulseChart.getYAxis().setLabel("Значение вершины");
      impulseChart.setCreateSymbols(false);
      impulseChart.setLegendVisible(true);
      impulseChart.setPrefSize(600, 400);
      chartContainer.getChildren().setAll(impulseChart);
    }
  }

  @FXML
  private ScrollPane vertexSelectionScrollPane;
  @FXML
  private FlowPane vertexSelectionPane;

  // Храним последние результаты моделирования
  private double[][] lastSimulationResult = null;
  private List<Vertex<String>> lastVerticesInOrder = null;
  private int lastSteps = 0;

  // Чекбоксы для каждой вершины
  private Map<Vertex<String>, CheckBox> vertexCheckBoxes = new HashMap<>();

  private void createVertexCheckBoxes(List<Vertex<String>> vertices) {
    vertexSelectionPane.getChildren().clear();
    vertexCheckBoxes.clear();

    for (Vertex<String> v : vertices) {
      CheckBox cb = new CheckBox("Вершина " + v.element());
      cb.setSelected(true); // по умолчанию отображаем все
      cb.setOnAction(e -> updateChart()); // при изменении — обновляем график
      vertexCheckBoxes.put(v, cb);
      vertexSelectionPane.getChildren().add(cb);
    }
  }

  private void updateChart() {
    if (lastSimulationResult == null || impulseChart == null) return;

    impulseChart.getData().clear();

    for (int i = 0; i < lastVerticesInOrder.size(); i++) {
      Vertex<String> v = lastVerticesInOrder.get(i);
      CheckBox cb = vertexCheckBoxes.get(v);
      if (cb != null && cb.isSelected()) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Вершина " + v.element());

        for (int t = 0; t <= lastSteps; t++) {
          series.getData().add(new XYChart.Data<>(t, lastSimulationResult[t][i]));
        }

        impulseChart.getData().add(series);
      }
    }
  }
}