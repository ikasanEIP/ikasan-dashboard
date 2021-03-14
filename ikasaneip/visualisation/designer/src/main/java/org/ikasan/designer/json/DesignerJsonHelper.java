package org.ikasan.designer.json;

import com.helger.commons.state.EContinue;
import com.vaadin.flow.component.html.Image;
import org.ikasan.designer.pallet.DesignerItemIdentifier;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DesignerJsonHelper {

    /**
     * Get a JSONObject based on DesignerItemIdentifier.type
     *
     * @param type
     * @param json
     * @return
     */
    public static List<JSONObject> getIdentifierTypeItems(String type, String json) {
        ArrayList<JSONObject> results = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(json);

        jsonArray.forEach(item -> {
            if(item instanceof JSONObject) {
                String id = ((JSONObject)item).getString("id");

                try {
                    DesignerItemIdentifier designerItemIdentifier = DesignerItemIdentifier.getIdentifier(id);

                    if(designerItemIdentifier.getType().equals(type)) {
                        results.add((JSONObject)item);
                    }
                }
                catch (IllegalArgumentException e){
                    // ignore as we are not interested in this item.
                }
            }
        });

        return results;
    }
}
