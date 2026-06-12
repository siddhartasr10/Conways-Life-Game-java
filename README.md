# Conway's Life Game in Javafx

https://github.com/user-attachments/assets/4394381f-41ef-42e8-840a-48588ea3834b


Use the space key to pause or resume the game.
### **Build with:** `mvn clean javafx:run`

Sometimes Maven won't copy the resources folder and will fail running. If that bothers you, run `mvn javafx:run` without clean until it copies the folder (two times max).
That way It won't remove the compilation folder 'Project' and won't forget to copy the resources (because they'll be there already).

### If you want to modify the code you'll need `mvn clean javafx:run` to automatically clean and recompile the files.

Edit **`STEPTIME`** to modify the time between generations or **`RWIDTH`** and **`RHEIGHT`** to change the cell sizing.



