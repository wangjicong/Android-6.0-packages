
package com.android.mmsfolderview.parser;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.util.Log;

import com.android.mmsfolderview.parser.data.TimeQuantumItem;

/**
 * the sample xml in this project's etc folder
 **/
public class XmlDomParser {

    public static final String TAG = "CustomSqlQuery";
    public static final boolean isDebug = true;
    private static ArrayList<TimeQuantumItem> sItemList;

    public XmlDomParser() {
    }

    public static ArrayList<TimeQuantumItem> getItemList() {
        if (sItemList == null) {
            sItemList = new ArrayList<TimeQuantumItem>();
        }
        return sItemList;
    }

    private static void addToItemList(TimeQuantumItem item) {
        getItemList().add(item);
    }

    public static boolean xmlParse(String pathFileName, Object obj) {
        if (isDebug) {
            Log.d(TAG, "XmlDomParser.xmlParse: Parse XML Begin---------->");
        }
        DocumentBuilder builder;
        try {
            getItemList().clear();
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc;
            File file = new File(pathFileName);
            if (!file.exists()) {
                Log.d(TAG, "XmlDomParser.xmlParse: " + pathFileName + " : is not exist.");
                return false;
            }
            doc = builder.parse(file);
            doc.getDocumentElement().normalize();
            NodeList rootList = doc.getElementsByTagName(TimeQuantumItem.FLAG_DATA);
            for (int i = 0; i < rootList.getLength(); i++) {
                Element dataElement = (Element) rootList.item(i);
                int len = dataElement.getElementsByTagName(TimeQuantumItem.FLAG_ITEM).getLength();
                for (int j = 0; j < len; j++) {
                    Element itemElement = (Element) dataElement.getElementsByTagName(
                            TimeQuantumItem.FLAG_ITEM).item(j);
                    TimeQuantumItem item = new TimeQuantumItem();
                    item.InitFromElement(itemElement);
                    addToItemList(item);
                }
            }
            if (isDebug) {
                Log.d(TAG, "XmlDomParser.xmlParse: <----------Parse XML End");
            }
        } catch (Exception e) {
            Log.e(TAG, "parser happens error:" + e);
            return false;
        }
        return true;
    }
}
