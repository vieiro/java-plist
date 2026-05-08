/*
 * Copyright 2025 Antonio Vieiro <antonio@vieiro.net>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.vieiro.plist.impl;

/**
 * Literals used as names of XML elements in plist files.
 */
interface PListLiterals {

    static final String PUBLIC_ID = "-//Apple//DTD PLIST 1.0//EN";
    static final String SYSTEM_ID = "http://www.apple.com/DTDs/PropertyList-1.0.dtd";
    static final String ARRAY = "array";
    static final String DATA = "data";
    static final String DATE = "date";
    static final String DICT = "dict";
    static final String FALSE = "false";
    static final String INTEGER = "integer";
    static final String KEY = "key";
    static final String PLIST = "plist";
    static final String REAL = "real";
    static final String STRING = "string";
    static final String TRUE = "true";
    static final String LONG = "long";

    static final String KEY_PLIST_CLASS = "net.vieiro.plist.persistence.class";
    static final String KEY_PLIST_ENUM = "net.vieiro.plist.persistence.enum";

}
