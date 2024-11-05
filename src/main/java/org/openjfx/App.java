package org.openjfx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
//import javafx.concurrent.Task; TODO: probar versiones de las versiones asíncronas como dos instancias de task.
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;

/* PREFERENTE TODO: haz los cálculos para con la resolucion y contando 1 de stroke (0.5 por cada lado = 1+ de ancho y alto).
 * TODO: de paso añade foto en el readme del github [descrip](link)
 * */
public class App extends Application {
        static Logger logger = Logger.getLogger(App.class.toString()); //private int clickcounter = 0; // for debugging
        final double RWIDTH = 30D, RHEIGHT = 30D; // rectangle size constraints. i was using 30 not perfect size.
        int STEPTIME = 500; // in milliseconds
        Rectangle[][] gridMap; List<String> pressedKeys = new ArrayList<String>();
        Label infoLabel = new Label("Game Stopped"), genLabel = new Label("Generations: 0"), popLabel = new Label("Population: 0");
        Boolean isGameRunning = false;
        Integer generations = 0, population = 0;


        protected Consumer<Rectangle> cellBehaviour = (Rectangle r) -> {
                //logger.log(Level.INFO,String.format("Cells clicked: %3s", ++clickcounter)); //logger.log(Level.INFO,String.format("Estilo de la celda antes: %s", r.getStyleClass().get(0)));
                switch (r.getStyleClass().get(0)) {
                        case "whiteSquare":
                                r.getStyleClass().remove(0); r.getStyleClass().add("blackSquare"); population--; updatePopulation();
                                break;
                        case "blackSquare":
                                r.getStyleClass().remove(0); r.getStyleClass().add("whiteSquare"); population++; updatePopulation();
                                break;
                }

        };

        // if the grid size is not divisible by the cell(rect) size there'll be a gap. could fix it to use colspan or rowspan to make it divisible but why. just use factors of 1920 or 1080.
        public Rectangle[][] populateGrid(GridPane grid, int maxRowCells, int maxColumnCells, double cellWidth, double cellHeight, Consumer<? super Rectangle> cellBehaviour ) {
                /* Populates a grid with cells, adding a parametrized event to the cells and saves its addresses on a 2d array */
                Rectangle[][] gridMap = new Rectangle[maxRowCells][maxColumnCells];
                for (int row = 0; row < maxRowCells; row++) {
                        for (int column = 0; column < maxColumnCells; column++) {
                                Rectangle cell = new Rectangle(cellWidth, cellHeight); cell.getStyleClass().add("blackSquare");
                                cell.setOnMousePressed((e) -> {cellBehaviour.accept(cell); e.consume();});
                                grid.add(cell, column, row); gridMap[row][column] = cell; // .add method is (columns,rows) and 2dArrays work by [rows][columns]
                        }
                }
                return gridMap;
        }

        @Override
        public void start(Stage stage) {
                Rectangle2D screenbox = Screen.getPrimary().getVisualBounds(); // I was using getBounds to get the screen size, getVisualBounds gets the real size.
                final int MaxCellsinWidth = (int)Math.floor(screenbox.getWidth()/(RWIDTH)), MaxCellsinHeight = (int)Math.floor(screenbox.getHeight()/(RHEIGHT));
                var root = new StackPane(); var vBox = new VBox(20) /*20px spacin */; var grid = new GridPane();
                var scene = new Scene(root, screenbox.getWidth(), screenbox.getHeight()); // Once the element is stated as the root, it gets the height and width of the scene and viceversa.
                scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); // that '/' is essential lol

                //logger.log(Level.INFO, String.format("maxX of the screen: %5f, maxX of the grid: %5f, maxX of the scene which contains the grid: %5f \n", new Object[] {screenbox.getMaxX(),grid.getLayoutBounds().getMaxX(), scene.getWidth()})); logger.log(Level.INFO, String.format("maxY of the screen: %5f, maxY of the grid: %5f, maxY of the scene which contains the grid: %5f \n", new Object[] {screenbox.getMaxY(),grid.getLayoutBounds().getMaxY(), scene.getHeight()}));logger.log(Level.INFO, String.format("Max cells width: %5s, Max cells height: %5s", MaxCellsinWidth, MaxCellsinHeight));

                vBox.setMouseTransparent(true); vBox.setAlignment(Pos.TOP_RIGHT); root.setStyle("-fx-background-color:black"); // if backgorund color isn't set borders bug
                root.getChildren().addAll(grid, vBox); vBox.getChildren().addAll(infoLabel, genLabel, popLabel);
                infoLabel.getStyleClass().add("stoppedText"); genLabel.getStyleClass().add("genericText"); popLabel.getStyleClass().add("genericText");

                // Don't save the alt key presses because if I alt+tab it doesnt detect the release of the alt (it isn't a big issue but anyways)
                scene.setOnKeyPressed((e) -> {if (!pressedKeys.contains(e.getCode().getName()) && !e.getCode().getName().equals("Alt")) pressedKeys.add(e.getCode().getName());}); //logger.info(pressedKeys.toString());});
                scene.setOnKeyReleased((e) -> {try {Thread.sleep(100);} catch (InterruptedException excpt) {excpt.printStackTrace();}; if (pressedKeys.contains(e.getCode().getName())) pressedKeys.remove(e.getCode().getName());}); //logger.info(pressedKeys.toString());});
                stage.setScene(scene); stage.centerOnScreen(); stage.show(); //logger.log(Level.INFO, String.format("is fullscreen: %s", stage.isFullScreen()));

