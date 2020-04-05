/*
    Polypus: a Big Data Self-Deployable Architecture for Microblogging 
    Text Extraction and Real-Time Sentiment Analysis

    Copyright (C) 2017 Rodrigo Martínez (brunneis) <dev@brunneis.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.brunneis.sg.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.brunneis.sg.exceptions.BadItemException;
import com.brunneis.sg.exceptions.DuplicateNameException;
import com.brunneis.sg.exceptions.FileParsingException;
import com.brunneis.sg.vo.Document;
import com.brunneis.sg.vo.SimpleGroup;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author brunneis
 */
public class FileHandler {

    public static void writeFile(Document doc) throws IOException {
        writeFile(doc, null);
    }

    public static void writeFile(Document doc, String path) throws IOException {
        if (doc == null) {
            throw new NullPointerException();
        }

        String title;
        if ((title = doc.getAttribute("TITLE")) == null) {
            title = "output.sg";
        } else {
            title += ".sg";
        }

        String fullPath;
        if (path == null) {
            fullPath = title;
        } else if (path.charAt(path.length() - 1)
                != '/' && path.charAt(path.length() - 1) != '\\') {
            fullPath = path + "/" + title;
        } else {
            fullPath = path + title;
        }

        try (PrintWriter writer = new PrintWriter(fullPath, "UTF-8")) {
            HashMap<String, String> attributes = doc.getAttributes();
            for (String key : attributes.keySet()) {
                writer.println(key + " = " + attributes.get(key));
            }
            writer.println("\n@BEGIN DOCUMENT\n");
            for (SimpleGroup sg : doc.getGroups()) {
                writer.println("\t@BEGIN " + sg.getName());
                List<String> keys = sg.getKeys();
                List<String> values = sg.getValues();
                // If it is a list, only the values are written
                if (keys == null) {
                    for (String value : values) {
                        writer.println("\t" + value);
                    }
                } else {
                    for (int i = 0; i < keys.size(); i++) {
                        writer.println("\t" + keys.get(i) + " = "
                                + values.get(i));
                    }
                }
                writer.println("\t@END " + sg.getName());
            }
            writer.println("\n@END DOCUMENT");
            writer.flush();
        }
    }

    public static Document loadFile(String path)
            throws FileNotFoundException, IOException, FileParsingException {
        Document document = new Document();
        File sgfile = new File(path);

        if (!sgfile.exists() || !sgfile.canRead()) {
            throw new FileNotFoundException();
        }

        BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(sgfile),
                        StandardCharsets.UTF_8));

        SimpleGroup sg = null;
        String lineContent;
        String[] kv;
        int state = 0;
        int line = 0;
        while ((lineContent = br.readLine()) != null) {
            System.out.println(lineContent);
            line++;
            // Empty line
            if (lineContent.matches("^(\\s)*$")) {
                continue;
            }
            lineContent = removeSpaces(lineContent);

            switch (state) {
                // Initial state
                case 0:
                    if (lineContent.matches("^@BEGIN DOCUMENT(\\s*)$")) {
                        state = 1;
                        break;
                    }

                    kv = lineContent.split("=");
                    if (kv.length < 2) {
                        throw new FileParsingException("At line " + line);
                    }

                    try {
                        document.addAttribute(
                                kv[0].trim(),
                                lineContent.substring(kv[0].length() + 1).trim()
                        );
                    } catch (BadItemException ex) {
                        throw new FileParsingException("At line " + line
                                + ": " + ex.getMessage());
                    }
                    break;

                // Document started
                case 1:
                    if (lineContent.matches("^@BEGIN\\s(.*)$")) {
                        String name
                                = removeSpaces(lineContent.split("^@BEGIN\\s")[1]);
                        sg = new SimpleGroup();
                        sg.setName(name);
                        state = 2;
                    }
                    if (lineContent.matches("^@END DOCUMENT(\\s*)$")) {
                        // The parsing is finished
                        return document;
                    }
                    break;
                // Group started
                case 2:
                    // If the group is finished
                    if (lineContent.matches("^@END\\s(.*)$")) {
                        try {
                            document.addGroup(sg.getCopy());
                        } catch (DuplicateNameException ex) {
                            throw new FileParsingException("At line " + line
                                    + ": " + ex.getMessage());
                        }
                        sg.clear();
                        state = 1;
                        break;
                    }

                    // If it is not finished a row is read
                    kv = lineContent.split("=");
                    try {
                        if (kv.length == 1) {
                            sg.addItem(kv[0].trim());
                        } else {
                            sg.addItem(
                                    kv[0].trim(),
                                    lineContent.substring(kv[0].length()
                                            + 1).trim()
                            );
                        }
                    } catch (BadItemException ex) {
                        throw new FileParsingException("At line " + line
                                + ": " + ex.getMessage());
                    }
                    break;
            }
        }
        throw new FileParsingException(
                "The document is not properly structured.");
    }

    private static String removeSpaces(String string) {
        return string.replaceAll("\\s\\s+", " ").trim();
    }

}
