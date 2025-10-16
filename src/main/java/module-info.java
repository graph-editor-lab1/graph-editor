@SuppressWarnings("JavaModuleDefinition")
module com {
  requires javafx.controls;
  requires javafx.fxml;
  requires com.brunomnsilva.smartgraph;

  opens com to javafx.fxml;
  opens com.controllers to javafx.fxml;
  exports com;
}