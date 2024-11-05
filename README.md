# Conway's Life Game in Javafx
![Gif showing the JavaFX application running along 17 generations with a random initial configuration that shows a conway's life game example, you can also see a text that shows if the game is stopped or not a generation counter and a population one on the right upper corner](https://s11.gifyu.com/images/SyNNe.gif)
### **Build with:** `mvn clean javafx:run`

Sometimes Maven won't copy the resources folder and will fail running. If that bothers you, run `mvn javafx:run` without clean until it copies the folder (two times max).
That way It won't remove the compilation folder 'Project' and won't forget to copy the resources (because they'll be there already).

### If you want to modify the code you'll need `mvn clean javafx:run` to automatically clean and recompile the files.

Edit **`STEPTIME`** to modify the time between generations or **`RWIDTH`** and **`RHEIGHT`** to change the cell sizing.



