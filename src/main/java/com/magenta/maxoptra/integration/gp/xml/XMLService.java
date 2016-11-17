package com.magenta.maxoptra.integration.gp.xml;

import org.apache.commons.io.IOUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class XMLService {

    public static String marshal(Object object, Class type) throws JAXBException {
        StringWriter stringWriter = new StringWriter();
        JAXBContext jaxbContext = JAXBContext.newInstance(type);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        jaxbMarshaller.marshal(object, stringWriter);
        return stringWriter.toString();
    }

    public static String marshal(Object object) throws JAXBException {
        return marshal(object, object.getClass());
    }

    public static Object unmarshal(String xml, Class type) throws Exception {
        InputStream inputStream = IOUtils.toInputStream(xml, StandardCharsets.UTF_8);
        return unmarshal(inputStream, type);
    }

    public static Object unmarshal(InputStream inputStream, Class type) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(type);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        return jaxbUnmarshaller.unmarshal(inputStream);
    }

}
