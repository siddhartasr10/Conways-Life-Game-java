package org.openjfx;

import javafx.application.Application;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Rectangle2D;
import javafx.scene.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import java.util.Objects;

/* TODO: Haz que las generaciones pasen presionando una tecla y que se vayan contando en una etiqueda,
 * además si puedes haz que se muestre la poblacion en otra etiqueta o algo y con una tecla que
 * el loop se pare y se reanude, (además que se muestre si está parado o reanudado en, otra etiqueta jajaj)
 * */
public class App extends Application {
        Logger logger = Logger.getLogger("Debug_Help");
        private int clickcounter = 0; // for debugging the height
        final int RWIDTH = 30; final int RHEIGHT = 30; // rectangle size constants.
        final int TIMEOUT = 5000; // in milliseconds
        protected Consumer<Rectangle> cellBehaviour = (r) -> {
                logger.log(Level.INFO,String.format("Cells clicked: %3s", ++clickcounter));
                //logger.log(Level.INFO,String.format("Estilo de la celda antes: %s", r.getStyle()));
                switch(r.getStyle()) {
                        case "-fx-fill:white":
                                r.setStyle("-fx-fill:black");
                                break;
                        case "-fx-fill:black":
                                r.setStyle("-fx-fill:white");
                                break;
                }
                //logger.log(Level.INFO,String.format("Estilo de la celda despues: %s", r.getStyle()));
        };
        // if the grid size is not divisible by the cell(rect) size there'll be a gap. could fix it to use colspan or rowspan to make it divisible but why. just use factors of 1920 or 1080 lol.
        public Rectangle[][] populateGrid(GridPane grid, int maxRowCells, int maxColumnCells, int cellWidth, int cellHeight, Consumer<? super Rectangle> cellBehaviour ) {
                /* Populates a grid with cells, adding a parametrized event to the cells and saves its pointers on a 2d array */
                Rectangle[][] gridMap = new Rectangle[maxRowCells][maxColumnCells]; // in a perfect fullscreen this would be precise
                for (int i = 0; i < maxRowCells; i++) { // 34 instead of 1080/30 = 36 bc cant fully fullscreen so i have less cells 0to34 is 35 but pixels start at 0.
                        for (int j = 0; j < maxColumnCells; j++) { // pixels start at 0 so real
                                Rectangle cell = new Rectangle(cellWidth, cellHeight); cell.setStyle("-fx-fill:black");
                                cell.setOnMousePressed((e) -> {cellBehaviour.accept(cell); e.consume();});
                                grid.add(cell, j, i); gridMap[i][j] = cell; // order is in reverse bc j is columns and i rows .add method is columns,rows and 2dArrays work by [rows][columns]
                        }
                }
                return gridMap;
        }

        @Override
        public void start(Stage stage) throws InterruptedException {
                Rectangle2D screenbox = Screen.getPrimary().getVisualBounds(); // VisualBounds not Bounds pls.
                var grid = new GridPane(); grid.setStyle("-fx-background-color:black");
                var mainScene = new Scene(grid, screenbox.getWidth(), screenbox.getHeight()); // Once the grid is stated as the root, it gets the height and width of the scene.
                logger.log(Level.INFO, String.format("maxX of the screen: %5f, maxX of the grid: %5f, maxX of the mainScene which contains the grid: %5f \n", new Object[] {screenbox.getMaxX(),grid.getLayoutBounds().getMaxX(), mainScene.getWidth()})); logger.log(Level.INFO, String.format("maxY of the screen: %5f, maxY of the grid: %5f, maxY of the mainScene which contains the grid: %5f \n", new Object[] {screenbox.getMaxY(),grid.getLayoutBounds().getMaxY(), mainScene.getHeight()}));
                // I was using getBounds to get the screen size, getVisualBounds gets the real size.
                final int MaxCellsinWidth= (int)Math.floor(mainScene.getWidth()/RWIDTH); final int MaxCellsinHeight= (int)Math.floor(mainScene.getHeight()/RHEIGHT);
                logger.log(Level.INFO, String.format("Max cells width: %5s, Max cells height: %5s", MaxCellsinWidth, MaxCellsinHeight));
                Rectangle[][] gridMap = populateGrid(grid, MaxCellsinHeight, MaxCellsinWidth, RWIDTH, RHEIGHT, cellBehaviour);
                stage.setScene(mainScene); stage.centerOnScreen(); stage.show();
                logger.log(Level.INFO, String.format("is fullscreen: %s", stage.isFullScreen()));logger.log(Level.INFO, String.format("total cells in width: %2s, total cells in height: %2s", new Object[] {gridMap[0].length, gridMap.length})); //logger.log(Level.INFO,String.format("Map grid: %5s", Arrays.toString(gridMap)));
                gameLoop(gridMap);
        }


