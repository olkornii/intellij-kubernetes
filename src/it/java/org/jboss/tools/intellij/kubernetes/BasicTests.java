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
import com.intellij.remoterobot.utils.Keyboard;
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;
import org.assertj.swing.core.MouseButton;
import org.jboss.tools.intellij.kubernetes.fixtures.dialogs.IdeFatalErrorsDialogFixture;
import org.jboss.tools.intellij.kubernetes.fixtures.dialogs.NewProjectDialogFixture;
import org.jboss.tools.intellij.kubernetes.fixtures.dialogs.WelcomeFrameDialogFixture;
import org.jboss.tools.intellij.kubernetes.fixtures.mainIdeWindow.EditorsSplittersFixture;
import org.jboss.tools.intellij.kubernetes.fixtures.mainIdeWindow.IdeStatusBarFixture;
import org.jboss.tools.intellij.kubernetes.fixtures.mainIdeWindow.KubernetesToolsFixture;
import org.jboss.tools.intellij.kubernetes.fixtures.mainIdeWindow.ToolWindowsPaneFixture;
import org.jboss.tools.intellij.kubernetes.fixtures.menus.ActionToolbarMenu;
import org.jboss.tools.intellij.kubernetes.fixtures.menus.RightClickMenu;
import org.jboss.tools.intellij.kubernetes.tests.*;
import org.jboss.tools.intellij.kubernetes.utils.GlobalUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.time.Duration;
import java.util.List;

/**
 * JUnit UI tests for intellij-kubernetes
 *
 * @author olkornii@redhat.com
 */
public class BasicTests extends AbstractKubernetesTest{

    private static RemoteRobot robot;
    private static ComponentFixture kubernetesViewTree;

    @BeforeAll
    public static void connect() throws InterruptedException {
        GlobalUtils.waitUntilIntelliJStarts(8082);
        robot = GlobalUtils.getRemoteRobotConnection(8082);
        GlobalUtils.clearTheWorkspace(robot);
        createEmptyProject();
        openKubernetesTab();
        KubernetesToolsFixture kubernetesToolsFixture = robot.find(KubernetesToolsFixture.class);
        kubernetesViewTree = kubernetesToolsFixture.getKubernetesViewTree();
        waitFor(Duration.ofSeconds(15), Duration.ofSeconds(1), "Kubernetes Tree View is not available.", BasicTests::isKubernetesViewTreeAvailable);
    }

    @Test
    public void checkClusterConnected() {
        step("New Empty Project", () -> ClusterConnectedTest.checkClusterConnected(robot, kubernetesViewTree));
    }

    @Test
    public void openResourceEditor() {
        step("open Resource Editor", () -> OpenResourceEditorTest.checkResourceEditor(robot, kubernetesViewTree));
    }

    @Test
    public void editResource() {
//        step("edit Resource", () -> EditResourceTest.editResource(robot, kubernetesViewTree));
//        public static void editResource(RemoteRobot robot, ComponentFixture kubernetesViewTree){
            openResourceContentList(new String[]{"Nodes"}, kubernetesViewTree);
            RemoteText selectedResource = getResourceByIdInParent("Nodes", 0, kubernetesViewTree);
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
            RemoteText placeForNewLabel = remote_text.get(labelsId+2); // +1 because we need the next one, +1 because between every 2 real elements is space
            placeForNewLabel.click(); // set the cursor
            Keyboard my_keyboard = new Keyboard(robot);
            my_keyboard.enterText("    some_label: \"some_label\"");
            my_keyboard.enter();
            my_keyboard.backspace();

            ActionToolbarMenu toolbarMenu = robot.find(ActionToolbarMenu.class);
            toolbarMenu.PushToCluster();

            editorSplitter.closeEditor(editorTitle);
            hideClusterContent(kubernetesViewTree);

            openResourceContentList(new String[]{"Nodes"}, kubernetesViewTree);

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

            editorSplitter.closeEditor(editorTitle); // close editor
            hideClusterContent(kubernetesViewTree);

            assertTrue(labelExist);
//        }
    }

    @Test
    public void createResourceByEdit() {
        step("create Resource", () -> CreateResourceByEditTest.createResourceByEdit(robot, kubernetesViewTree));

        step("delete Resource", () -> CreateResourceByEditTest.deleteResource(robot, kubernetesViewTree));
    }

    private static final String newResourceName = "newresourcename2";

