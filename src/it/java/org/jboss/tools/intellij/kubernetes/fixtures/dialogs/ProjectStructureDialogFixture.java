/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.intellij.kubernetes.fixtures.dialogs;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.CommonContainerFixture;
import com.intellij.remoterobot.fixtures.DefaultXpath;
import com.intellij.remoterobot.fixtures.FixtureName;
import org.jetbrains.annotations.NotNull;

/**
 * Project Structure dialog fixture
 *
 * @author olkornii@redhat.com
 */
@DefaultXpath(by = "MyDialog type", xpath = "//div[@accessiblename='Project Structure' and @class='MyDialog']")
@FixtureName(name = "Tip Of The Day Dialog")
public class ProjectStructureDialogFixture extends CommonContainerFixture {
    public ProjectStructureDialogFixture(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }
}
