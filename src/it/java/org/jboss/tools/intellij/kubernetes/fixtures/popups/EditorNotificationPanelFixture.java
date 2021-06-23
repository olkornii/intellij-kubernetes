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
package org.jboss.tools.intellij.kubernetes.fixtures.popups;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.CommonContainerFixture;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.DefaultXpath;
import com.intellij.remoterobot.fixtures.FixtureName;
import org.jetbrains.annotations.NotNull;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

/**
 * @author olkornii@redhat.com
 */
@DefaultXpath(by = "EditorNotificationPanel type", xpath = "//div[@class='EditorNotificationPanel']")
@FixtureName(name = "Editor Notification Panel")
public class EditorNotificationPanelFixture extends CommonContainerFixture {
    public EditorNotificationPanelFixture(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    public void PushToCluster(){
        find(ComponentFixture.class, byXpath("//div[@accessiblename='Push to Cluster' and @class='ActionHyperlinkLabel']")).click();
    }

    public void Ignore(){
        find(ComponentFixture.class, byXpath("//div[@accessiblename='Ignore' and @class='ActionHyperlinkLabel']")).click();
    }
}