    @Test
    public void createAnotherResourceTypeByEdit() {
//        step("create another type of Resource", () -> CreateAnotherTypeResourceByEditTest.createAnotherTypeResourceByEdit(robot, kubernetesViewTree));
//        private static final String newResourceName = "newresourcename2";

//        public static void createAnotherTypeResourceByEdit(RemoteRobot robot, ComponentFixture kubernetesViewTree){
            clearErrors(robot);

            openResourceContentList(new String[]{"Nodes"}, kubernetesViewTree);
            RemoteText selectedResource = getResourceByIdInParent("Nodes", 0, kubernetesViewTree);
            selectedResource.doubleClick();

            EditorsSplittersFixture editorSplitter = robot.find(EditorsSplittersFixture.class);
            Keyboard myKeyboard = new Keyboard(robot);

            setupNewPod(robot, myKeyboard, selectedResource.getText() + ".yml");

            clearErrors(robot);

            ActionToolbarMenu toolbarMenu = robot.find(ActionToolbarMenu.class);
            toolbarMenu.PushToCluster();

            editorSplitter.closeEditor(newResourceName + ".yml"); // close editor
            hideClusterContent(kubernetesViewTree);
            openResourceContentList(new String[] {"Workloads", "Pods"}, kubernetesViewTree);
            waitFor(Duration.ofSeconds(15), Duration.ofSeconds(1), "New resource was not been created.", () -> isResourceCreated(kubernetesViewTree, newResourceName, false));
            hideClusterContent(kubernetesViewTree);


            String errorMessage = "";
            if (isError(robot)){
                robot.find(IdeStatusBarFixture.class).ideErrorsIcon().click();
                IdeFatalErrorsDialogFixture ideErrorsDialog = robot.find(IdeFatalErrorsDialogFixture.class);
                for (RemoteText remoteText: ideErrorsDialog.exceptionDescriptionJTextArea().findAllText()){
                    errorMessage = errorMessage + remoteText.getText();
                }
            }

            assertFalse(isError(robot), errorMessage);
//        }
    }

    private static void setupNewPod(RemoteRobot robot, Keyboard myKeyboard, String editorTitle){
        Clipboard clipboard = getSystemClipboard();

        String text = "apiVersion: apps/v1\n" +
                "kind: Deployment\n" +
                "metadata:\n" +
                "  name: " + newResourceName + "\n" +
                "spec:\n" +
                "  replicas: 2\n" +
                "  selector:\n" +
                "    matchLabels:\n" +
                "      app: sise\n" +
                "  template:\n" +
                "    metadata:\n" +
                "      labels:\n" +
                "        app: sise\n" +
                "    spec:\n" +
                "      containers:\n" +
                "      - name: sise\n" +
                "        image: quay.io/openshiftlabs/simpleservice:0.5.0\n" +
                "        ports:\n" +
                "        - containerPort: 9876\n" +
                "        env:\n" +
                "        - name: SIMPLE_SERVICE_VERSION\n" +
                "          value: \"0.9\"";

        clipboard.setContents(new StringSelection(text), null);

        EditorsSplittersFixture editorSplitter = robot.find(EditorsSplittersFixture.class);
        ComponentFixture textFixture = editorSplitter.getEditorTextFixture(editorTitle);
        RemoteText remoteText = textFixture.findAllText().get(0);

        myKeyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A);
        remoteText.click(MouseButton.RIGHT_BUTTON);

        RightClickMenu rightClickMenu = robot.find(RightClickMenu.class);
        rightClickMenu.select("Paste");
    }

    private static Clipboard getSystemClipboard()
    {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        Clipboard systemClipboard = defaultToolkit.getSystemClipboard();

        return systemClipboard;
    }

    private static void createEmptyProject(){
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

    private static void openKubernetesTab(){
        final ToolWindowsPaneFixture toolWindowsPaneFixture = robot.find(ToolWindowsPaneFixture.class);
        waitFor(Duration.ofSeconds(10), Duration.ofSeconds(1), "The 'Kubernetes' stripe button is not available.", () -> isStripeButtonAvailable(toolWindowsPaneFixture, "Kubernetes"));
        toolWindowsPaneFixture.stripeButton("Kubernetes").click();
    }

    private static boolean isStripeButtonAvailable(ToolWindowsPaneFixture toolWindowsPaneFixture, String label) { // loading...
        try {
            toolWindowsPaneFixture.stripeButton(label);
        } catch (WaitForConditionTimeoutException e) {
            return false;
        }
        return true;
    }

    private static boolean isKubernetesViewTreeAvailable(){
        List<RemoteText> allText = kubernetesViewTree.findAllText();
        String firstText = allText.get(0).getText();
        return !"Nothing to show".equals(firstText);
    }

}