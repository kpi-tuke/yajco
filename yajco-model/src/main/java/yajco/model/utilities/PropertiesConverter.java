package yajco.model.utilities;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.util.Map;
import java.util.Properties;

public class PropertiesConverter implements Converter {

    @Override
    public boolean canConvert(Class type) {
        return Properties.class == type;
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        Properties props = (Properties) source;
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            writer.startNode("property");
            writer.addAttribute("name", entry.getKey().toString());
            writer.addAttribute("value", entry.getValue().toString());
            writer.endNode();
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Properties props = new Properties();
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            String key = reader.getAttribute("name");
            String value = reader.getAttribute("value");
            props.setProperty(key, value);
            reader.moveUp();
        }
        return props;
    }
}
