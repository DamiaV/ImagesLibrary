package net.darmo_creations.imageslibrary;

import javafx.fxml.*;
import javafx.scene.control.*;

public class HelloController {
  @FXML
  private Label welcomeText;

  @FXML
  protected void onHelloButtonClick() {
    this.welcomeText.setText("Welcome to JavaFX Application!");
  }
}
