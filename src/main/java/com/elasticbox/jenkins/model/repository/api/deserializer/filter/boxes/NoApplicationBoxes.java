/*
 *
 *  ElasticBox Confidential
 *  Copyright (c) 2016 All Right Reserved, ElasticBox Inc.
 *
 *  NOTICE:  All information contained herein is, and remains the property
 *  of ElasticBox. The intellectual and technical concepts contained herein are
 *  proprietary and may be covered by U.S. and Foreign Patents, patents in process,
 *  and are protected by trade secret or copyright law. Dissemination of this
 *  information or reproduction of this material is strictly forbidden unless prior
 *  written permission is obtained from ElasticBox.
 *
 */

package com.elasticbox.jenkins.model.repository.api.deserializer.filter.boxes;

import com.elasticbox.jenkins.model.box.BoxType;
import net.sf.json.JSONObject;

/**
 * Created by serna on 12/3/15.
 */
public class NoApplicationBoxes extends NoPolicyBoxesFilter {

    @Override
    public boolean apply(JSONObject jsonObject) {

        String schema = jsonObject.getString("schema");

        if(BoxType.APPLICATION.isType(schema)){
            return false;
        }

        return true;
    }

}