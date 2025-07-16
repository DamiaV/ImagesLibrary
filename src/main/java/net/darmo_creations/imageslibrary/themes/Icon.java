package net.darmo_creations.imageslibrary.themes;

import org.jetbrains.annotations.*;

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
  MOVE_IMAGES("picture_go"),
  MERGE_IMAGES_TAGS("two_pictures"),
  DELETE_IMAGES("picture_delete"),
  EDIT_TAG("tag_blue_edit"),
  DELETE_TAGS("tag_blue_delete"),
  CREATE_TAG("tag_blue_add"),
  EDIT_TAG_TYPE("pencil"),
  DELETE_TAG_TYPE("delete"),
  CREATE_TAG_TYPE("add"),
  NEW_TAB("tab_add"),
  CLOSE_TAB("tab_delete"),
  SLIDESHOW("slideshow"),
  SLIDESHOW_SELECTED("slideshow_valid"),
  SEARCH_NO_TAGS("tag_blue_error"),
  SEARCH_NO_FILE("picture_error"),
  SEARCH_NO_HASH("tag_hash_error"),
  SEARCH_VIDEOS("film_magnify"),
  SAVED_QUERIES("magnifier"),
  MANAGE_SAVED_QUERIES("magnifier_gear"),
  FOCUS_SEARCH_BAR("search_field_go"),
  BATCH_OPERATIONS("pictures_lightning"),
  CONVERT_PYTHON_DB("database_to_database"),
  ABOUT("information"),
  HELP("help"),

  SAVE_QUERY("diskette"),
  SEARCH_HISTORY("clock_history_frame"),
  CLEAR_TEXT("draw_eraser"),
  SEARCH("magnifier"),
  SYNTAX_HIGHLIGHTING("highlighter"),
  CASE_SENSITIVITY("text_smallcaps"),
  NO_TAGS("tag_blue_error"),
  NO_FILE("picture_error"),
  NO_HASH("tag_hash_error"),
  EDIT_TAGS("pencil"),

  COMPOUND_TAG("three_tags"),

  COPY_TO_CLIPBOARD("clipboard_invoice"),

  OPEN_DB_FILE("folder_database"),
  OPEN_FILE_IN_EXPLORER("folder_go"),

  SHOW_SILIMAR_IMAGES("pictures"),
  COPY_TAGS("three_tags_down"),

  WARNING("error"),
  SELECT_DIRECTORY("folder"),

  MOVE_UP("arrow_up"),
  MOVE_DOWN("arrow_down"),
  DELETE_QUERIES("delete"),

  NEW_OPERATION_BATCH("add"),
  ADD_OPERATION("plus"),
  DELETE_OPERATION("cross"),
  SAVE_OPERATION_BATCH("diskette"),
  LOAD_OPERATION_BATCH("arrow_up"),
  DELETE_OPERATION_BATCH("delete"),
  RENAME_OPERATION_BATCH("pencil"),

  MERGE_TO_RIGHT("arrow_right"),
  MERGE_TO_LEFT("arrow_left"),

  INFO("information"),

  IMAGE("picture"),
  VIDEO("film"),

  CONTROL_PLAY("control_play_blue"),
  CONTROL_PAUSE("control_pause_blue"),
  CONTROL_STOP("control_stop_blue"),
  CONTROL_LOOP_ON("control_repeat_blue"),
  CONTROL_LOOP_OFF("control_repeat"),
  CONTROL_SOUND_ON("sound"),
  CONTROL_SOUND_OFF("sound_mute"),
  ;

  private final String baseName;

  Icon(@NotNull String baseName) {
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
