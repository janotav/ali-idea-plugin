//(C) Copyright 2003-2012 Hewlett-Packard Development Company, L.P.

package com.hp.alm.ali.idea.model.parser;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.ArrayList;

public abstract class AbstractList<E> extends ArrayList<E> {

    protected XMLEventReader reader;

    protected AbstractList() {
    }

    protected void initNoEx(InputStream is) {
        try {
            init(is);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    protected void init(InputStream is) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        reader = factory.createXMLEventReader(is);
        while(true) {
            XMLEvent event = reader.nextEvent();
            if(event instanceof EndDocument) {
                reader.close();
                break;
            }
            if(event instanceof StartElement) {
                onStartElement((StartElement)event);
            }
            if(event instanceof EndElement) {
                onEndElement((EndElement)event);
            }
        }
    }

    protected AbstractList(InputStream is) throws XMLStreamException {
        init(is);
    }

    protected String readNextValue() throws XMLStreamException {
        return readNextValue(reader);
    }

    protected E getLast() {
        return get(size() - 1);
    }

    public static String readNextValue(XMLEventReader reader) throws XMLStreamException {
        StringBuffer buf = new StringBuffer();
        while(reader.peek() instanceof Characters) {
            buf.append(((Characters)reader.nextEvent()).getData());
        }
        return buf.toString();
    }

    protected abstract void onStartElement(StartElement element) throws XMLStreamException;

    protected void onEndElement(EndElement element) throws XMLStreamException {
    }

}