        // It can't work reading from top to bottom and changing values at the same time. (because some cells will die before others count them and viceversa) this game works step after step.
        // it can't also use a copy to pass the values (all of the values are pointers so the copy is basically the same object)
        // this func only works for square grids, (because every row is the same length)
        public void gameStep(Rectangle[][] grid) {
                String alive = "-fx-fill:white"; String dead = "-fx-fill:black"; Boolean[][] fakeGrid = new Boolean[grid.length][grid[0].length];
                for (int i = 0; i < grid.length; i++) {
                        for (int j = 0; j < grid[0].length; j++) { // grid[i][j];
                                int aliveNeighbors = 0;
                                if (i != 0 && j != 0) { int res = (grid[i-1][j-1].getStyle().equals(alive)) ? 1 : 0; aliveNeighbors += res;} // check upleft corner if not first row or column
                                if (i != 0 && j != grid[i].length-1) { int res = (grid[i-1][j+1].getStyle().equals(alive)) ? 1 : 0; aliveNeighbors += res;} // check upright corner if not 1º row or last column
                                if (i != grid.length-1 && j != 0) { int res = (grid[i+1][j-1].getStyle().equals(alive)) ? 1 : 0; aliveNeighbors += res;} // check downleft corner if not last row or first column
                                if (i != grid.length-1 && j != grid[i].length-1) { int res = (grid[i+1][j+1].getStyle().equals(alive)) ? 1 : 0; aliveNeighbors += res;} // check downright if not lastrow or last column

                                if (i != 0) {int res = (grid[i-1][j].getStyle().equals(alive)) ? 1 : 0; aliveNeighbors += res;} //check up if not on first row
                                if (i != grid.length-1) {int res = (grid[i+1][j].getStyle().equals(alive)) ? 1 : 0; aliveNeighbors += res;} // check down if not last row
                                if (j != 0) {int res = (grid[i][j-1].getStyle().equals(alive)) ? 1 : 0;aliveNeighbors += res;} // check left if not on first column
                                if (j != grid[i].length-1){int res = (grid[i][j+1].getStyle().equals(alive)) ? 1 : 0; aliveNeighbors += res;} // check right if not on last column

                                if (grid[i][j].getStyle().equals(dead) && aliveNeighbors == 3) {fakeGrid[i][j] = true; continue;} // get born
                                if (grid[i][j].getStyle().equals(alive) && (aliveNeighbors > 3 || aliveNeighbors < 2)) {fakeGrid[i][j] = false; continue;} // death causes
                                // if not under/over populated or born then the cells stays the same (2 or 3 neighbors and alive or != 3 and dead)
                        }
                }

                for (int i = 0; i < fakeGrid.length; i++) {
                        for (int j = 0; j < fakeGrid[0].length; j++) {
                                if (Objects.isNull(fakeGrid[i][j])) continue; // the cells that survived didn't get added to the fakeGrid, so null is == continues alive. === nothing .
                                if (!fakeGrid[i][j]) {grid[i][j].setStyle(dead); continue;}
                                grid[i][j].setStyle(alive);
                        }
                }
        }
        public CompletableFuture<Void> gameLoop(Rectangle[][] grid) {
                return CompletableFuture.runAsync(() -> {
                                while (true) {
                                        gameStep(grid);
                                        try {Thread.sleep(TIMEOUT);}
                                        catch (InterruptedException e) {e.printStackTrace(); break;}
                                }
                        });
        }

        public static void main(String[] args) {
                launch();
        }
}
        /* Nace: Si una célula muerta tiene exactamente 3 células vecinas vivas "nace" (es decir, al turno siguiente estará viva).
         * Muere: una célula viva puede morir por uno de 2 casos:
         *     Sobrepoblación
         *     Aislamiento: si tiene solo un vecino alrededor o ninguno.
         * Vive: una célula se mantiene viva si tiene 2 o 3 vecinos a su alrededor.
         * cuentan las esquinas.
         */
