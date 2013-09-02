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

package com.hp.alm.ali.idea.progress;

import com.intellij.openapi.progress.ProgressIndicator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

public class IndicatingOutputStream extends FileOutputStream {
    private ProgressIndicator indicator;
    private int size;
    private int pos;
    private List<OutputStream> observers;

    public IndicatingOutputStream(File file, int size, ProgressIndicator indicator) throws FileNotFoundException {
        super(file);
        this.size = size;
        this.indicator = indicator;
        this.pos = 0;

        observers = new LinkedList<OutputStream>();
    }

    public void addObserver(OutputStream observer) {
        observers.add(observer);
    }

    public void write(int i) throws IOException {
        cancel();
        super.write(i);
        for(OutputStream observer: observers) {
            observer.write(i);
        }
        report(1);
    }

    public void write(byte[] bytes) throws IOException {
        cancel();
        super.write(bytes);
        for(OutputStream observer: observers) {
            observer.write(bytes);
        }
        report(bytes.length);
    }

    public void write(byte[] bytes, int off, int len) throws IOException {
        cancel();
        super.write(bytes, off, len);
        for(OutputStream observer: observers) {
            observer.write(bytes, off, len);
        }
        report(len);
    }

    private void cancel() throws IOException {
        if(indicator != null && indicator.isCanceled()) {
            throw new CanceledException();
        }
    }

    private void report(int len) {
        pos += len;
        if(indicator != null) {
            indicator.setFraction((double)pos / size);
        }
    }
}