                gridMap = populateGrid(grid, MaxCellsinHeight, MaxCellsinWidth, RWIDTH, RHEIGHT, cellBehaviour);
                assert MaxCellsinHeight == gridMap.length && MaxCellsinWidth == gridMap[0].length; logger.log(Level.INFO, "Cells in grid are the predicted maxcellinwidth and inheight"); // logger.log(Level.INFO, String.format("total cells in width: %2s, total cells in height: %2s", new Object[] {gridMap[0].length, gridMap.length})); //logger.log(Level.INFO,String.format("Map grid: %5s", Arrays.toString(gridMap)));
                getInputsAsync();
        }


        // It can't work reading from top to bottom and changing values at the same time. (because some cells will die before others count them and viceversa) this game works step after step.
        // it can't also use a copy to pass the values (all of the values are pointers so the copy is basically the same object)
        // this func only works for square grids, (because every row is the same length)
        public void gameStep(Rectangle[][] grid) {
                String alive = "whiteSquare"; String dead = "blackSquare"; Boolean[][] fakeGrid = new Boolean[grid.length][grid[0].length];
                for (int i = 0; i < grid.length; i++) {
                        for (int j = 0; j < grid[0].length; j++) { // grid[i][j];
                                int neighbors = 0; // this code assumes that every rectangle has a style class. if not a nullException will occur
                                if (i != 0 && j != 0) { int res = (grid[i-1][j-1].getStyleClass().get(0).equals(alive)) ? 1 : 0; neighbors += res;} // check upleft corner if not first row or column
                                if (i != 0 && j != grid[i].length-1) { int res = (grid[i-1][j+1].getStyleClass().get(0).equals(alive)) ? 1 : 0; neighbors += res;} // check upright corner if not 1º row or last column
                                if (i != grid.length-1 && j != 0) { int res = (grid[i+1][j-1].getStyleClass().get(0).equals(alive)) ? 1 : 0; neighbors += res;} // check downleft corner if not last row or first column
                                if (i != grid.length-1 && j != grid[i].length-1) { int res = (grid[i+1][j+1].getStyleClass().get(0).equals(alive)) ? 1 : 0; neighbors += res;} // check downright if not lastrow or last column

                                if (i != 0) { int res = (grid[i-1][j].getStyleClass().get(0).equals(alive)) ? 1 : 0; neighbors += res;} //check up if not on first row
                                if (i != grid.length-1) { int res = (grid[i+1][j].getStyleClass().get(0).equals(alive)) ? 1 : 0; neighbors += res;} // check down if not last row
                                if (j != 0) { int res = (grid[i][j-1].getStyleClass().get(0).equals(alive)) ? 1 : 0; neighbors += res;} // check left if not on first column
                                if (j != grid[i].length-1){ int res = (grid[i][j+1].getStyleClass().get(0).equals(alive)) ? 1 : 0; neighbors += res;} // check right if not on last column

                                if (grid[i][j].getStyleClass().get(0).equals(dead) && neighbors == 3) {fakeGrid[i][j] = true; continue;} // get born
                                if (grid[i][j].getStyleClass().get(0).equals(alive) && (neighbors > 3 || neighbors < 2)) {fakeGrid[i][j] = false; continue;} // death causes
                                // if not under/over populated or born then the cells stays the same (2 or 3 neighbors and alive or != 3 and dead)
                        }
                }

                Platform.runLater(() -> {
                                for (int i = 0; i < fakeGrid.length; i++) {
                                        for (int j = 0; j < fakeGrid[0].length; j++) {
                                                if (Objects.isNull(fakeGrid[i][j])) continue; // the cells that survived didn't get added to the fakeGrid, so null is == continues alive. === nothing .
                                                if (!fakeGrid[i][j]) {grid[i][j].getStyleClass().remove(0); grid[i][j].getStyleClass().add(dead); population--; continue;}
                                                grid[i][j].getStyleClass().remove(0); grid[i][j].getStyleClass().add(alive); population++;
                                        }
                                }
                                updateGeneration(); updatePopulation();
                        });
        }
        public CompletableFuture<Void> gameLoopAsync(Rectangle[][] grid) {
                return CompletableFuture.runAsync(() -> {
                                while (true) {
                                        if (!isGameRunning) break;
                                        gameStep(grid);
                                        try {Thread.sleep(STEPTIME);}
                                        catch (InterruptedException e) {e.printStackTrace(); break;}
                                }
                        });
        }

        public CompletableFuture<Void> getInputsAsync() {
                return CompletableFuture.runAsync(() -> {
                                while (true) {
                                        Platform.runLater(() -> {
                                                        Iterator<String> iterator = pressedKeys.iterator();
                                                        while (iterator.hasNext()) {
                                                                String key = iterator.next();
                                                                switch (key) {
                                                                case "Space":
                                                                        iterator.remove();
                                                                        //logger.info("Logged space key!");
                                                                        isGameRunning = !isGameRunning; infoLabel.getStyleClass().removeAll("runningText", "stoppedText");
                                                                        // if we change it from 'off' to on we need to turn it on
                                                                        if (isGameRunning) {
                                                                                gameLoopAsync(gridMap); infoLabel.setText("Game is Running");
                                                                                infoLabel.getStyleClass().add("runningText"); //logger.info(infoLabel.getStyleClass().toString());
                                                                                continue;
                                                                        }
                                                                        infoLabel.setText("Game stopped"); infoLabel.getStyleClass().add("stoppedText");}// logger.info(infoLabel.getStyleClass().toString());
                                                        }
                                                });
                                        try {Thread.sleep(100);}
                                        catch (InterruptedException e) {e.printStackTrace();}
                                }
                        });
        }
        public void updatePopulation() {
                popLabel.setText(String.format("Population: %s", population));
        }
        public void updateGeneration() {
                generations++; genLabel.setText(String.format("Generations: %s", generations));
        }

        public static void main(String[] args) {
                launch();
        }


}
