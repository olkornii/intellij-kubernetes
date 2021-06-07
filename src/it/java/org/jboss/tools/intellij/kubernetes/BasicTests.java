/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.intellij.kubernetes;

import com.intellij.remoterobot.RemoteRobot;

import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;
import org.assertj.swing.core.MouseButton;
import org.jboss.tools.intellij.kubernetes.fixtures.dialogs.NewProjectDialogFixture;
import org.jboss.tools.intellij.kubernetes.fixtures.dialogs.WelcomeFrameDialogFixture;
import org.jboss.tools.intellij.kubernetes.fixtures.mainIdeWindow.KubernetesToolsFixture;
import org.jboss.tools.intellij.kubernetes.fixtures.mainIdeWindow.ToolWindowsPaneFixture;
import org.jboss.tools.intellij.kubernetes.utils.GlobalUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.util.List;

/**
 * Basic JUnit UI tests for intellij-kubernetes
 *
 * @author olkornii@redhat.com
 */
public class BasicTests {

    private static RemoteRobot robot;

    @BeforeAll
    public static void connect() throws InterruptedException {
        GlobalUtils.waitUntilIntelliJStarts(8082);
        robot = GlobalUtils.getRemoteRobotConnection(8082);
        GlobalUtils.clearTheWorkspace(robot);
    }
    
    @Test
    public void checkClusterConnected() {
    	step("New Empty Project", () -> {
            createEpmtyProject();
            final ToolWindowsPaneFixture toolWindowsPaneFixture = robot.find(ToolWindowsPaneFixture.class);
            waitFor(Duration.ofSeconds(10), Duration.ofSeconds(1), "The 'Kubernetes' stripe button is not available.", () -> isStripeButtonAvailable(toolWindowsPaneFixture, "Kubernetes"));
            toolWindowsPaneFixture.stripeButton("Kubernetes").click();
            final KubernetesToolsFixture kubernetesToolsFixture = robot.find(KubernetesToolsFixture.class);
            waitFor(Duration.ofSeconds(15), Duration.ofSeconds(1), "Kubernetes Tree View is not available.", () -> isKubernetesViewTreeAvailable(kubernetesToolsFixture));
            String clusterText = kubernetesToolsFixture.kubernetesViewTree().findAllText().get(0).getText();
            assertTrue(clusterText.contains("minikube"));
            List<RemoteText> all_text_1 = kubernetesToolsFixture.kubernetesViewTree().findAllText();
            boolean needClickMinikube = true;
            System.out.println("1-st ====================================================================");
            for (RemoteText text_for_print : all_text_1){
                System.out.println(text_for_print.getText());
                if (text_for_print.getText().contains("Namespaces")){
                    needClickMinikube = false;
                }
            }
            if (needClickMinikube){
                kubernetesToolsFixture.kubernetesViewTree().findText(clusterText).doubleClick(MouseButton.LEFT_BUTTON);
            }
            waitFor(Duration.ofSeconds(15), Duration.ofSeconds(1), "Kubernetes Tree View is not available.", () -> isNamespaceOpened(kubernetesToolsFixture));
            List<RemoteText> all_text_2 = kubernetesToolsFixture.kubernetesViewTree().findAllText();
            System.out.println("2-nd ====================================================================");
            for (RemoteText text_for_print : all_text_2){
                System.out.println(text_for_print.getText());
            }
            kubernetesToolsFixture.kubernetesViewTree().findText("Nodes").doubleClick(MouseButton.LEFT_BUTTON);
            waitFor(Duration.ofSeconds(15), Duration.ofSeconds(1), "Namespace is not available.", () -> isNamespaceLoaded(kubernetesToolsFixture));
//            String namespaces = kubernetesToolsFixture.kubernetesViewTree().findAllText().get(1).getText();
            ComponentFixture try_to_find = robot.find(ComponentFixture.class, byXpath("//div[@class='Tree']"));
            List<RemoteText> all_text = try_to_find.findAllText();
            System.out.println("3-d ====================================================================");
//            System.out.println(all_text);
            for (RemoteText text_for_print : all_text){
                System.out.println(text_for_print.getText());
                assertFalse(text_for_print.getText().contains("Error: Could not get"));
            }
//            try {
//                Thread.sleep(15000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        });
 	}

 	private static void createEpmtyProject(){
        final WelcomeFrameDialogFixture welcomeFrameDialogFixture = robot.find(WelcomeFrameDialogFixture.class);
        welcomeFrameDialogFixture.createNewProjectLink().click();
        final NewProjectDialogFixture newProjectDialogFixture = welcomeFrameDialogFixture.find(NewProjectDialogFixture.class, Duration.ofSeconds(20));
        newProjectDialogFixture.projectTypeJBList().findText("Empty Project").click();
        newProjectDialogFixture.button("Next").click();
        newProjectDialogFixture.button("Finish").click();
        GlobalUtils.waitUntilTheProjectImportIsComplete(robot);
        GlobalUtils.cancelProjectStructureDialogIfItAppears(robot);
        GlobalUtils.closeTheTipOfTheDayDialogIfItAppears(robot);
        GlobalUtils.waitUntilAllTheBgTasksFinish(robot);
    }

    private static boolean isStripeButtonAvailable(ToolWindowsPaneFixture toolWindowsPaneFixture, String label) { // loading...
        try {
            toolWindowsPaneFixture.stripeButton(label);
        } catch (WaitForConditionTimeoutException e) {
            return false;
        }
        return true;
    }

    private static boolean isKubernetesViewTreeAvailable(KubernetesToolsFixture kubernetesToolsFixture){
        List<RemoteText> allText = kubernetesToolsFixture.kubernetesViewTree().findAllText();
        String firstText = allText.get(0).getText();
        if("Nothing to show".equals(firstText)){
            return false;
        }
        return true;
    }

    private static boolean isNamespaceOpened(KubernetesToolsFixture kubernetesToolsFixture){
        List<RemoteText> allTextFromTree = kubernetesToolsFixture.kubernetesViewTree().findAllText();
        for (RemoteText actualText : allTextFromTree){
            if (actualText.getText().contains("Namespaces")){
                return true;
            }
        }
        return false;
    }

    private static boolean isNamespaceLoaded(KubernetesToolsFixture kubernetesToolsFixture){
        List<RemoteText> allTextFromTree = kubernetesToolsFixture.kubernetesViewTree().findAllText();
        for (RemoteText actualText : allTextFromTree){
            if (actualText.getText().contains("loading...")){
                return false;
            }
        }
        return true;
    }
}
