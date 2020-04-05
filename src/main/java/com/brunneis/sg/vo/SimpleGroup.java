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
import java.util.List;
import com.brunneis.sg.exceptions.BadItemException;

/**
 *
 * @author brunneis
 */
public class SimpleGroup {

    private String name;
    private final HashMap<String, String> items;
    private Boolean map;

    public SimpleGroup() {
        this.name = "";
        this.items = new HashMap<>();
        this.map = null;
    }

    private SimpleGroup(String name, HashMap<String, String> items, Boolean map) {
        this.name = name;
        this.items = items;
        this.map = map;
    }

    public List<String> getValues() {
        // If it is a list, the real values are returned (stored as keys of the map)
        if (map == false) {
            return (List<String>) new ArrayList(this.items.keySet());
        }
        return (List<String>) new ArrayList(this.items.values());
    }

    public List<String> getKeys() {
        // If it is a list, the function returns null
        if (map == false) {
            return null;
        }
        // If it is a map, a list of keys is returned
        return (List<String>) new ArrayList(this.items.keySet());
    }

    public void addItem(String key, String value) throws BadItemException {
        if (map != null) {
            if (map == false) {
                throw new BadItemException("This SimpleGroup is already marked "
                        + "as a list.");
            }
        }
        if (key == null || value == null) {
            throw new BadItemException("An attribute must have a valid key "
                    + "and a valid value.");
        }
        if (key.equals("") || value.equals("")) {
            throw new BadItemException("An attribute must have a valid key "
                    + "and a valid value.");
        }
        this.items.put(key, value);
        this.map = true;
    }

    public void addItem(String value) throws BadItemException {
        if (map != null) {
            if (map == true) {
                throw new BadItemException("This SimpleGroup is already marked "
                        + "as a map.");
            }
        }
        if (value == null) {
            throw new BadItemException("An attribute must have a valid key "
                    + "and a valid value.");
        }
        if (value.equals("")) {
            throw new BadItemException("An attribute must have a valid key "
                    + "and a valid value.");
        }
        this.items.put(value, null);
        this.map = false;
    }

    public void clear() {
        this.name = "";
        this.items.clear();
        this.map = null;
    }

    public SimpleGroup getCopy() {
        return new SimpleGroup(
                this.name,
                (HashMap<String, String>) this.items.clone(),
                this.map
        );
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int size() {
        return this.items.size();
    }

}
