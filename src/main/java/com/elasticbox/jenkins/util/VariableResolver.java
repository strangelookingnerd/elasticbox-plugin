/*
 * ElasticBox Confidential
 * Copyright (c) 2014 All Right Reserved, ElasticBox Inc.
 *
 * NOTICE:  All information contained herein is, and remains the property
 * of ElasticBox. The intellectual and technical concepts contained herein are
 * proprietary and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law. Dissemination of this
 * information or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from ElasticBox.
 */

package com.elasticbox.jenkins.util;

import com.elasticbox.jenkins.builders.IInstanceProvider;
import hudson.model.AbstractBuild;
import hudson.model.Project;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONObject;

/**
 *
 * @author Phong Nguyen Le
 */
public final class VariableResolver {
    private final List<IInstanceProvider> instanceProviders;
    private final hudson.util.VariableResolver<String> resolver;

    public VariableResolver(AbstractBuild build) {
        instanceProviders = new ArrayList<IInstanceProvider>();
        for (Object builder : ((Project) build.getProject()).getBuilders()) {
            if (builder instanceof IInstanceProvider) {
                instanceProviders.add((IInstanceProvider) builder);
            }
        }
        resolver = build.getBuildVariableResolver();
    }
    
    public JSONObject resolve(JSONObject variable) {
        String value = variable.getString("value");
        if (value.startsWith("${") && value.endsWith("}")) {
            String resolvedValue = resolver.resolve(value.substring(2, value.length() - 1));
            if (resolvedValue != null) {
                variable.put("value", resolvedValue);
            }
        }

        if ("Binding".equals(variable.getString("type")) && value.startsWith("com.elasticbox.jenkins.builders.")) {
            for (IInstanceProvider instanceProvider : instanceProviders) {
                if (value.equals(instanceProvider.getId())) {
                    variable.put("value", instanceProvider.getInstanceId());
                }
            }                
        }

        if (variable.getString("scope").isEmpty()) {
            variable.remove("scope");
        }
            
        return variable;
    }
    
}