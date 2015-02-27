/*
 * Copyright (C) 2013 Pavel Stastny
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.incad.kramerius.rest.api.k5.client.item.decorators.details;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.inject.Inject;

import cz.incad.kramerius.SolrAccess;
import cz.incad.kramerius.rest.api.k5.client.SolrMemoization;
import cz.incad.kramerius.rest.api.k5.client.AbstractDecorator.TokenizedPath;
import cz.incad.kramerius.rest.api.k5.client.item.decorators.AbstractItemDecorator;
import cz.incad.kramerius.rest.api.k5.client.item.decorators.ItemSolrRootPidDecorate;
import cz.incad.kramerius.rest.api.k5.client.utils.SOLRDecoratorUtils;
import cz.incad.kramerius.rest.api.k5.client.utils.SOLRUtils;
import cz.incad.kramerius.utils.XMLUtils;

public class MonographUnitDecorate extends AbstractDetailDecorator {

    public static final Logger LOGGER = Logger
            .getLogger(ItemSolrRootPidDecorate.class.getName());

    public static final String MONOGRAPH_UNIT = AbstractItemDecorator
            .key("DETAILS.MONOGRAPHUNIT");

    @Inject
    SolrAccess solrAccess;

    @Inject
    SolrMemoization memo;

    @Override
    public String getKey() {
        return MONOGRAPH_UNIT;
    }

    @Override
    public void decorate(JSONObject jsonObject,
            Map<String, Object> runtimeContext) {
        // 1929##1
        if (jsonObject.containsKey("pid")) {
            String pid = jsonObject.getString("pid");
            try {
                Element doc = this.memo.getRememberedIndexedDoc(pid);
                if (doc == null)
                    doc = this.memo.askForIndexDocument(pid);

                if (doc != null) {
                    List<String> array = SOLRUtils.array(doc, "details",
                            String.class);
                    if (!array.isEmpty()) {
                        String[] details = super.details(array.get(0));
                        JSONObject detailsJSONObject = new JSONObject();
                        if (details.length > 0) {
                            detailsJSONObject.put("partNumber", details[0]);
                        }
                        if (details.length > 1) {
                            detailsJSONObject.put("title", details[1]);
                        }
                        if (details.length > 2) {
                            detailsJSONObject.put("number", details[2]);
                        }
                        if (detailsJSONObject.keySet().size() > 0) {
                            jsonObject.put("details", detailsJSONObject);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }

    }

    @Override
    public boolean apply(JSONObject jsonObject, String context) {
        String m = super.getModel(jsonObject);
        TokenizedPath tpath = super.itemContext(tokenize(context));
        return tpath.isParsed() && m != null && m.equals("monographunit");
    }

}
