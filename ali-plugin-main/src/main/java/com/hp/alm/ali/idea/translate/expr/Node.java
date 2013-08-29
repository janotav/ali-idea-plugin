/*
 * Copyright 2013 Hewlett-Packard Development Company, L.P
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.alm.ali.idea.translate.expr;

public class Node {

    public Type type;
    public Node left;
    public Node right;
    public String value;

    public Node(String value) {
        type = Type.VALUE;
        this.value = value;
    }

    public Node(Type type, Node left) {
        this.type = type;
        this.left = left;
    }

    public Node(Type type, Node left, Node right) {
        this.type = type;
        this.left = left;
        this.right = right;
    }
}
