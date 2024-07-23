# Image Library

Image Library lets you manage images by associating tags to them.
This is a full re-implementation of my [Python-based application](https://github.com/Darmo117/ImageDatabase).

Main features:

- Image tagging
- Tagged images searching with a simple querying language
- Replace/move image files while keeping all associated tags
- Manage tags (create, remove, organize with types)
- Tag completion in queries and image tags editor
- Apply pattern-based transformations to images paths and tags
- List similar images (hash-based)
- Fully translated interface, available in English, French, and Esperanto

# Installation

Simply drop the `.jar` file where you want the application to run from.

# Updating

Replace the old `.jar` file by the new one. The database will be updated automatically the next time the app is
launched.

# Usage

## Registering images

Go through the *File* menu and click on *Add Files* to add images or *Add Directory* to import all images from a
directory; or you can simply drag-and-drop files and directories into the main window.

You should see a dialog window with a preview of an image and a text area. This text area is where you have to type
the tags for the displayed image. Once you’re done, click on *Next* to go to the next image or *Finish* to finish. You
can click on *Skip* to skip the current image and go directly to the next one.

While editing tags, you can choose where to move the current image by clicking on *Move to…*; the path is then displayed
below the button.

If the application found similar images already registered, a button labelled *Show similar images…* will be available
above the text area. It will show a list of similar images, ordered by decreasing estimated similarity. You can select
one of these images and copy its tags by clicking on the button above the tags list (**Warning**: it will replace all
tags in the text box).

## Searching for registered images

You can search for images by typing queries in the search field.

### Basic syntax

Tag names can contain any Unicode letters and digits, as well as underscores `_`.

- `a` will match images with tag `a`
- `a b` will match images with both tags `a` *and* `b`
- `a + b` will match images with tags `a` *or* `b` or *both*
- `-a` will match images *without* tag `a`
- Parentheses `(` and `)` can be used to group query elements

### Flags

Flags are special tags that represent a specific image property.

#### Syntax

```
#<name>
```

Where `<name>` is the flag’s name.

#### Available flags

<table>
    <tr>
        <th>Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td><code>no_tags</code></td>
        <td>Match images that have no attached tags</td>
    </tr>
    <tr>
        <td><code>no_file</code></td>
        <td>Match images whose file is missing</td>
    </tr>
    <tr>
        <td><code>no_hash</code></td>
        <td>Match images whose hash could not be computed</td>
    </tr>
</table>

#### Examples

```
#no_file a b
```

This will match all images whose file is missing that have both tags `a` and `b`.

```
#no_file #no_tags
```

This will match all images whose file is missing and that have no tags.

### Pseudo-tags

#### Syntax

There exist special tags called *pseudo-tags*. Their syntax is as follows:

```
<name>=<flags><value>
```

Where `<name>` is the pseudo-tag’s name, `<value>` is its value, and `<flags>` are optional flags that modify how the
value is interpreted.

This value may be one of two types:

- A string, delimited by double quotes `"` (e.g.: `"this is text"`), to specify a plain text value. String values may
  support some special placeholder tags: `?` to match 0 or 1 character, `*` to match 0 or more characters. For instance,
  `"a*b"` will match any text starting with an `a` and ending with a `b` with any character in-between.
  You can disable them by putting a `\ ` before (e.g.: `\*` will match the character `*`
  literally). You also have to escape all single `\ ` by doubling them: `\\`.
  Double quotes must be escaped as well.
- A RegEx, delimited by slashes `/` (e.g.: `/this is a RegEx/`). RegExs use the Java Pattern syntax.
  Note that you have to escape all `/` too, as this is the delimiter.

Both types support flags. Flags are specified before the value, without any spaces in-between (e.g. `i/regex/`
and `i"text"`).

Available flags are:

- `s` to force case-sensitive matching
- `i` to force case-insensitive matching

The default case-sensitivity depends on the app’s configuration.

Full syntax examples:

- `ext=i"jp?g"` matches all files whose extension is either `jpg` or `jpeg`, regardless of case.
- `name=s/illustration_\d+/` matches all files whose name contains the string `illustration_` followed by a one or more
  digits (e.g.: `illustration_1` or `illustration_22_background`).
- `ext="png"` matches all files whose extension is `png`, using the application’s default case-sensitivity.

#### Available pseudo-tags

<table>
    <tr>
        <th>Name</th>
        <th>Supports string placeholders</th>
        <th>Supports RegEx values</th>
        <th>Description</th>
    </tr>
    <tr>
        <td><code>ext</code></td>
        <td>Yes</td>
        <td>Yes</td>
        <td>Match images based on their extension (without the dot)</td>
    </tr>
    <tr>
        <td><code>name</code></td>
        <td>Yes</td>
        <td>Yes</td>
        <td>Match images based on their name, including extension</td>
    </tr>
    <tr>
        <td><code>path</code></td>
        <td>Yes</td>
        <td>Yes</td>
        <td>Match images based on their full path</td>
    </tr>
    <tr>
        <td><code>similar_to</code></td>
        <td>No</td>
        <td>No</td>
        <td>Match images that are similar to the specified one</td>
    </tr>
</table>

#### Examples

```
path="/home/user/images/summer?.png
```

This will match images with *paths* like `/home/user/images/summer.png`, `/home/user/images/summer1.png`,
`/home/user/images/summers.png`, etc.

```
similar_to="/home/user/images/house.png"
```

This will match all images that are similar to `/home/user/images/house.png` (if it is registered in the database).

```
a (b + c) + -(d + e) ext=i"jp?g"
```

Here’s how to interpret it:

- `a (b + c)` returns the set of images with both tags `a` and `b` and/or both tags `a` and `c`.
- `-(d + e) ext=i"jp?g"` = `-d -e ext=i"jp?g"` returns the set of JPG images without tags `d` nor `e`; note the `?` to
  match both `jpg` and `jpeg` extensions, and the `i` flag to disregard case.

The result is the union of both image sets.

### Tag definitions

The application supports tag definitions, i.e. tags defined from tag queries (e.g.: tag `animal` could be defined
as `cat + dog + bird`). You cannot tag images directly with tags that have a definition, they exist only for querying
purposes.

# Configuration file

The following configurations can be modified in the `settings.ini` file. If the file does not exist, launch the
application at least once to create it.

- Section `[App]`:
    - `database_file`: path to database file; can be absolute, or relative to the app’s root directory
    - `language`: language code of app’s interface; can be either `en` for English, `fr` for French, or `eo` for
      Esperanto
    - `theme`: the theme to apply; can be either `light` or `dark`
- Section `[Queries]`:
    - `case_sensitive_by_default` (boolean): specifies whether pseudo-tags matches should be case sensitive by default
    - `syntax_highlighting` (boolean): specifies whether tag queries syntax highlighting should be activated
- Section `[Slideshow]`:
    - `shuffle` (boolean): specifies whether slideshow images should be shuffled
    - `delay` (integer): the delay in seconds between each slideshow image

# Found a bug?

If you encounter a bug or the app crashed, check the error log located in the `logs` directory where you put the
application’s JAR file and see if there’s an error.
You can send me a message or open an issue with the error and a description of how you got this error, that would be
really appreciated!

# Documentation

Soon…

# Requirements

- [Java 17 or later](https://www.oracle.com/java/technologies/downloads/) (untested on Java 18+)

# Author

- Damia Vergnet [@Darmo117](https://github.com/Darmo117)
