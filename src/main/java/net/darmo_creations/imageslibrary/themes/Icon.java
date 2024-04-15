package net.darmo_creations.imageslibrary.themes;

import java.util.*;

/**
 * Enumeration of all icons used throughout the app.
 */
public enum Icon {
  IMPORT_IMAGES("folder_picture"),
  IMPORT_DIRECTORIES("folder"),
  SETTINGS("cog"),
  QUIT("door_in"),
  EDIT_IMAGES("picture_edit"),
  RENAME_IMAGES("picture_textfield"),
  MOVE_IMAGES("picture_go"),
  DELETE_IMAGES("picture_delete"),
  EDIT_TAG("tag_blue_edit"),
  DELETE_TAGS("tag_blue_delete"),
  EDIT_TAG_TYPE("pencil"),
  DELETE_TAG_TYPE("delete"),
  CREATE_TAG_TYPE("add"),
  SLIDESHOW("slideshow"),
  SLIDESHOW_SELECTED("slideshow"),
  SEARCH_NO_TAGS("tag_blue_error"),
  SEARCH_NO_FILE("picture_error"),
  CONVERT_PYTHON_DB("database_gear"),
  FOCUS_SEARCH_BAR("search_field_go"),
  ABOUT("information"),
  HELP("help"),

  SEARCH_HISTORY("clock_history_frame"),
  CLEAR_TEXT("draw_eraser"),
  SEARCH("magnifier"),
  SYNTAX_HIGHLIGHTING("highlighter"),
  CASE_SENSITIVITY("text_smallcaps"),
  NO_TAGS("tag_blue_error"),
  NO_FILE("picture_error"),

  COMPOUND_TAG("three_tags"),

  OPEN_DB_FILE("folder_database"),
  OPEN_FILE_IN_EXPLORER("folder_go"),
  ;

  private final String baseName;

  Icon(String baseName) {
    this.baseName = Objects.requireNonNull(baseName);
  }

  /**
   * The base name of this icon.
   */
  public String baseName() {
    return this.baseName;
  }

  /**
   * Enumeration of possible icon sizes.
   */
  public enum Size {
    /**
     * 16x16 pixels size.
     */
    SMALL(16),
    /**
     * 32x32 pixels size.
     */
    BIG(32),
    ;

    private final int pixels;

    Size(int pixels) {
      this.pixels = pixels;
    }

    /**
     * The width/height in pixels.
     */
    public int pixels() {
      return this.pixels;
    }
  }
}
