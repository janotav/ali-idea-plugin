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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

public class IndicatingInputStream extends FileInputStream {

    private ProgressIndicator indicator;
    private long size;
    private long pos;
    private List<OutputStream> observers = new LinkedList<OutputStream>();

    public IndicatingInputStream(File file, ProgressIndicator indicator) throws FileNotFoundException {
        super(file);

        this.indicator = indicator;
        size = file.length();
    }

    public void addObserver(OutputStream observer) {
        observers.add(observer);
    }

    public int read() throws IOException {
        cancel();
        int i = super.read();
        if(i != -1) {
            for(OutputStream observer: observers) {
                observer.write(i);
            }
            report(1);
        }
        return i;
    }

    public int read(byte[] bytes) throws IOException {
        cancel();
        int i = super.read(bytes);
        if(i != -1) {
            for(OutputStream observer: observers) {
                observer.write(bytes, 0, i);
            }
            report(i);
        }
        return i;
    }

    public int read(byte[] bytes, int ofs, int len) throws IOException {
        cancel();
        int i = super.read(bytes, ofs, len);
        if(i != -1) {
            for(OutputStream observer: observers) {
                observer.write(bytes, ofs, i);
            }
            report(len);
        }
        return i;
    }

    private void report(int len) {
        pos += len;
        if(indicator != null) {
            indicator.setFraction((double)pos / size);
        }
    }

    private void cancel() throws IOException {
        if(indicator != null && indicator.isCanceled()) {
            throw new CanceledException();
        }
    }
}
