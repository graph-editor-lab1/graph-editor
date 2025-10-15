package com;

/**
 * App
 * 	Grid (передаем координату, там внутри он смотрит очередь и обрабатывает клик как желает нужно по типу Event)
 * 		Map<Coordinate, GraphObject>
 * 		EventQueue
 * 	Panel (генерирует Event-ы в очередь событий)
 * 		List<Button> (вершина, дуга, текст)
 * 		EventQueue
 */
public class App {
  public static void main(String[] args) {
    System.out.println("Hello World!");
  }

}
