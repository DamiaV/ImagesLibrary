package net.darmo_creations.imageslibrary.ui;

import javafx.scene.layout.*;
import net.darmo_creations.imageslibrary.data.*;
import org.jetbrains.annotations.*;

public class ImagePreviewPane extends VBox {
  // TODO
  public void setImage(@Nullable Picture picture) {
    // TODO
    System.out.println(picture == null ? "null" : picture.path());
  }
}
