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

package com.hp.alm.ali.idea.services;

import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.util.Pair;
import org.junit.Assert;

import java.util.LinkedList;

public class TestMessages implements TestDialog {

    private LinkedList<Pair<String, Integer>> messages = new LinkedList<Pair<String, Integer>>();

    @Override
    public int show(String message) {
        Assert.assertFalse("Unexpected message: " + message, messages.isEmpty());
        Pair<String, Integer> messageAndResponse = messages.removeFirst();
        Assert.assertEquals(messageAndResponse.getFirst(), message);
        return messageAndResponse.getSecond();
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public void add(String message, int response) {
        messages.add(new Pair<String, Integer>(message, response));
    }

    public String asString() {
        return messages.toString();
    }
}
