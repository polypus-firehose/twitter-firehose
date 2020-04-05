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
package com.brunneis.sg.exceptions;

/**
 * This exception is launched when trying to add an attribute to a Document or
 * an item to a SimpleGroup with a null or void key or value. Also when trying
 * to add a single value when the SimpleGroup is marked as a map or when adding
 * a map entry when the SimpleGroup is marked as a list.
 *
 * @author brunneis
 */
public class BadItemException extends Exception {

    public BadItemException() {
    }

    public BadItemException(String message) {
        super(message);
    }
}
