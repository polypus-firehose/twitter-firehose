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
package com.brunneis.sg.vo;

import java.util.ArrayList;
import java.util.HashMap;
import com.brunneis.sg.exceptions.BadItemException;
import com.brunneis.sg.exceptions.DuplicateNameException;
import java.util.List;

/**
 *
 * @author brunneis
 */
public class Document {

    HashMap<String, String> attributes;
    ArrayList<SimpleGroup> groups;

    public Document() {
        this.attributes = new HashMap<>();
        this.groups = new ArrayList<>();
    }

    public void addAttribute(String key, String value)
            throws BadItemException {
        if (key == null || value == null) {
            throw new BadItemException("An attribute must have a valid key "
                    + "and a valid value.");
        }
        if (key.equals("") || value.equals("")) {
            throw new BadItemException("An attribute must have a valid key "
                    + "and a valid value.");
        }
        this.attributes.put(key.toUpperCase(), value);
    }

    public String getAttribute(String key) {
        key = key.toUpperCase();
        return this.attributes.get(key);
    }

    public HashMap<String, String> getAttributes() {
        return this.attributes;
    }

    public void addGroup(SimpleGroup sg) throws DuplicateNameException {
        for (SimpleGroup group : groups) {
            if (group.getName().toLowerCase()
                    .equals(sg.getName().toLowerCase())) {
                throw new DuplicateNameException("One group already exists"
                        + "with the same name.");
            }
        }
        // If the name is empty a serial number is used
        if (sg.getName() == null) {
            sg.setName(nextSerial());
        } else if (sg.getName().equals("")) {
            sg.setName(nextSerial());
        }
        this.groups.add(sg);
    }

    public List<SimpleGroup> getGroups() {
        return this.groups;
    }

    public SimpleGroup getGroup(String name) {
        name = name.toLowerCase();
        SimpleGroup sg;
        for (SimpleGroup sgi : this.groups) {
            if (sgi.getName().toLowerCase().equals(name)) {
                return sgi;
            }
        }
        return null;
    }

    private String nextSerial() {
        String name = null;
        boolean unique = true;

        for (int i = 1; !unique || i == 1; i++) {
            name = "GROUP_" + Integer.toString(this.groups.size() + i);
            for (SimpleGroup group : groups) {
                if (group.getName().toLowerCase().equals(name.toLowerCase())) {
                    unique = false;
                }
            }
        }

        return name;
    }

    public void clear() {
        this.attributes.clear();
        this.groups.clear();
    }
}
