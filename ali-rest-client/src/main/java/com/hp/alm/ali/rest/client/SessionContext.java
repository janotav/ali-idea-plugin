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

package com.hp.alm.ali.rest.client;

import org.apache.commons.httpclient.Cookie;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SessionContext implements Serializable {
    private static final Logger logger = Logger.getLogger(SessionContext.class.getName());

    private final String almLocation;
    private final Cookie ssoCookie;
    private final Cookie qcCookie;


    SessionContext(String almLocation, Cookie ssoCookie, Cookie qcCookie) {
        this.almLocation = almLocation;
        this.ssoCookie = ssoCookie;
        this.qcCookie = qcCookie;
    }

    public void save(File targetFile) {
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(targetFile));
            out.writeObject(this);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "IOException thrown while closing stream.", e);
                }
            }
        }

    }

    public static class InvalidFormatException extends RuntimeException {

        public InvalidFormatException(Throwable cause) {
            super(cause);
        }

        public InvalidFormatException() {
        }
    }


    public static SessionContext load(File sourceFile) {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(sourceFile));
            return (SessionContext) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InvalidClassException e) {
            throw new InvalidFormatException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "IOException thrown while closing stream.", e);
                }
            }
        }
    }


    Cookie getSsoCookie() {
        return ssoCookie;
    }

    Cookie getQcCookie() {
        return qcCookie;
    }


    String getAlmLocation() {
        return almLocation;
    }

}
