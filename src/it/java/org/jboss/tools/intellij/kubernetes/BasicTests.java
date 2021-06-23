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
import com.intellij.remoterobot.utils.Keyboard;

import com.intellij.remoterobot.data.TextData;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;
import org.assertj.swing.core.MouseButton;
import org.jboss.tools.intellij.kubernetes.fixtures.dialogs.NewProjectDialogFixture;
import org.jboss.tools.intellij.kubernetes.fixtures.dialogs.WelcomeFrameDialogFixture;
import org.jboss.tools.intellij.kubernetes.fixtures.mainIdeWindow.EditorsSplittersFixture;
import org.jboss.tools.intellij.kubernetes.fixtures.mainIdeWindow.IdeStatusBarFixture;
import org.jboss.tools.intellij.kubernetes.fixtures.mainIdeWindow.KubernetesToolsFixture;
import org.jboss.tools.intellij.kubernetes.fixtures.mainIdeWindow.ToolWindowsPaneFixture;
import org.jboss.tools.intellij.kubernetes.fixtures.menus.RightClickMenu;
import org.jboss.tools.intellij.kubernetes.fixtures.popups.EditorNotificationPanelFixture;
import org.jboss.tools.intellij.kubernetes.utils.GlobalUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        createEpmtyProject();
    }

    @Test
    public void checkClusterConnected() {
        step("New Empty Project", () -> {
            final ToolWindowsPaneFixture toolWindowsPaneFixture = robot.find(ToolWindowsPaneFixture.class);
            waitFor(Duration.ofSeconds(10), Duration.ofSeconds(1), "The 'Kubernetes' stripe button is not available.", () -> isStripeButtonAvailable(toolWindowsPaneFixture, "Kubernetes"));
            toolWindowsPaneFixture.stripeButton("Kubernetes").click();
            final KubernetesToolsFixture kubernetesToolsFixture = robot.find(KubernetesToolsFixture.class);
            final ComponentFixture kubernetesViewTree = kubernetesToolsFixture.getKubernetesViewTree();
            waitFor(Duration.ofSeconds(15), Duration.ofSeconds(1), "Kubernetes Tree View is not available.", () -> isKubernetesViewTreeAvailable(kubernetesViewTree));
            String clusterText = kubernetesViewTree.findAllText().get(0).getText();
            assertTrue(clusterText.contains("default")); // change to "minikube" after testing

//            List<RemoteText> all_text_2 = kubernetesToolsFixture.getKubernetesViewTree().findAllText();
//            System.out.println("2-nd ====================================================================");
//            for (RemoteText text_for_print : all_text_2){
//                System.out.println(text_for_print.getText());
//            }
//            kubernetesToolsFixture.getKubernetesViewTree().findText("Nodes").doubleClick(MouseButton.LEFT_BUTTON);
//            waitFor(Duration.ofSeconds(15), Duration.ofSeconds(1), "Namespace is not available.", () -> isNamespaceLoaded(kubernetesToolsFixture));
////            String namespaces = kubernetesToolsFixture.getKubernetesViewTree().findAllText().get(1).getText();
//            ComponentFixture try_to_find = robot.find(ComponentFixture.class, byXpath("//div[@class='Tree']"));
//            List<RemoteText> all_text = try_to_find.findAllText();
//            System.out.println("3-d ====================================================================");
////            System.out.println(all_text);
//            for (RemoteText text_for_print : all_text){
//                System.out.println(text_for_print.getText());
//                assertFalse(text_for_print.getText().contains("Error: Could not get"));
//            }
//            try {
//                Thread.sleep(15000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        });
    }

    @Test
    public void openResourceEditor() {
        step("open Resource Editor", () -> {
            final KubernetesToolsFixture kubernetesToolsFixture = robot.find(KubernetesToolsFixture.class);
            final ComponentFixture kubernetesViewTree = kubernetesToolsFixture.getKubernetesViewTree();
            openClusterContent(kubernetesViewTree);
            kubernetesViewTree.findText("Nodes").doubleClick(MouseButton.LEFT_BUTTON);
            waitFor(Duration.ofSeconds(15), Duration.ofSeconds(1), "Nodes is not available.", () -> isNodesLoaded(kubernetesViewTree));

            int resourceId = getResourceByIdInParent("Nodes", 0, kubernetesViewTree);
            RemoteText selectedResource = kubernetesViewTree.findAllText().get(resourceId);
            selectedResource.click(MouseButton.RIGHT_BUTTON);
            RightClickMenu rightClickMenu = robot.find(RightClickMenu.class);
            rightClickMenu.select("Edit...");

            EditorsSplittersFixture editorSplitter = robot.find(EditorsSplittersFixture.class);
            String editorTitle = selectedResource.getText() + ".yml";
            waitFor(Duration.ofSeconds(15), Duration.ofSeconds(1), "Editor is not available.", () -> isEditorOpened(editorTitle));
            waitFor(Duration.ofSeconds(15), Duration.ofSeconds(1), "Resource schema is wrong.", () -> isSchemaSet("v1#Node"));

            editorSplitter.closeEditor(editorTitle);
            String clusterText = kubernetesViewTree.findAllText().get(0).getText();
            kubernetesViewTree.findText(clusterText).doubleClick();
        });
    }

    @Test
    public void editResource() {
        step("edit Resource", () -> {
            final KubernetesToolsFixture kubernetesToolsFixture = robot.find(KubernetesToolsFixture.class);
            final ComponentFixture kubernetesViewTree = kubernetesToolsFixture.getKubernetesViewTree();

            openClusterContent(kubernetesViewTree);
            kubernetesViewTree.findText("Nodes").doubleClick(MouseButton.LEFT_BUTTON);
            waitFor(Duration.ofSeconds(15), Duration.ofSeconds(1), "Nodes is not available.", () -> isNodesLoaded(kubernetesViewTree));

            int resourceId = getResourceByIdInParent("Nodes", 0, kubernetesViewTree);
            RemoteText selectedResource = kubernetesViewTree.findAllText().get(resourceId);
            selectedResource.doubleClick();

            EditorsSplittersFixture editorSplitter = robot.find(EditorsSplittersFixture.class);
            String editorTitle = selectedResource.getText() + ".yml";

            ComponentFixture textFixture = editorSplitter.getEditorTextFixture(editorTitle);
            List<RemoteText> remote_text = textFixture.findAllText();
            int labelsId = 0;
            for (RemoteText actual_remote_text : remote_text){
                if ("labels".equals(actual_remote_text.getText())){
                    break;
                }
                labelsId++;
            }
            RemoteText place_for_print = remote_text.get(labelsId+2);
            place_for_print.click();
            Keyboard my_keyboard = new Keyboard(robot);
            my_keyboard.enterText("    some_label: \"some_label\"");
            my_keyboard.enter();
            my_keyboard.backspace();

            EditorNotificationPanelFixture notificationPanel = robot.find(EditorNotificationPanelFixture.class);
            notificationPanel.PushToCluster();

            editorSplitter.closeEditor(editorTitle);
            String clusterText = kubernetesViewTree.findAllText().get(0).getText();
            kubernetesViewTree.findText(clusterText).doubleClick();

            openClusterContent(kubernetesViewTree);
            kubernetesViewTree.findText("Nodes").doubleClick(MouseButton.LEFT_BUTTON);
            waitFor(Duration.ofSeconds(15), Duration.ofSeconds(1), "Nodes is not available.", () -> isNodesLoaded(kubernetesViewTree));
            selectedResource.doubleClick();
            ComponentFixture textFixtureNew = editorSplitter.getEditorTextFixture(editorTitle);
            List<RemoteText> remoteTextNew = textFixtureNew.findAllText();
            boolean labelExist = false;
            for (RemoteText actual_remote_text : remoteTextNew){
                if (actual_remote_text.getText().contains("some_label")){
                    labelExist = true;
                    break;
                }
            }

            editorSplitter.closeEditor(editorTitle);
            kubernetesViewTree.findText(clusterText).doubleClick();

            assertTrue(labelExist);
        });
    }

//    @Test
    public void createNewResourceFromAnother() {
        step("create new resource from another", () -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            final KubernetesToolsFixture kubernetesToolsFixture = robot.find(KubernetesToolsFixture.class);
            final ComponentFixture kubernetesViewTree = kubernetesToolsFixture.getKubernetesViewTree();

            openClusterContent(kubernetesViewTree);
            kubernetesViewTree.findText("Nodes").doubleClick(MouseButton.LEFT_BUTTON);
            waitFor(Duration.ofSeconds(15), Duration.ofSeconds(1), "Nodes is not available.", () -> isNodesLoaded(kubernetesViewTree));

            int resourceId = getResourceByIdInParent("Nodes", 0, kubernetesViewTree);
            RemoteText selectedResource = kubernetesViewTree.findAllText().get(resourceId);
            selectedResource.doubleClick();

            EditorsSplittersFixture editorSplitter = robot.find(EditorsSplittersFixture.class);
            String editorTitle = selectedResource.getText() + ".yml";

            ComponentFixture textFixture = editorSplitter.getEditorTextFixture(editorTitle);
            List<TextData> data = textFixture.extractData();
            System.out.println(data);
//            List<RemoteText> remote_text = textFixture.findAllText();
//            String text = "";
//            for (RemoteText actual_remote_text : remote_text){
//                text = actual_remote_text.getText();
//                System.out.println(text);
//            }

        });
    }

    private static boolean isSchemaSet(String schemaName){
        try {
            IdeStatusBarFixture statusBarFixture = robot.find(IdeStatusBarFixture.class);
            statusBarFixture.withIconAndArrows("Schema: " + schemaName);
        } catch (WaitForConditionTimeoutException e) {
            return false;
        }
        return true;
    }

    private static int getResourceByIdInParent(String parentName, int id, ComponentFixture kubernetesViewTree){
        List<RemoteText> kubernetesToolsText = kubernetesViewTree.findAllText();
        int parentId = 0;
        for (RemoteText findParent : kubernetesToolsText){
            if (findParent.getText().contains(parentName)){
                break;
            }
            parentId++;
        }
        return (parentId + id + 1);
    }

    private static void openClusterContent(ComponentFixture kubernetesViewTree){
        List<RemoteText> kubernetesToolsText = kubernetesViewTree.findAllText();
        boolean needClickOnMinikube = true;
        for (RemoteText findNodes : kubernetesToolsText){
            if (findNodes.getText().contains("Nodes")){
                needClickOnMinikube = false;
                break;
            }
        }
        if (needClickOnMinikube){
            String clusterText = kubernetesViewTree.findAllText().get(0).getText();
            kubernetesViewTree.findText(clusterText).doubleClick(MouseButton.LEFT_BUTTON);
        }
        waitFor(Duration.ofSeconds(15), Duration.ofSeconds(1), "Kubernetes Tree View is not available.", () -> isNodesOpened(kubernetesViewTree));
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

    private static boolean isEditorOpened(String editorTitle){
        try {
            robot.find(EditorsSplittersFixture.class, byXpath("//div[@accessiblename='Editor for " + editorTitle + "' and @class='EditorComponentImpl']"));
        } catch (WaitForConditionTimeoutException e) {
            return false;
        }
        return true;
    }

    private static boolean isStripeButtonAvailable(ToolWindowsPaneFixture toolWindowsPaneFixture, String label) { // loading...
        try {
            toolWindowsPaneFixture.stripeButton(label);
        } catch (WaitForConditionTimeoutException e) {
            return false;
        }
        return true;
    }

    private static boolean isKubernetesViewTreeAvailable(ComponentFixture kubernetesViewTree){
        List<RemoteText> allText = kubernetesViewTree.findAllText();
        String firstText = allText.get(0).getText();
        if("Nothing to show".equals(firstText)){
            return false;
        }
        return true;
    }

    private static boolean isNodesOpened(ComponentFixture kubernetesViewTree){
        List<RemoteText> allTextFromTree = kubernetesViewTree.findAllText();
        for (RemoteText actualText : allTextFromTree){
            if (actualText.getText().contains("Nodes")){
                return true;
            }
        }
        return false;
    }

    private static boolean isNodesLoaded(ComponentFixture kubernetesViewTree){
        List<RemoteText> allTextFromTree = kubernetesViewTree.findAllText();
        for (RemoteText actualText : allTextFromTree){
            if (actualText.getText().contains("loading...")){
                return false;
            }
        }
        return true;
    }
}
