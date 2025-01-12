package org.jenkinsci.plugins.codesonar.conditions;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.codesonar.CodeSonarLogger;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.analysis.Alert;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.util.FormValidation;

/**
 *
 * @author Andrius
 */
public class YellowAlertLimitCondition extends Condition {
    private static final Logger LOGGER = Logger.getLogger(YellowAlertLimitCondition.class.getName());
    
    private static final String NAME = "Yellow alerts";
    private static final String RESULT_DESCRIPTION_MESSAGE_FORMAT = "threshold={0,number,0}, count: {1,number,0}";
   
    private int alertLimit = 1;
    private String warrantedResult = Result.UNSTABLE.toString();

    @DataBoundConstructor
    public YellowAlertLimitCondition(int alertLimit) {
        this.alertLimit = alertLimit;
    }

    public int getAlertLimit() {
        return alertLimit;
    }

    @DataBoundSetter
    public void setAlertLimit(int alertLimit) {
        this.alertLimit = alertLimit;
    }
   
    public String getWarrantedResult() {
        return warrantedResult;
    }

    @DataBoundSetter
    public void setWarrantedResult(String warrantedResult) {
        this.warrantedResult = warrantedResult;
    }

    @Override
    public Result validate(CodeSonarBuildActionDTO current, CodeSonarBuildActionDTO previous, Launcher launcher, TaskListener listener, CodeSonarLogger csLogger) {
        if (current == null) {
            registerResult(csLogger, CURRENT_BUILD_DATA_NOT_AVAILABLE);
            return Result.SUCCESS;
        }
        
        Analysis analysisActiveWarnings = current.getAnalysisActiveWarnings();
        
        // Going to produce build failure in the case of missing necessary information
        if(analysisActiveWarnings == null) {
            LOGGER.log(Level.SEVERE, "\"analysisActiveWarnings\" data not found in persisted build.");
            registerResult(csLogger, CURRENT_BUILD_DATA_NOT_AVAILABLE);
            return Result.FAILURE;
        }
        
        List<Alert> yellowAlerts = analysisActiveWarnings.getYellowAlerts();
        if (yellowAlerts.size() > alertLimit) {
            registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, alertLimit, yellowAlerts.size());
            return Result.fromString(warrantedResult);
        }

        registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, alertLimit, yellowAlerts.size());
        return Result.SUCCESS;
    }

    @Symbol("yellowAlerts")
    @Extension
    public static final class DescriptorImpl extends ConditionDescriptor<YellowAlertLimitCondition> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public @Nonnull String getDisplayName() {
            return NAME;
        }

        public FormValidation doCheckAlertLimit(@QueryParameter("alertLimit") int alertLimit) {
            if (alertLimit < 0) {
                return FormValidation.error("Cannot be a negative number");
            }

            return FormValidation.ok();
        }
    }
}
