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

package com.elasticbox.jenkins;

import com.elasticbox.jenkins.migration.RetentionTimeConverter;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.slaves.Cloud;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import java.io.IOException;
import java.util.UUID;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author Phong Nguyen Le
 */
public class InstanceCreator extends BuildWrapper {
    @Deprecated
    private String cloud;
    @Deprecated
    private String workspace;
    @Deprecated
    private String box;
    @Deprecated
    private String profile;
    @Deprecated
    private String variables;
    @Deprecated
    private String boxVersion;

    private ProjectSlaveConfiguration slaveConfiguration;

    private transient ElasticBoxSlave ebSlave;

    @DataBoundConstructor
    public InstanceCreator(ProjectSlaveConfiguration slaveConfiguration) {
        super();
        this.slaveConfiguration = slaveConfiguration;
    }

    public ProjectSlaveConfiguration getSlaveConfiguration() {
        return slaveConfiguration;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        for (Node node : build.getProject().getAssignedLabel().getNodes()) {
            if (node instanceof ElasticBoxSlave) {
                ElasticBoxSlave slave = (ElasticBoxSlave) node;
                if (slave.getComputer().getBuilds().contains(build)) {
                    ebSlave = slave;
                    break;
                }
            }
        }

        return new Environment() {
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                if (ebSlave.isSingleUse()) {
                    ebSlave.getComputer().setAcceptingTasks(false);
                }
                build.getProject().setAssignedLabel(null);
                return true;
            }
        };
    }

    protected Object readResolve() {
        if (slaveConfiguration == null) {
            ElasticBoxCloud ebCloud = null;
            if (cloud == null) {
                ebCloud = ElasticBoxCloud.getInstance();
                if (ebCloud != null) {
                    cloud = ebCloud.name;
                }
            } else {
                Cloud c = Jenkins.getInstance().getCloud(cloud);
                if (c instanceof ElasticBoxCloud) {
                    ebCloud = (ElasticBoxCloud) c;
                }
            }
            slaveConfiguration = new ProjectSlaveConfiguration(UUID.randomUUID().toString(), cloud, workspace, box,
                    boxVersion, profile, null, null, null, ebCloud != null ? ebCloud.getMaxInstances() : 1, null, variables,
                    StringUtils.EMPTY, 30, null, 1,
                    ElasticBoxSlaveHandler.TIMEOUT_MINUTES, null);
        }

        return this;
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Launch dedicated slave via ElasticBox";
        }

        @Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) throws FormException {


            JSONObject slaveConfigJson = formData.getJSONObject(ProjectSlaveConfiguration.SLAVE_CONFIGURATION);
            if(DescriptorHelper.anyOfThemIsBlank(slaveConfigJson.getString("cloud"), slaveConfigJson.getString("workspace"), slaveConfigJson.getString("box"))){
                throw new FormException("Required fields should be provided", ProjectSlaveConfiguration.SLAVE_CONFIGURATION);
            }

            DescriptorHelper.fixDeploymentPolicyFormData(slaveConfigJson);

            InstanceCreator instanceCreator = (InstanceCreator) super.newInstance(req, formData);

            ProjectSlaveConfiguration.DescriptorImpl descriptor = (ProjectSlaveConfiguration.DescriptorImpl) instanceCreator.getSlaveConfiguration().getDescriptor();
            descriptor.validateSlaveConfiguration(instanceCreator.getSlaveConfiguration());

            return instanceCreator;
        }

    }

    public static class ConverterImpl extends RetentionTimeConverter<InstanceCreator> {

        @Override
        protected void fixZeroRetentionTime(InstanceCreator obj) {
            ProjectSlaveConfiguration slaveConfig = obj.getSlaveConfiguration();
            if (slaveConfig != null && slaveConfig.getRetentionTime() == 0) {
                slaveConfig.retentionTime = Integer.MAX_VALUE;
            }
        }

    }
}
