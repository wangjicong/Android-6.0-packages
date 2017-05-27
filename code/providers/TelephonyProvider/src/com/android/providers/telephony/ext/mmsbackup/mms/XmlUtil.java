
package com.android.providers.telephony.ext.mmsbackup.mms;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.ContentValues;
import android.text.TextUtils;

public class XmlUtil {

    public static final String[] MMS_KEYS = new String[] {
            "_id", "isread", "msg_box",
            "date", "m_size", "sim_id",
            "islocked"
    };
    protected static final String RECORD = "record";

    protected static final String DATE = "backupdate";
    protected static final String DEVICE = "devicetype";
    protected static final String SYSTEM = "system";
    protected static final String COMPONENTLIST = "component_list";
    protected static final String COMPONENT = "component";
    protected static final String NAME = "name";
    protected static final String ID = "id";
    public static final String FOLDER = "folder";
    public static final String COUNT = "count";

    private DocumentBuilder mBuilder;
    private Document mDoc;
    private Element mRoot;
    private ArrayList<ContentValues> mList;

    public XmlUtil(String root) {
        if (TextUtils.isEmpty(root)) {
            throw new IllegalArgumentException("Root is invalid!");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            mBuilder = factory.newDocumentBuilder();
            mDoc = mBuilder.newDocument();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        if (mDoc != null) {
            mRoot = mDoc.createElement(root);
            mDoc.appendChild(mRoot);
        }
    }

    public XmlUtil() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            mBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void setAttribute(ContentValues values) {
        if (values == null || mRoot == null || mDoc == null) {
            return;
        }
        int size = values.size();
        if (values.size() > 0) {
            Element e = mDoc.createElement(RECORD);
            String[] keys = new String[size];
            Set<Entry<String, Object>> set = values.valueSet();
            Iterator<Entry<String, Object>> its = set.iterator();
            int n = 0;
            while (its.hasNext()) {
                Entry<String, Object> entry = its.next();
                String key = entry.getKey();
                keys[n] = key;
                n++;
            }
            if ("note".equalsIgnoreCase(mRoot.getTagName())) {
                for (String key : keys) {
                    String v = values.getAsString(key);
                    e.setAttribute(key, v);
                }
            } else {
                /** try to store in the order **/
                String[] v = new String[n];
                for (int i = 0; i < n; i++) {
                    v[i] = values.getAsString(MMS_KEYS[i]);
                    e.setAttribute(MMS_KEYS[i], v[i]);
                }
            }
            mRoot.appendChild(e);
        }
    }

    public boolean save(FileOutputStream file) {
        try {
            TransformerFactory f = TransformerFactory.newInstance();
            Transformer tf = f.newTransformer();
            tf.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            tf.setOutputProperty(OutputKeys.INDENT, "no");
            tf.setOutputProperty(OutputKeys.STANDALONE, "yes");
            DOMSource source = new DOMSource(mDoc);
            PrintWriter pw = new PrintWriter(file);
            StreamResult result = new StreamResult(pw);
            tf.transform(source, result);
            return true;
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean loadFile(InputStream file, String[] fields) {
        try {
            mList = new ArrayList<ContentValues>();
            mDoc = mBuilder.parse(file);
            Element root = mDoc.getDocumentElement();
            NodeList list = root.getElementsByTagName(RECORD);
            int length = list.getLength();
            for (int i = 0; i < length; i++) {
                ContentValues map = new ContentValues();
                Element e = (Element) list.item(i);
                if (e == null) {
                    continue;
                }
                for (int k = 0; k < fields.length; k++) {
                    String v = e.getAttribute(fields[k]);
                    if (TextUtils.isEmpty(v)) {
                        continue;
                    }
                    map.put(fields[k], v);
                }
                mList.add(map);
            }
            return true;
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public ArrayList<ContentValues> getContentValues() {
        return mList;
    }

}
